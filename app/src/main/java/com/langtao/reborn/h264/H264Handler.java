package com.langtao.reborn.h264;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by zzr on 2017/12/21.
 */

class H264Handler extends Handler {
    static final int LOG_MSG = 1;
    static final int SHOW_RATE = 2;

    WeakReference<RawH264Activity> mActivity;

    public H264Handler(RawH264Activity activity){
        mActivity = new WeakReference<RawH264Activity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        RawH264Activity activity = mActivity.get();
        if(activity==null)
            return;
        switch (msg.what) {
            case LOG_MSG: {
                String s = (String) msg.obj;
                activity.refreshLogView(s);
                break;
            }
            case SHOW_RATE: {
                activity.updateDataRate((Integer) msg.obj);
                break;
            }
            default:
                super.handleMessage(msg);
                break;
        }
    }
}
