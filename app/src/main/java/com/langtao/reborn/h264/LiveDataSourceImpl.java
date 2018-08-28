package com.langtao.reborn.h264;


import android.os.Handler;
import android.os.Message;

import com.langtao.codec.videoFrame;

import java.util.Locale;

import glnk.client.DataSourceListener2;
import glnk.client.GlnkStreamFormat;

/**
 * Created by zzr on 2017/12/21.
 */

public class LiveDataSourceImpl extends DataSourceListener2 {
    private Handler mHandler;
    private LangTaoDecoder mRenderer;

    public LiveDataSourceImpl(Handler handler, LangTaoDecoder render) {
        mHandler = handler;
        mRenderer = render;
    }

    @Override
    public void onAVStreamFormat(byte[] data){
        GlnkStreamFormat fmt = GlnkStreamFormat.getGlnkStreamFormat(data);
        String s = String.format(Locale.CHINA,"onAVStreamFormat\nvideoFmt:%x,frameRate:%d, width:%d, height:%d, frameInterval:%d, audioFmt:%d, channels:%d, sampleRate:%d",
                fmt.getVideofmt(),fmt.getVideoFramerate(), fmt.getVideoWidth(), fmt.getVideoHeight(), fmt.getVideoIFrameInterval(), fmt.getAudiofmt(), fmt.getAudioChannels(), fmt.getAudioSampleRate());
        if(mHandler!= null ) {
            Message message = mHandler.obtainMessage(H264Handler.LOG_MSG);
            message.obj = s+"\n";
            mHandler.sendMessage(message);
        }
        if(mRenderer != null ){
            mRenderer.setGlnkStreamFormat(fmt);
            mRenderer.prepare();
            mRenderer.play();
        }
    }

    @Override
    public void onVideoData(byte[] data, int frameIndex, int timestamp, boolean isIFrame){
        String s = "onVideoData frameIndex:"+frameIndex+" timestamp:"+timestamp+" isIFrame:"+isIFrame;
        System.out.println(s + "   "+data[4]);
        if(mRenderer != null ){
            videoFrame frame = new videoFrame();
            frame.setData(data);
            frame.setFrameIndex(frameIndex);
            frame.setTimestamp(timestamp);
            frame.setIFrame(isIFrame);
            mRenderer.addVideoData(frame);
        }
    }


    @Override
    public void onDataRate(int bytesPersecond) {
        super.onDataRate(bytesPersecond);
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(H264Handler.SHOW_RATE);
            message.obj = bytesPersecond ;
            mHandler.sendMessage(message);
        }
    }

    @Override
    protected void onVideoDataByManu(byte[] data, byte[] frameInfo,
                                     int frameIndex, int timestamp, int isIFrame) {
        String s = "onVideoDataByManu frameIndex:"+frameIndex+" timestamp:"+timestamp+" isIFrame:"+isIFrame;
        System.out.println(s);
    }

    @Override
    public void onAudioData(byte[] data, int timestamp){
        //System.out.println("onAudioData timestamp: " + timestamp);
        //if(mRenderer != null ){
        //    audioFrame frame = new audioFrame();
        //    frame.setData(data);
        //    frame.setTimestamp(timestamp);
        //    mRenderer.addAudioData(frame);
        //}
    }

    @Override
    public void onConnecting(){
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(H264Handler.LOG_MSG);
            message.obj = "LiveDataSource  onConnecting ... "+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onConnected(int mode, String ip, int port){
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(H264Handler.LOG_MSG);
            message.obj = "LiveDataSource  onConnected ... "+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onModeChanged(int mode, String ip, int port){
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(H264Handler.LOG_MSG);
            message.obj = "LiveDataSource  onModeChanged : "+mode+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onDisconnected(int errcode){
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(H264Handler.LOG_MSG);
            message.obj = "LiveDataSource  onDisconnected : "+errcode+"\n";
            mHandler.sendMessage(message);
        }
    }


    @Override
    protected void onOpenVideoProcess(int nProcess) {
        // TODO Auto-generated method stub
        super.onOpenVideoProcess(nProcess);

        String sProStateStr = "";
        if (nProcess == 1)
        {
            sProStateStr = "启动 p2p udp";
        }
        else if (nProcess == 2)
        {
            sProStateStr = "启动 p2p tcp";
        }
        else if (nProcess == 3)
        {
            sProStateStr = "启动conn fwdsvr流程";
        }
        else if (nProcess == 4)
        {
            sProStateStr = "开始连接goosvr获取fwdsvr地址";
        }
        else if (nProcess == 5)
        {
            sProStateStr = "开始连接fwdsvr";
        }
        else if (nProcess == 6)
        {
            sProStateStr = "未从lbs获取到 goosvr addr";
        }
        else if (nProcess == 7)
        {
            sProStateStr = "已连接到goosvr,开始请求fwd地址";
        }
        else if (nProcess == 8)
        {
            sProStateStr = "连接goosvr 失败";
        }
        else if (nProcess == 9)
        {
            sProStateStr = "已连接到goosvr,等待数据返回失败，重新连接goosvr";
        }
        else if (nProcess == 10)
        {
            sProStateStr = "断开fwd连接";
        }
        else if (nProcess == 11)
        {
            sProStateStr = "fwd已连接，开始发送登录设备请求";
        }
        else if (nProcess == 13)
        {
            sProStateStr = "请求fwd连接失败";
        }

        if (sProStateStr != "")
        {
            Message message = mHandler.obtainMessage(H264Handler.LOG_MSG);
            message.obj = sProStateStr+"\n";
            mHandler.sendMessage(message);
        }
    }


    @Override
    public void onPermision(int i) { }
}
