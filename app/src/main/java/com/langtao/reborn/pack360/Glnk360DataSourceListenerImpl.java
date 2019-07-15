package com.langtao.reborn.pack360;

import android.util.Log;

import glnk.media.GlnkDataSourceListener;

/**
 * Created by zzr on 2017/12/8.
 */


public class Glnk360DataSourceListenerImpl implements GlnkDataSourceListener {

    private static final String TAG = LangTao360Activity.TAG;

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
        Log.d(TAG, "DataSource  onConnecting ... "+"\n");
    }

    @Override
    public void onGetFwdAddr(int i, String s, int i1) {

    }

    @Override
    public void onConnected(int i, String s, int i1) {
        Log.d(TAG, "DataSource  onConnected ... "+s+"\n");
    }

    @Override
    public void onAuthorized(int i) {
        Log.d(TAG, "DataSource  onAuthorized ... "+i+"\n");
    }

    @Override
    public void onPermision(int i) {

    }

    @Override
    public void onModeChanged(int i, String s, int i1) {

    }

    @Override
    public void onDisconnected(int i) {
        Log.d(TAG, "DataSource  onDisconnected ... "+i+"\n");
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
