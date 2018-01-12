package com.langtao.device;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Build;
import android.util.Log;

import glnk.client.GlnkClient;

/**
 * Created by shanlin on 2016/3/3.
 */
public class SDKinitUtil {

    private static int count = 0;

    public static GlnkClient gClient = null;

    public static void initGlnkSDK(){
        if(count > 0) {
            Log.w("SDKinitUtil", "SDKinitUtil Already initialized");
            return;
        }
        if(!GlnkClient.supported()){
            throw new RuntimeException("暂不支持的手机");
        }
        if(gClient==null){
            gClient = GlnkClient.getInstance();
        }
        GlnkApplication app = GlnkApplication.getApp();
        if(app == null){
            Log.w("SDKinitUtil", "GlnkApplication has been destroy");
            return;
        }
        gClient.init(app, "langtao", "20140909", "1234567890", 101, 1);
        gClient.setStatusAutoUpdate(true);
        gClient.setOnDeviceStatusChangedListener(DeviceStatusManager.getInstance());
        gClient.setAppKey("yTLzaJ5iWdtOUMd5/K7l4IDn/9hFALSXr8ghZA=="); //applicationId "com.langtao.fisheye"
        //gClient.setAppKey("6Dfua7h3VcJDS9Y0/6Kl65Xo94JCBumJ880=");      //applicationId "com.langtao.v9"
        gClient.start();
        count++;
        Log.d("SDKinitUtil", "SDKInitUtil initializ done ... ...");
    }

    public static void release(){
        if(gClient == null || count == 0){
            return;
        }
        gClient.setOnDeviceStatusChangedListener(null);
        gClient.release();
        count = 0;
        gClient = null;
        Log.d("SDKinitUtil", "SDKInitUtil release ... ...");
    }

    public static boolean checkGLEnvironment() {
        ActivityManager activityManager =
                (ActivityManager) GlnkApplication.getApp().getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo deviceConfigurationInfo =
                activityManager.getDeviceConfigurationInfo();
        int reqGlEsVersion = deviceConfigurationInfo.reqGlEsVersion;
        final boolean supportsEs2 =
                reqGlEsVersion >= 0x20000
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86")));
        return supportsEs2;
    }
}
