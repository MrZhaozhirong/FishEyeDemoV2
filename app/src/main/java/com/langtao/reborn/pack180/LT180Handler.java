package com.langtao.reborn.pack180;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by zzr on 2018/1/31.
 */

public class LT180Handler extends Handler {

    static final int START_VIDEO_SEARCH = 0x01;
    static final int REQUEST_PLAY_VIDEO = 0x02;
    static final int STOP_VIDEO_SEARCH = 0x03;
    static final int PLAY_REMOTE_FILE = 0x04;

    private WeakReference<LangTao180Activity> mActivityWrf;

    public LT180Handler(LangTao180Activity activity){
        mActivityWrf = new WeakReference<LangTao180Activity>(activity);
    }


    @Override
    public void handleMessage(Message msg) {
        LangTao180Activity lt180Activity = mActivityWrf.get();
        if(lt180Activity == null)
            return;

        switch (msg.what) {
            case START_VIDEO_SEARCH:
                lt180Activity.startVideoSearch();
                break;
            case STOP_VIDEO_SEARCH:
                lt180Activity.stopVideoSearch();
                break;
            case REQUEST_PLAY_VIDEO:
                String fileName = (String) msg.obj;
                lt180Activity.requestPlayVideo(fileName);
                break;
            case PLAY_REMOTE_FILE:
                String fileName2 = (String) msg.obj;
                lt180Activity.playRemoteFile(fileName2);
                break;
        }
    }
}
