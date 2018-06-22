package com.langtao.device;

import android.app.Application;

/**
 * Created by zzr on 2017/8/3.
 */

public class GlnkApplication extends Application {

    private static GlnkApplication glnkApplication;

    @Override
    public void onCreate() {
        super.onCreate();

        glnkApplication = this;

        SDKinitUtil.initGlnkSDK();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SDKinitUtil.release();
    }

    public static GlnkApplication getApp() {
        return glnkApplication;
    }



    public static float px2dip(float pxValue) {
        if(getApp()!=null) {
            float scale = getApp().getResources().getDisplayMetrics().density;
            return (int) (pxValue / scale + 0.5f);
        }
        return pxValue;
    }

    public static float dip2px(float dpValue) {
        if(getApp()!=null) {
            float scale = getApp().getResources().getDisplayMetrics().density;
            return (int) (dpValue * scale + 0.5f);
        }
        return dpValue;
    }

}
