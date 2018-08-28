package com.langtao.reborn.pack720;

import android.os.Message;

import glnk.media.GlnkDataSourceListener;

/**
 * Created by zzr on 2017/12/1.
 */

public class GlnkDataSourceListenerImpl implements GlnkDataSourceListener {

    private LT720Handler mHandler;

    public GlnkDataSourceListenerImpl(LT720Handler handler) {
        mHandler =  handler;
    }

    @Override
    public void onTalkingResp(int i) {

    }

    @Override
    public void onIOCtrl(int i, byte[] bytes) {

    }

    @Override
    public void onIOCtrlByManu(byte[] bytes) {

    }

    @Override
    public void onRemoteFileResp(int i, int i1, int i2) {

    }

    @Override
    public void onRemoteFileEOF() {

    }

    @Override
    public void onConnecting() {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "DataSource  onConnecting ... "+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onConnected(int i, String s, int i1) {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "DataSource  onConnected ... "+s+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onAuthorized(int i) {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "DataSource  onAuthorized ... "+i+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onPermision(int i) {

    }

    @Override
    public void onModeChanged(int i, String s, int i1) {

    }

    @Override
    public void onDisconnected(int i) {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "DataSource  disconnected ... "+i+"\n";
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void onDataRate(int i) {

    }

    @Override
    public void onReConnecting() {

    }

    @Override
    public void onEndOfFileCtrl(int i) {

    }

    @Override
    public void onLocalFileOpenResp(int i, int i1) {

    }

    @Override
    public void onLocalFilePlayingStamp(int i) {

    }

    @Override
    public void onLocalFileEOF() {

    }

    @Override
    public void onOpenVideoProcess(int i) {

    }

    @Override
    public void onVideoFrameRate(int i) {

    }

    @Override
    public void onAppVideoFrameRate(int i) {

    }

    @Override
    public void onJsonDataRsp(byte[] bytes) {

    }
}
