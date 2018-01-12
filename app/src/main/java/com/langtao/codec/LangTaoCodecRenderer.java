package com.langtao.codec;

import android.view.Surface;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import glnk.client.GlnkStreamFormat;

/**
 * Created by zzr on 2017/12/22.
 */

public class LangTaoCodecRenderer  {

    private GlnkStreamFormat liveStreamFmt;
    private static volatile BlockingQueue<videoFrame> video_data_queue = new ArrayBlockingQueue<videoFrame>(1000);
    private static volatile BlockingQueue<audioFrame> audio_data_queue = new ArrayBlockingQueue<audioFrame>(1000);

    private Surface mOutputSurface;
    private VideoDecoder videoDecoder;
    //private AudioDecoder audioDecoder;

    private boolean isPrepare ;
    private boolean isPlaying ;


    public LangTaoCodecRenderer(Surface outputSurface) {
        mOutputSurface = outputSurface;
        videoDecoder = new VideoDecoder(this);
        isPrepare = false;
        isPlaying = false;
    }

    public void prepare() {
        if(!isPrepare){
            videoDecoder.prepareDecoder(mOutputSurface);
            //audioDecoder
            isPrepare = true;
        }
    }

    public void play() {
        if (isPrepare && !isPlaying) {
            videoDecoder.execute();
            //audioDecoder
            isPlaying = true;
        }
    }

    public void stopPlay() {
        if(isPlaying) {
            videoDecoder.requestStop();
            //audioDecoder
            isPlaying = false;
        }
    }

    public void release() {
        videoDecoder.release();
    }
    //添加视频数据
    public void addVideoData(videoFrame data) {
        try {
            video_data_queue.put(data);// offer
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public videoFrame getVideoData() throws InterruptedException {

        return video_data_queue.poll(500, TimeUnit.NANOSECONDS);// offer
    }
    //添加音频数据
    public void addAudioData(audioFrame data) {
        try {
            audio_data_queue.put(data);// offer
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public GlnkStreamFormat getLiveStreamFmt() {
        return liveStreamFmt;
    }

    public void setGlnkStreamFormat(GlnkStreamFormat fmt) {
        this.liveStreamFmt = fmt;
    }
}
