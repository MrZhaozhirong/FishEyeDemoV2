package com.langtao.device;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;

import glnk.io.OnDeviceStatusChangedListener;


/**
 *  Created by nicky on 2017/5/25.
 */

public class DeviceStatusManager implements OnDeviceStatusChangedListener {

    private static final String TAG = "DeviceStatusManager";
    private static DeviceStatusManager instance = null;

    private static ConcurrentHashMap<String, DeviceStatusObject>
            statusMap = new ConcurrentHashMap<String, DeviceStatusObject>();

    public ConcurrentHashMap<String, DeviceStatusObject> getStatusMap() {
        return statusMap;
    }

    public static final String DSM_ON_CHANGED_CALL = "com.langtao.DeviceStatusManager.OnChanged.CALL";
    public static final String DSM_ON_PUSH_SVRINFO_CALL = "com.langtao.DeviceStatusManager.onPushSvrInfo.CALL";
    public static final String DSM_ON_DEV_FUN_INFO_CALL = "com.langtao.DeviceStatusManager.onDevFunInfo.CALL";
    public static final String DEV_ID = "devId";

    public static DeviceStatusManager getInstance() {
        if(instance == null) {
            instance = new DeviceStatusManager();
        }
        return instance;
    }


    @Override
    public void onChanged(String devId, int nDevStatus) {
        Log.d(TAG, "devId : "+devId+"      nDevStatus : "+nDevStatus     );
        if( statusMap.containsKey(devId) ){
            DeviceStatusObject deviceStatusObject = statusMap.get(devId);
            deviceStatusObject.setDevStatus(nDevStatus);
            statusMap.replace(devId, deviceStatusObject);
        }else{
            DeviceStatusObject deviceStatusObject = new DeviceStatusObject();
            deviceStatusObject.setDevStatus(nDevStatus);
            statusMap.put(devId, deviceStatusObject);
        }
        GlnkApplication app = GlnkApplication.getApp();
        if(app!=null) {
            Intent intent = new Intent(DSM_ON_CHANGED_CALL);
            intent.putExtra(DEV_ID, devId);
            LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
        }
    }


    @Override
    public void onPushSvrInfo(String devId, String sPushSvrIp, int nPushSvrPort) {
        Log.d(TAG, "devId : "+devId+"      sPushSvrIp : "+sPushSvrIp     );
        if( statusMap.containsKey(devId) ){
            DeviceStatusObject deviceStatusObject = statusMap.get(devId);
            deviceStatusObject.setDevPushSvrIp(sPushSvrIp);
            deviceStatusObject.setDevPushSvrPort(nPushSvrPort);
            statusMap.replace(devId, deviceStatusObject);
        }else{
            DeviceStatusObject deviceStatusObject = new DeviceStatusObject();
            deviceStatusObject.setDevPushSvrIp(sPushSvrIp);
            deviceStatusObject.setDevPushSvrPort(nPushSvrPort);
            statusMap.put(devId, deviceStatusObject);
        }

        GlnkApplication app = GlnkApplication.getApp();
        if(app!=null) {
            Intent intent = new Intent(DSM_ON_PUSH_SVRINFO_CALL);
            intent.putExtra(DEV_ID, devId);
            LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
        }
    }



    private Gson gson ;
    @Override
    public void onDevFunInfo(String devId, String sDevFunInfo) {
        if (gson == null){
            gson = new Gson();
        }
        try{
            final DevCapability devFunInfo = gson.fromJson(sDevFunInfo, DevCapability.class);
            Log.w(TAG, "devId : "+devId+"   devFunInfo : "+devFunInfo.toString());

            if( statusMap.containsKey(devId) ){
                DeviceStatusObject deviceStatusObject = statusMap.get(devId);
                deviceStatusObject.setDevFunInfo(devFunInfo);
                statusMap.replace(devId, deviceStatusObject);
            }else{
                DeviceStatusObject deviceStatusObject = new DeviceStatusObject();
                deviceStatusObject.setDevFunInfo(devFunInfo);
                statusMap.put(devId, deviceStatusObject);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        GlnkApplication app = GlnkApplication.getApp();
        if(app!=null) {
            Intent intent = new Intent(DSM_ON_DEV_FUN_INFO_CALL);
            intent.putExtra(DEV_ID, devId);
            LocalBroadcastManager.getInstance(app).sendBroadcast(intent);
        }
    }

    @Override
    public void onDevNewLbs(String s, int i) {

    }

    @Override
    public void onDevStVersion(String s, int i) {

    }

    @Override
    public void onStAuthResult(String s) {

    }

    @Override
    public void onStDevList(String s, String s1) {

    }

    @Override
    public void onDevVersion(String s, String s1) {

    }

    @Override
    public void onDevLbsSvrTimeStamp(String s, int i) {

    }

    @Override
    public void onDevDoorDellWebDomain(String s, String s1, int i) {

    }


    public class DeviceStatusObject{
        int devStatus;
        String devPushSvrIp;
        int devPushSvrPort;
        DevCapability devFunInfo;

        public DevCapability getDevFunInfo() {
            return devFunInfo;
        }

        public void setDevFunInfo(DevCapability devFunInfo) {
            this.devFunInfo = devFunInfo;
        }

        public int getDevStatus() {
            return devStatus;
        }

        public void setDevStatus(int devStatus) {
            this.devStatus = devStatus;
        }

        public String getDevPushSvrIp() {
            return devPushSvrIp;
        }

        public void setDevPushSvrIp(String devPushSvrIp) {
            this.devPushSvrIp = devPushSvrIp;
        }

        public int getDevPushSvrPort() {
            return devPushSvrPort;
        }

        public void setDevPushSvrPort(int devPushSvrPort) {
            this.devPushSvrPort = devPushSvrPort;
        }
    }
}
