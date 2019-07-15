package com.langtao.reborn.pack180;

import android.os.Handler;
import android.os.Message;
import android.util.Log;


import glnk.media.GlnkDataSourceListener;


/**
 * Created by zzr on 2018/1/31.
 */

public class VideoSearchDataSourceListenerImpl implements GlnkDataSourceListener {
    private static final String TAG = LangTao180Activity.TAG;

    private String playbackVideoFilename;

    public void setPlaybackVideoFilename(String playbackVideoFilename) {
        this.playbackVideoFilename = playbackVideoFilename;
    }

    private Handler handler;

    public VideoSearchDataSourceListenerImpl(LT180Handler handler){
        this.handler = handler;
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
        Log.d(TAG, "VideoSearch DataSource  onConnecting ... "+"\n");
    }

    @Override
    public void onGetFwdAddr(int i, String s, int i1) {

    }

    @Override
    public void onConnected(int i, String s, int i1) {
        Log.d(TAG, "VideoSearch DataSource  onConnected ... "+s+"\n");
    }

    @Override
    public void onAuthorized(int i) {
        Log.d(TAG, "VideoSearch DataSource  onAuthorized ... "+i+"\n");
        if (i==1 && handler!=null) {
            Message msg1 = new Message();
            msg1.what = LT180Handler.PLAY_REMOTE_FILE;
            msg1.obj = playbackVideoFilename;
            handler.sendMessage(msg1);
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
        Log.d(TAG, "VideoSearch DataSource  onDisconnected ... "+i+"\n");
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
    public void onRemoteFileCtrlResp2(int i, int i1) {

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
