package com.langtao.reborn.pack180;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.langtao.device.SDKinitUtil;
import com.langtao.ltpanorama.LangTao180RenderMgr;
import com.langtao.reborn.R;

import glnk.client.GlnkClient;
import glnk.media.AViewRenderer;
import glnk.media.AliOSSDataSource;
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
    private Handler handler= new Handler();
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
    }

    private void initGLSurfaceView() {
        gl_view_container.removeAllViews();
        if( SDKinitUtil.checkGLEnvironment() ){
            if(mLT180RenderMgr==null)
                mLT180RenderMgr = new LangTao180RenderMgr();
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

        renderer = new AViewRenderer(LangTao180Activity.this, null);
        ((AViewRenderer) renderer).setCallBackDataType(AViewRenderer.CB_DATA_TYPE_YUV);
        ((AViewRenderer) renderer).setValidateYUVCallback(new AViewRenderer.ValidateYUVCallback() {
            @Override
            public void yuv_Callback(int width, int height, byte[] byYdata, int nYLen, byte[] byUdata, int nULen, byte[] byVdata, int nVLen) {
                //Log.d(TAG, "Note: yuv_Callback !!! ");
                if( mLT180RenderMgr != null ){
                    //Log.i(TAG, "mLT180RenderMgr  add_buffer !!!");
                    mLT180RenderMgr.addBuffer(width,height,byYdata,byUdata,byVdata);
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
}
