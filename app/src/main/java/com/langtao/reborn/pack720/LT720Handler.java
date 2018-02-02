package com.langtao.reborn.pack720;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by zzr on 2017/12/1.
 */

class LT720Handler extends Handler {
    final int LOG_MSG = 1;
    final int CLOSE_CONNECT = 2;

    final int VIDEO_ONAUTH = 3;
    final int VIDEO_SEARCH = 4;
    final int VIDEO_DISCONNECT = 5;

    final int DELAY_INIT_GL = 6;

    WeakReference<LangTao720Activity> mActivity;

    public LT720Handler(LangTao720Activity activity){
        mActivity = new WeakReference<LangTao720Activity>(activity);
    }


    @Override
    public void handleMessage(Message msg) {
        LangTao720Activity LangTao720Activity = mActivity.get();
        if(LangTao720Activity==null)
            return;
        switch (msg.what) {
            case DELAY_INIT_GL:{
                LangTao720Activity.initGLSurfaceView();
                break;
            }
            case VIDEO_SEARCH:{
                //搜索模板文件
                LangTao720Activity.PanoFileDownload("pano.tem");
                //自动回调GlnkVodSearchDataSourceImpl 的 onPanoFileDownload
                break;
            }
            case VIDEO_ONAUTH:{
                int result = msg.arg1;
                if (result == 1) {// 登录成功
                    LangTao720Activity.refreshLogView("start search pano.tem ..."+"\n"+"\n");
                    this.sendEmptyMessageDelayed(VIDEO_SEARCH, 1000);
                } else { //登录失败,关闭通道
                    LangTao720Activity.disconnectDevice();
                }
                break;
            }
            case VIDEO_DISCONNECT: {
                LangTao720Activity.disconnectDevice();
                break;
            }
            case CLOSE_CONNECT: {
                LangTao720Activity.close_connect();
                break;
            }
            case LOG_MSG: {
                String s = (String) msg.obj;
                LangTao720Activity.refreshLogView(s+"\n");
                break;
            }
            default:
                super.handleMessage(msg);
                break;
        }
    }
}
