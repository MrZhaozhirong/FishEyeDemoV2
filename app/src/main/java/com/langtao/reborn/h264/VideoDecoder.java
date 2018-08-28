package com.langtao.reborn.h264;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.langtao.codec.videoFrame;

import java.nio.ByteBuffer;

import glnk.client.GlnkStreamFormat;

/**
 * Created by zzr on 2017/12/22.
 */

public class VideoDecoder implements Runnable {
    private static final String TAG = DecodeH264Activity.TAG;
    private static final boolean DEBUG = true;

    private static final String VIDEO_AVC_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private LangTaoDecoder mRenderer;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private MediaCodec videoDecoder;

    public VideoDecoder(LangTaoDecoder renderer) {
        mRenderer = renderer;
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        mIsStopRequested = false;
    }

    /**
     * Note: call *after* set GlnkStreamFormat
     */
    public void prepareDecoder(Surface outputSurface) {
        GlnkStreamFormat liveStreamFmt = mRenderer.getLiveStreamFmt();
        if(liveStreamFmt == null){
            throw new IllegalStateException("Error: GlnkStreamFormat Is NULL !!!");
        }
        int mVideoWidth = liveStreamFmt.getVideoWidth();
        int mVideoHeight = liveStreamFmt.getVideoHeight();

        byte[] video_sps = {0, 0, 0, 1, 103, 100, 64, 41, -84, 44, -88, 10, 2, -1, -107};

        byte[] sps0 = {0, 0, 0, 1, 103, 100, 64, 41, -84, 44, -88, 5, 0, 91, -112};
        byte[] pps0 = {0, 0, 0, 1, 104, -18, 56, -128};

        byte[] sps1 = { 0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31, 0, 0, 3, 3, -23, 0, 0, -22, 96, -108 };
        byte[] pps1 = { 0, 0, 0, 1, 104, -18, 60, -128 };

        byte[] sps2 = { 0, 0, 0, 1, 39, 66, -32, 30, -115, 104, 10, 3, -38, 108, -128, 0, 0, 3, 0, -128, 0, 0, 12, -121, -118, 17, 80 };
        byte[] pps2 = { 0, 0, 0, 1, 40, -50, 4, 73, 32 };

        byte[] csd0 = { 0, 0, 0, 1, 103, 66, -128, 30, -38, 5, -63, 71, -105, -128, 109, 10, 19, 80 };
        byte[] csd1 = { 0, 0, 0, 1, 104, -50, 6, -30 };

        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_AVC_MIME_TYPE, mVideoWidth, mVideoHeight);
        //videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
        videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));

        Log.w(TAG, videoFormat.toString());
        try {
            videoDecoder = MediaCodec.createDecoderByType(VIDEO_AVC_MIME_TYPE);
            videoDecoder.configure(videoFormat, outputSurface, null, 0);
            videoDecoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (mLock){
            mReady = true;
            mLock.notify();
        }
    }


    private final Object mLock = new Object();
    private boolean mReady;
    private boolean mRunning;
    private volatile boolean mIsStopRequested;
    private Thread decodeThread;

    void execute() {
        if(decodeThread != null){
            try {
                mIsStopRequested = true;
                decodeThread.join();
                decodeThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        synchronized (mLock){
            while(!mReady) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mIsStopRequested = false;
        if(DEBUG) Log.d(TAG, "VideoDecoder: start Decode Thread ...");
        decodeThread = new Thread(this, "doExtractRawH264");
        decodeThread.start();
        synchronized (mLock){
            mRunning = true;
        }
    }

    void requestStop() {
        if(decodeThread != null){
            try {
                mIsStopRequested = true;
                decodeThread.join();
                decodeThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void release() {
        if(videoDecoder!=null){
            videoDecoder.flush();
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
    }

    @Override
    public void run() {
        final int TIMEOUT_USEC = 10000;
        boolean outputDone = false;
        boolean doRender = true;
        ByteBuffer[] decoderInputBuffers = videoDecoder.getInputBuffers();
        while ( !outputDone ){

            if (mIsStopRequested) {
                if(DEBUG) Log.d(TAG, "Stop requested, videoDecoder.releaseOutputBuffer(false)");
                doRender = false;
            }

            try {
                if(!mIsStopRequested) {
                    // Feed more data to the decoder.
                    videoFrame frameRawH264 = mRenderer.getVideoData();
                    if(frameRawH264 != null) {
                        int inputBufIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                        if (inputBufIndex >= 0) {
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            inputBuf.clear();
                            inputBuf.rewind();
                            inputBuf.put(frameRawH264.getData(), 0, frameRawH264.getData().length);
                            videoDecoder.queueInputBuffer(inputBufIndex, 0, frameRawH264.getData().length,
                                    frameRawH264.getTimestamp(), 0);
                        } else {
                            if(DEBUG) Log.d(TAG, "VideoDecoder input buffer not available");
                        }
                    }
                } else {
                    if(DEBUG) Log.d(TAG, "Stop requested,send BUFFER_FLAG_END_OF_STREAM to videoDecoder");
                    int inputBufIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        inputBuf.clear();
                        inputBuf.rewind();
                        videoDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }


                // dequeue And Render OutputBuffer
                int decoderStatus = videoDecoder.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    // if (DEBUG) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (DEBUG) Log.d(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = videoDecoder.getOutputFormat();
                    if (DEBUG) Log.d(TAG, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw new RuntimeException(
                            "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (DEBUG) Log.d(TAG, "output EOS");
                        outputDone = true;
                    }
                    videoDecoder.releaseOutputBuffer(decoderStatus, doRender);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(DEBUG) Log.d(TAG, "Decoder thread exiting");
        synchronized (mLock) {
            mReady = mRunning = false;
        }
    }

}
