package com.langtao.reborn.pack180;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.langtao.device.SDKinitUtil;
import com.langtao.ltpanorama.LangTao180RenderMgr;
import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.reborn.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import glnk.client.GlnkChannel;
import glnk.client.GlnkClient;
import glnk.media.AViewRenderer;
import glnk.media.GlnkDataSource;
import glnk.media.GlnkPlayer;
import glnk.media.VideoRenderer;


/**
 * Created by zzr on 2017/12/8.
 */

public class LangTao180Activity extends Activity {
    public static final String TAG = "LangTao180Activity";
    private RelativeLayout gl_view_container;
    private GLSurfaceView gl_view;
    private EditText et_cut_percent;
    private EditText et_radio_percent;
    private EditText et_view_distance;
    private EditText et_range_angle;
    private LT180Handler handler;
    //nStreamType实时流，码流类型， 0-主码流； 1-次码流
    //dataType流数据类型, 0-视频流, 1-音频流, 2-音视频流
    private int channelNo=0, streamType=0, dataType=2;

    private LangTao180RenderMgr mLT180RenderMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, TAG+" onCreate");
        setContentView(R.layout.activity_180);

        gl_view_container = (RelativeLayout) findViewById(R.id.gl_view_container);
        et_cut_percent = (EditText) findViewById(R.id.cut_percent);
        et_radio_percent = (EditText) findViewById(R.id.ratio_percent);
        et_view_distance = (EditText) findViewById(R.id.view_distance);
        et_range_angle = (EditText) findViewById(R.id.range_angle);

        handler= new LT180Handler(LangTao180Activity.this);

        initGLSurfaceView();
    }

    private void initGLSurfaceView() {
        gl_view_container.removeAllViews();
        if( SDKinitUtil.checkGLEnvironment() ){
            if (mLT180RenderMgr==null)
                mLT180RenderMgr = new LangTao180RenderMgr();
            mLT180RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_180);
            gl_view = new GLSurfaceView(LangTao180Activity.this);
            gl_view.setEGLContextClientVersion(2);
            gl_view.setRenderer(mLT180RenderMgr);
            gl_view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            gl_view.setVisibility(View.VISIBLE);
            gl_view.setClickable(true);
            gl_view.setOnTouchListener(new GLViewTouchListener());
            gl_view.onResume();
            gl_view_container.addView(gl_view);
        } else {
            Toast.makeText(this, "this device does not support OpenGL ES 2.0",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(TAG, TAG+" onResume");
        if( gl_view!=null){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gl_view.onResume();
                    float distance = mLT180RenderMgr.getPavedRect().getDistance();
                    et_view_distance.setText(String.valueOf(distance));
                }
            }, 2000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, TAG+" onPause");
        if(gl_view!=null) gl_view.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, TAG+" onDestroy");
        //disconnectDevice();
        close_connect();
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
        try{
            //Warm：在连接视频前调用。连接后无效。
            String s = et_cut_percent.getText().toString();
            mLT180RenderMgr.getPavedRect().setCutPercent(Float.parseFloat(s));
            String s1 = et_radio_percent.getText().toString();
            mLT180RenderMgr.getPavedRect().setRadioPercent(Float.parseFloat(s1));
            String s2 = et_range_angle.getText().toString();
            mLT180RenderMgr.getPavedRect().setRangeAngle(Float.parseFloat(s2));
        }catch (Exception e){
            e.getMessage();
        }
    }

    public void clickSetDistance(@SuppressLint("USELESS") View view) {
        try{
            String s = et_view_distance.getText().toString();
            // 即时生效，任意时刻调用。
            mLT180RenderMgr.getPavedRect().setDistance(Float.parseFloat(s));
        }catch (Exception e){
            e.getMessage();
        }
    }

    public void clickNear(@SuppressLint("USELESS") View view) {
        mLT180RenderMgr.getPavedRect().nearDistance();
        float distance = mLT180RenderMgr.getPavedRect().getDistance();
        et_view_distance.setText(String.valueOf(distance));
    }
    public void clickFar(@SuppressLint("USELESS") View view) {
        mLT180RenderMgr.getPavedRect().farDistance();
        float distance = mLT180RenderMgr.getPavedRect().getDistance();
        et_view_distance.setText(String.valueOf(distance));
    }

    public void clickCurved(@SuppressLint("USELESS")  View view) {
        if( mLT180RenderMgr != null){
            mLT180RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_180);
        }
    }

    public void clickPaved(@SuppressLint("USELESS") View view) {
        if( mLT180RenderMgr != null){
            mLT180RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_180_PAVED);
        }
    }

    private class GLViewTouchListener implements View.OnTouchListener {
        private float oldDist;
        private int mode = 0;
        private long lastClickTime;
        private VelocityTracker mVelocityTracker = null;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // -------------判断多少个触碰点---------------------------------
            switch (event.getAction() & MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_DOWN:
                    mode = 1;
                    break;
                case MotionEvent.ACTION_UP:
                    mode = 0;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    mode = 0;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    mode = 2;
                    break;
            }

            if(event.getAction() == MotionEvent.ACTION_DOWN){
                //普通点击
                if (mode == 1) {
                    final float x = event.getX();
                    final float y = event.getY();
                    gl_view.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            mLT180RenderMgr.handleTouchDown(x,y);
                        }
                    });
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                }

                if (System.currentTimeMillis() - lastClickTime < 500) {
                    gl_view.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            mLT180RenderMgr.handleDoubleClick();
                        }
                    });
                    lastClickTime = 0;
                } else {
                    lastClickTime = System.currentTimeMillis();
                }
            }else if(event.getAction() ==MotionEvent.ACTION_MOVE){
                if(mode == 1){
                    final float x = event.getX();
                    final float y = event.getY();
                    gl_view.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            mLT180RenderMgr.handleTouchMove(x,y);
                        }
                    });
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                }
                if (mode == 2) {
                    //双指操作
                    float newDist = spacing(event);
                    if ( (newDist > oldDist + 10) || (newDist < oldDist - 15) ) {
                        final float distance = newDist - oldDist;
                        gl_view.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                mLT180RenderMgr.handleMultiTouch(distance);
                            }
                        });
                        oldDist = newDist;
                    }
                }
            }else if(event.getAction() == MotionEvent.ACTION_UP){
                final float x = event.getX();
                final float y = event.getY();
                final float xVelocity = mVelocityTracker.getXVelocity();
                final float yVelocity = mVelocityTracker.getYVelocity();
                gl_view.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mLT180RenderMgr.handleTouchUp(x, y, xVelocity, yVelocity);
                    }
                });
            }

            return true;
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////   截图显示   //////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void clickCaptureScreen(@SuppressLint("USELESS") View view) {
        final ImageView someImageView = (ImageView) this.findViewById(R.id.someImageView);

        mLT180RenderMgr.requestCaptureScreen(0, 0, gl_view.getWidth(), gl_view.getHeight(),
                new LangTao180RenderMgr.CaptureScreenCallbacks() {
                    @Override
                    public void onCaptureScreenReady(final int w, final int h, final int[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap bitmap = Bitmap.createBitmap(data, w, h, Bitmap.Config.ARGB_8888);
                                someImageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                });
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////   搜索录像回放   //////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void clickSearchVodPlayback(@SuppressLint("USELESS") View view) {
        EditText et_gid = (EditText) findViewById(R.id.gid);
        EditText et_account = (EditText) findViewById(R.id.account);
        EditText et_password = (EditText) findViewById(R.id.password);
        if (vodChannel != null) {
            vodChannel.stop();
            vodChannel.release();
            vodChannel = null;
        }
        Log.w(TAG, "设备sd卡连接... ...");
        vodSource = new VideoSearchDataSourceImpl(handler);
        vodChannel = new GlnkChannel(vodSource);
        vodChannel.setMetaData(et_gid.getText().toString(),
                et_account.getText().toString(),
                et_password.getText().toString(),
                0, 2, 0);
        vodChannel.start();
    }
    public void disconnectDevice() {
        if (vodChannel != null) {
            vodChannel.stop();
            vodChannel.release();
            vodChannel = null;
        }
    }

    private GlnkChannel vodChannel;
    private VideoSearchDataSourceImpl vodSource;
    private VideoSearchDataSourceListenerImpl videoSearchListener;

    public void startVideoSearch() {
        EditText et_day = (EditText) findViewById(R.id.day);
        EditText et_hour = (EditText) findViewById(R.id.hour);
        EditText et_minute = (EditText) findViewById(R.id.minute);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date d = new Date();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(et_day.getText().toString()));
        c.set(Calendar.HOUR, Integer.parseInt(et_hour.getText().toString()));
        c.set(Calendar.MINUTE, Integer.parseInt(et_minute.getText().toString()));
        int rs = this.vodChannel.searchRemoteFile2(
                0xff,
                0xff,
                //start time
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR), c.get(Calendar.MINUTE), 0,
                //end time
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR)+23, c.get(Calendar.MINUTE)+59, 59);
        if (rs < 0) {
            Toast.makeText(this, "搜索文件失败", Toast.LENGTH_SHORT).show();
        }
    }
    public void stopVideoSearch() {
        if (vodChannel != null) {
            vodChannel.stop();
            vodChannel.release();
            vodChannel = null;
        }
    }

    public void requestPlayVideo(String fileName) {
        if (source != null) {
            source.stop();
            source.release();
            source = null;
        }
        source = new GlnkDataSource(GlnkClient.getInstance());
        source.getGlnkChannel().setReconnectable(false);
        EditText et_gid = (EditText) findViewById(R.id.gid);
        EditText et_account = (EditText) findViewById(R.id.account);
        EditText et_password = (EditText) findViewById(R.id.password);
        source.setMetaData(et_gid.getText().toString(),
                et_account.getText().toString(), et_password.getText().toString(),
                channelNo, streamType, dataType);
        if(videoSearchListener ==null) {
            videoSearchListener = new VideoSearchDataSourceListenerImpl(handler);
            videoSearchListener.setPlaybackVideoFilename(fileName);
        }
        source.setGlnkDataSourceListener(videoSearchListener);
        if(renderer!=null){
            renderer.release();
            renderer = null;
        }
        renderer = new AViewRenderer(LangTao180Activity.this, null);
        ((AViewRenderer) renderer).setCallBackDataType(AViewRenderer.CB_DATA_TYPE_YUV);
        ((AViewRenderer) renderer).setValidateYUVCallback(new AViewRenderer.ValidateYUVCallback() {
            @Override
            public void yuv_Callback(int width, int height, byte[] byYdata, int nYLen, byte[] byUdata, int nULen, byte[] byVdata, int nVLen) {
                //Log.d(TAG, "Note: yuv_Callback !!! ");
                if( mLT180RenderMgr != null ){
                    //Log.i(TAG, "mLT180RenderMgr  add_buffer !!!");
                    mLT180RenderMgr.addBuffer(width,height, byYdata,byUdata,byVdata);
                }
                if(requestDumpYuv){
                    new DumpYUVFrameFile(width,height,byYdata,byUdata,byVdata,"dump_video.yuv").start();
                }
            }
        });
        if( player!=null){
            player.stop();
            player.release();
            player = null;
        }
        player = new GlnkPlayer();
        player.prepare();
        player.setDataSource(source);
        player.setDisplay(renderer);
        player.start();
    }

    public void playRemoteFile(String playbackVideoFilename) {
        String[] split = playbackVideoFilename.split(",");
        if(split.length != 4){
            Log.w(TAG, "playbackVideoFilename 格式错误。");
            return;
        }
        int count = Integer.parseInt(split[0]);
        int recordType = Integer.parseInt(split[1]);
        String startTimeStr = split[2]; // "%d:%d:%d:%d:%d:%d"
        String endTimeStr = split[3];
        String[] startTime = startTimeStr.split(":");
        if(startTime.length != 6) {
            Log.w(TAG, "playbackVideoFilename 格式错误。");
            return;
        }
        int iStartYear = Integer.parseInt(startTime[0]);
        int iStartMonth = Integer.parseInt(startTime[1]);
        int iStartDay = Integer.parseInt(startTime[2]);
        int iStartHour = Integer.parseInt(startTime[3]);
        int iStartMinute = Integer.parseInt(startTime[4]);
        int iStartSec = Integer.parseInt(startTime[5]);
        int result_video_file_request = source.remoteFileRequest2(
                iStartYear, iStartMonth, iStartDay, iStartHour, iStartMinute, iStartSec);
        Log.w(TAG, "result_vodfile_request = "
                + result_video_file_request + " " + playbackVideoFilename);
        if (result_video_file_request != 0) {
            Log.w(TAG, "request video file failed!    re_request_video_file");
            //requestPlayVideo(playbackVideoFilename);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////   链接直播视频源   ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private VideoRenderer renderer;
    private GlnkPlayer player;
    private GlnkDataSource source;
    private Glnk180DataSourceListenerImpl dataSourceListener;

    public void open_connect(String gid, String username, String password,
                             int channelNo,int streamType,int dataType){
        if( renderer!=null){
            renderer.release();
            renderer = null;
        }

        renderer = new AViewRenderer(LangTao180Activity.this, null);
        ((AViewRenderer) renderer).setCallBackDataType(AViewRenderer.CB_DATA_TYPE_YUV);
        ((AViewRenderer) renderer).setValidateYUVCallback(new AViewRenderer.ValidateYUVCallback() {
            @Override
            public void yuv_Callback(int width, int height, byte[] byYdata, int nYLen, byte[] byUdata, int nULen, byte[] byVdata, int nVLen) {
                if( mLT180RenderMgr != null ){
                    mLT180RenderMgr.addBuffer(width,height,byYdata,byUdata,byVdata);
                }
                if(requestDumpYuv){
                    new DumpYUVFrameFile(width,height,byYdata,byUdata,byVdata,"dump_real.yuv").start();
                }
            }
        });

        if( source!=null){
            source.stop();
            source.release();
            source = null;
        }
        if(dataSourceListener ==null)
            dataSourceListener = new Glnk180DataSourceListenerImpl();
        source = new GlnkDataSource(GlnkClient.getInstance());
        source.setGlnkDataSourceListener(dataSourceListener);
        source.setMetaData(gid, username, password, channelNo, streamType, dataType);

        if(player!=null){
            player.stop();
            player.release();
            player = null;
        }
        player = new GlnkPlayer();
        player.prepare();
        player.setDataSource(source);
        player.setDisplay(renderer);
        player.start();
    }

    public void close_connect() {
        if(player!=null){
            player.stop();
            //里面已经会把source也stop
            player.release();
            player = null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private volatile boolean requestDumpYuv = false;

    public void clickDumpYuv(@SuppressLint("USELESS") View view) {
        requestDumpYuv = true;
    }

    private volatile String PanoramaScreenshot_path = Environment.getExternalStorageDirectory().getPath();

    private class DumpYUVFrameFile extends Thread{
        private final YUVFrame frame;
        private final String fileName;
        public DumpYUVFrameFile(YUVFrame frame,String fileName){
            this.frame = frame;
            this.fileName = fileName;
            requestDumpYuv = false;
        }
        public DumpYUVFrameFile(int width, int height,
                                byte[] byYdata, byte[] byUdata, byte[] byVdata,
                                String fileName){
            YUVFrame yuvFrame = new YUVFrame();
            yuvFrame.setYDataBuffer(byYdata);
            yuvFrame.setUDataBuffer(byUdata);
            yuvFrame.setVDataBuffer(byVdata);
            yuvFrame.setWidth(width);
            yuvFrame.setHeight(height);
            this.frame = yuvFrame;
            this.fileName = fileName;
            requestDumpYuv = false;
        }

        @Override
        public void run() {
            super.run();
            String filename = PanoramaScreenshot_path+"/"+fileName;
            Log.d(TAG, "dump yuv file : "+filename);
            try {
                File file = new File(filename);
                if(file.exists()) {
                    boolean delete = file.delete();
                    if(delete) {
                        FileOutputStream fos = new FileOutputStream(file,false);
                        fos.write(frame.getYuvbyte());
                        fos.close();
                    }
                } else {
                    FileOutputStream fos = new FileOutputStream(file,false);
                    fos.write(frame.getYuvbyte());
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
