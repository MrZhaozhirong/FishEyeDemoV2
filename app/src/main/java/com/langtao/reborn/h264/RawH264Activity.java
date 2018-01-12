package com.langtao.reborn.h264;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.langtao.codec.LangTaoCodecRenderer;
import com.langtao.device.DevCapability;
import com.langtao.device.DeviceStatusManager;
import com.langtao.reborn.R;

import java.util.concurrent.ConcurrentHashMap;

import glnk.client.GlnkChannel;
import glnk.client.GlnkClient;


/**
 * Created by zzr on 2017/12/21.
 */

public class RawH264Activity extends Activity implements TextureView.SurfaceTextureListener {
    public static final String TAG = "RawH264Activity";
    private TextView logView;
    // int StreamType = 0;
    // if(录像文件 != null || 录像时间 != null){
    //     StreamType = 2;
    // }
    // dataType流数据类型, 0-视频流, 1-音频流, 2-音视频流
    private int channelNo=0, streamType=0, dataType=2;

    DeviceStatusReceiver deviceStatusReceiver = new DeviceStatusReceiver();


    private static H264Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_h264);
        logView=(TextView)findViewById(R.id.logView2);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logView.setTextColor(0x50000000);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceStatusManager.DSM_ON_CHANGED_CALL);
        filter.addAction(DeviceStatusManager.DSM_ON_PUSH_SVRINFO_CALL);
        filter.addAction(DeviceStatusManager.DSM_ON_DEV_FUN_INFO_CALL);
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceStatusReceiver,filter);

        handler = new H264Handler(RawH264Activity.this);

        TextureView live_stream_view = (TextureView) findViewById(R.id.live_stream_view);
        live_stream_view.setSurfaceTextureListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean finishing = isFinishing();
        Log.d(TAG, "onPause isFinishing: " + finishing);
        close_connect();
        Log.d(TAG, "onPause close_connect");
        mRenderer.stopPlay();
        Log.d(TAG, "onPause mRenderer.stopPlay");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceStatusReceiver);
        mRenderer.release();
    }


    public void updateDataRate(int bytesPersecond) {
        TextView tv_data_rate = (TextView) findViewById(R.id.tv_data_rate);
        tv_data_rate.setText("Rate "+bytesPersecond);
    }

    public void clickAddGid(@SuppressLint("USELESS") View view) {
        EditText gid = (EditText) findViewById(R.id.gid);
        GlnkClient.getInstance().addGID(gid.getText().toString());
    }

    public void clickConnect(@SuppressLint("USELESS") View view) {
        EditText gid = (EditText) findViewById(R.id.gid);
        EditText account = (EditText) findViewById(R.id.account);
        EditText password = (EditText) findViewById(R.id.password);
        open_connect(gid.getText().toString(),
                account.getText().toString(), password.getText().toString(),
                channelNo, streamType, dataType);
    }



    private LangTaoCodecRenderer mRenderer;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable : " + st );
        mRenderer = new LangTaoCodecRenderer(new Surface(st));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged size=" + width + "x" + height + ", st=" + surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////   链接直播视频源   ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private GlnkChannel liveChannel = null;
    private LiveDataSourceImpl liveSource = null;
    void close_connect() {
        if (liveChannel!=null) {
            //liveChannel.stopStream();
            liveChannel.stop();
            liveChannel.release();
            liveChannel = null;
        }
    }
    void open_connect(String gid, String username, String password,
                      int channelNo, int streamType, int dataType){
        if(mRenderer == null ) return;
        if (liveChannel!=null) {
            liveChannel.stopStream();
            liveChannel.stop();
            liveChannel.release();
            liveChannel = null;
        }
        if(liveSource==null)
            liveSource = new LiveDataSourceImpl(handler/*null*/, mRenderer);

        liveChannel = new GlnkChannel(liveSource);
        liveChannel.setMetaData(gid, username, password, channelNo, streamType, dataType);
        liveChannel.start();
    }

    private class DeviceStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String devID = intent.getStringExtra(DeviceStatusManager.DEV_ID);
            EditText gid = (EditText) findViewById(R.id.gid);
            if(devID.equalsIgnoreCase(gid.getText().toString())){
                ConcurrentHashMap<String, DeviceStatusManager.DeviceStatusObject> statusMap =
                        DeviceStatusManager.getInstance().getStatusMap();

                DeviceStatusManager.DeviceStatusObject deviceStatusObject = statusMap.get(devID);
                if(deviceStatusObject!=null){
                    DevCapability devFunInfo = deviceStatusObject.getDevFunInfo();
                    if(devFunInfo!=null ) {
                        refreshLogView((devID + " status:" + deviceStatusObject.getDevStatus())+"\n");
                        //refreshLogView(devFunInfo.toString()+"\n");
                    }
                }
            }

        }
    }

    public void refreshLogView(String msg){
        logView.append(msg);
        int offset=logView.getLineCount()*logView.getLineHeight();
        if(offset>logView.getHeight()){
            logView.scrollTo(0,offset-logView.getHeight());
        }
    }
}
