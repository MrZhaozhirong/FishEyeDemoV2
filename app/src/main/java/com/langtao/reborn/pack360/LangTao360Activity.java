package com.langtao.reborn.pack360;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.langtao.device.SDKinitUtil;
import com.langtao.ltpanorama.LangTao360RenderMgr;
import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.reborn.R;
import com.langtao.reborn.pack180.Glnk180DataSourceListenerImpl;

import java.io.File;
import java.io.FileOutputStream;

import glnk.client.GlnkClient;
import glnk.media.AViewRenderer;
import glnk.media.AliOSSDataSource;
import glnk.media.GlnkDataSource;
import glnk.media.GlnkPlayer;
import glnk.media.VideoRenderer;

/**
 * Created by zzr on 2017/12/8.
 */

public class LangTao360Activity extends Activity {

    public static final String TAG = "LangTao360Activity";
    private RelativeLayout gl_view_container;
    private GLSurfaceView gl_view;
    private Handler handler= new Handler();
    //nStreamType实时流，码流类型， 0-主码流； 1-次码流
    //dataType流数据类型, 0-视频流, 1-音频流, 2-音视频流
    private int channelNo=0, streamType=0, dataType=2;

    private LangTao360RenderMgr mLT360RenderMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_360);

        gl_view_container = (RelativeLayout) findViewById(R.id.gl_view_container);
    }

    private void initGLSurfaceView() {
        gl_view_container.removeAllViews();
        if( SDKinitUtil.checkGLEnvironment() ){
            if(mLT360RenderMgr == null)
                mLT360RenderMgr = new LangTao360RenderMgr();
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_360);
            mLT360RenderMgr.setAutoCruise(true);
            gl_view = new GLSurfaceView(LangTao360Activity.this);
            gl_view.setEGLContextClientVersion(2);
            gl_view.setRenderer(mLT360RenderMgr);
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
        if(gl_view!=null){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gl_view.onResume();
                }
            },2000);
        }else {
            //延时初始化，有效降低初始化时候主线程卡住导致无响应的问题。
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initGLSurfaceView();
                }
            },5000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, TAG+" onPause");
        if(gl_view!=null) gl_view.onPause();

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
    }

    public void clickFishEye360(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_360);
        }
    }
    public void clickFour360(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_FOUR_EYE);
        }
    }
    public void clickTwoRectangle(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_TWO_RECTANGLE);
        }
    }
    public void clickCylinder(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_CYLINDER);
        }
    }
    public void clickLeftCruise(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setCruiseDirection(LTRenderMode.CRUISE_LEFT);
        }
    }
    public void clickRightCruise(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setCruiseDirection(LTRenderMode.CRUISE_RIGHT);
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
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
                            mLT360RenderMgr.handleTouchDown(x,y);
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
                            mLT360RenderMgr.handleDoubleClick();
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
                            mLT360RenderMgr.handleTouchMove(x,y);
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
                                mLT360RenderMgr.handleMultiTouch(distance);
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
                        mLT360RenderMgr.handleTouchUp(x, y, xVelocity, yVelocity);
                    }
                });
            }

            return true;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////   链接直播视频源   ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private VideoRenderer renderer;
    private GlnkPlayer player;
    private GlnkDataSource source;
    private AliOSSDataSource alisource; //云存同理，都是在yuv_callback回调 往mPanoramaRenderMgr里面addBuffer
    private Glnk180DataSourceListenerImpl dataSourceListener;

    public void open_connect(String gid, String username, String password,
                             int channelNo,int streamType,int dataType){
        if(renderer!=null){
            renderer.release();
            renderer = null;
        }

        renderer = new AViewRenderer(LangTao360Activity.this, null);
        ((AViewRenderer) renderer).setCallBackDataType(AViewRenderer.CB_DATA_TYPE_YUV);
        ((AViewRenderer) renderer).setValidateYUVCallback(new AViewRenderer.ValidateYUVCallback() {
            @Override
            public void yuv_Callback(int width, int height, byte[] byYdata, int nYLen, byte[] byUdata, int nULen, byte[] byVdata, int nVLen) {
                //Log.d(TAG, "Note: yuv_Callback !!! ");
                if( mLT360RenderMgr != null ){
                    //Log.i(TAG, "mLT360RenderMgr  add_buffer !!!");
                    mLT360RenderMgr.addBuffer(width,height,byYdata,byUdata,byVdata);
                }
                if(requestDumpYuv){
                    new DumpYUVFrameFile(width,height,byYdata,byUdata,byVdata,"dump_video.yuv").start();
                }
            }
        });

        if(source!=null){
            source.stop();
            source.release();
            source = null;
        }
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
