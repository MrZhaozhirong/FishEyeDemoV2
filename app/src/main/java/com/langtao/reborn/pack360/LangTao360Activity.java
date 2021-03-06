package com.langtao.reborn.pack360;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.langtao.device.GlnkApplication;
import com.langtao.device.SDKinitUtil;
import com.langtao.ltpanorama.LangTao360RenderMgr;
import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.FishEye360Desktop;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.reborn.R;
import com.langtao.reborn.pack180.Glnk180DataSourceListenerImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

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

    private EditText overtureEt, UpperEt, LowerEt, WathPositionEt, ForwardPositionEt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_360);

        gl_view_container = (RelativeLayout) findViewById(R.id.gl_view_container);
        overtureEt = (EditText) findViewById(R.id.overture);
        WathPositionEt = (EditText) findViewById(R.id.watch_position_z);
        ForwardPositionEt = (EditText) findViewById(R.id.watch_position_y);
        UpperEt = (EditText) findViewById(R.id.upper_vision);
        LowerEt = (EditText) findViewById(R.id.lower_vision);
    }

    private void initGLSurfaceView() {
        gl_view_container.removeAllViews();
        if( SDKinitUtil.checkGLEnvironment() ){
            if( mLT360RenderMgr == null)
                mLT360RenderMgr = new LangTao360RenderMgr();
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_DESKTOP);
            SharedPreferences prefs = getSharedPreferences("FISH_EYE", Context.MODE_PRIVATE);
            float rotationX = prefs.getFloat("rotationX", 0.0f);
            float rotationY = prefs.getFloat("rotationY", 0.0f);
            float[] point = {rotationX, rotationY};
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_DESKTOP);
            mLT360RenderMgr.setRotationPoint(point);
            float desktop_scale = prefs.getFloat("desktop_scale", 0.0f);
            mLT360RenderMgr.setCurrentScale(desktop_scale);
            //mLT360RenderMgr.setAutoCruise(true);

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
        if( gl_view!=null){
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

    private boolean getToolsData() {
        FishEye360Desktop desktop = mLT360RenderMgr.getDesktopModel();
        if(desktop == null) return false;
        overtureEt.setText(String.valueOf(desktop.getOverture()));
        WathPositionEt.setText(String.valueOf(desktop.getWatchPosition()));
        ForwardPositionEt.setText(String.valueOf(desktop.getForwardPosition()));
        UpperEt.setText(String.valueOf(desktop.getUpperVisionValue()));
        LowerEt.setText(String.valueOf(desktop.getLowerVisionValue()));
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, TAG+" onPause");
        if(gl_view!=null) gl_view.onPause();

        close_connect();

        if (mLT360RenderMgr!=null) {
            float[] currentRotation = mLT360RenderMgr.getRotationPoint();
            float currentScale = mLT360RenderMgr.getCurrentScale();
            if(currentRotation==null) return;
            SharedPreferences prefs = getSharedPreferences("FISH_EYE", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putFloat("rotationX", currentRotation[0]);
            edit.putFloat("rotationY", currentRotation[1]);
            edit.putFloat("desktop_scale", currentScale);
            edit.apply();
        }
    }


    //Note 谨慎调整，精确度0.01
    public void clickSetOverture(@SuppressLint("USELESS") View view) {
        FishEye360Desktop desktop = mLT360RenderMgr.getDesktopModel();
        if(desktop == null) return;
        String s = overtureEt.getText().toString();
        desktop.setOverture(Double.valueOf(s));
    }
    //Note 精确度0.1
    public void clickSetWatchPosition(@SuppressLint("USELESS") View view) {
        FishEye360Desktop desktop = mLT360RenderMgr.getDesktopModel();
        if(desktop == null) return;
        String s = WathPositionEt.getText().toString();
        desktop.setWatchPosition(Float.valueOf(s));
    }
    public void clickSetForwardPosition(@SuppressLint("USELESS") View view) {
        FishEye360Desktop desktop = mLT360RenderMgr.getDesktopModel();
        if(desktop == null) return;
        String s = ForwardPositionEt.getText().toString();
        desktop.setForwardPosition(Float.valueOf(s));
    }
    //Note 调整upper和lower的时候，请结合logcat的TAG = OneFishEye360的输出日志
    //Note 调整upper和lower的时候，请结合logcat的TAG = OneFishEye360的输出日志
    //Note 调整upper和lower的时候，请结合logcat的TAG = OneFishEye360的输出日志
    public void clickSetUpperVision(@SuppressLint("USELESS") View view) {
        FishEye360Desktop desktop = mLT360RenderMgr.getDesktopModel();
        if(desktop == null) return;
        String s = UpperEt.getText().toString();
        desktop.setUpperVisionValue(Float.valueOf(s));
    }
    public void clickSetLowerVision(@SuppressLint("USELESS") View view) {
        FishEye360Desktop desktop = mLT360RenderMgr.getDesktopModel();
        if(desktop == null) return;
        String s = LowerEt.getText().toString();
        desktop.setLowerVisionValue(Float.valueOf(s));
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


    public void clickFishEyeDesktop(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            SharedPreferences prefs = getSharedPreferences("FISH_EYE", Context.MODE_PRIVATE);
            float rotationX = prefs.getFloat("rotationX", 0.0f);
            float rotationY = prefs.getFloat("rotationY", 0.0f);
            float[] point = {rotationX, rotationY};
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_DESKTOP);
            // 先设置 LTRenderMode.RENDER_MODE_DESKTOP 再设置Rotation
            mLT360RenderMgr.setRotationPoint(point);
        }
    }
    public void clickFishEye360Up(@SuppressLint("USELESS") View view) {
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
    public void clickMode180(@SuppressLint("USELESS") View view) {
        if(mLT360RenderMgr!=null){
            mLT360RenderMgr.setRenderMode(LTRenderMode.RENDER_MODE_180);
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
    public void clickCaptureFrame(@SuppressLint("USELESS") View view){
        if(renderer!=null && renderer instanceof AViewRenderer){
            ((AViewRenderer) renderer).setYuvRequestRGBFrame(
                    new AViewRenderer.ValidateRGBCallback() {
                        @Override
                        public void rgb_Callback(byte[] bytes, int length,int width,int height) {
                            new CaptureFrameFile("frame.jpg",bytes,length,width,height).start();
                        }
                    });
        }
    }

    public void clickPreviewPicFishEye(@SuppressLint("USELESS") View view) {
        close_connect();
        if (mLT360RenderMgr!=null) {
            mLT360RenderMgr.setPreviewFishEyePicture(PanoramaScreenshot_path+File.separator+"frame.jpg");
        }
    }

    public void clickOverScreen(@SuppressLint("USELESS") View view) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void clickCaptureScreen(@SuppressLint("USELESS") View view) {
        final ImageView someImageView = (ImageView) this.findViewById(R.id.someImageView);
        //captureBitmap(new BitmapReadyCallbacks() {
        //    @Override
        //    public void onBitmapReady(Bitmap bitmap) {
        //        someImageView.setImageBitmap(bitmap);
        //    }
        //});
        // 2019.10.17为解决华为等一些机型利用glReadPixel生成屏幕缩略图出现黑屏没数据的情况
        // 利用fbo重draw一遍再glReadPixel
        mLT360RenderMgr.requestCaptureScreen(0, 0, gl_view.getWidth(), gl_view.getHeight(),
                new LangTao360RenderMgr.CaptureScreenCallbacks() {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(this.getResources().getConfiguration().orientation ==Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.first).setVisibility(View.GONE);
            findViewById(R.id.second).setVisibility(View.GONE);
            findViewById(R.id.third).setVisibility(View.GONE);

            ViewGroup.LayoutParams layoutParams = gl_view_container.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            gl_view_container.setLayoutParams(layoutParams);
        } else if(this.getResources().getConfiguration().orientation ==Configuration.ORIENTATION_PORTRAIT) {
            findViewById(R.id.first).setVisibility(View.VISIBLE);
            findViewById(R.id.second).setVisibility(View.VISIBLE);
            findViewById(R.id.third).setVisibility(View.VISIBLE);

            ViewGroup.LayoutParams layoutParams = gl_view_container.getLayoutParams();
            layoutParams.width = (int) GlnkApplication.dip2px(350f);
            layoutParams.height = (int) GlnkApplication.dip2px(350f);
            gl_view_container.setLayoutParams(layoutParams);
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

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
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
        if (renderer!=null){
            renderer.release();
            renderer = null;
        }

        renderer = new AViewRenderer(LangTao360Activity.this, null);
        ((AViewRenderer) renderer).setCallBackDataType(AViewRenderer.CB_DATA_TYPE_YUV);
        ((AViewRenderer) renderer).setValidateYUVCallback(new AViewRenderer.ValidateYUVCallback() {
            @Override
            public void yuv_Callback(int width, int height, byte[] byYdata, int nYLen, byte[] byUdata, int nULen, byte[] byVdata, int nVLen) {
                //Log.d(TAG, "Note: yuv_Callback !!! ");
                if (mLT360RenderMgr != null ){
                    //Log.i(TAG, "mLT360RenderMgr  add_buffer !!!");
                    mLT360RenderMgr.addBuffer(width,height,byYdata,byUdata,byVdata);
                }
                if (requestDumpYuv){
                    new DumpYUVFrameFile(width,height,byYdata,byUdata,byVdata,"dump_video.yuv").start();
                }
            }
        });

        if (source!=null){
            source.stop();
            source.release();
            source = null;
        }
        dataSourceListener = new Glnk180DataSourceListenerImpl();
        source = new GlnkDataSource(GlnkClient.getInstance());
        source.setGlnkDataSourceListener(dataSourceListener);
        source.setMetaData(gid, username, password, channelNo, streamType, dataType);

        if (player!=null){
            player.stop();
            player.release();
            player = null;
        }
        player = new GlnkPlayer();
        player.prepare();
        player.setDataSource(source);
        player.setDisplay(renderer);
        player.start();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getToolsData();
            }
        },  5_000);
    }

    public void close_connect() {
        if( player!=null){
            player.stop();
            //里面已经会把source也stop
            player.release();
            player = null;
        }
    }

    ///////////////////glSurfaceView截图 start//////////////////////////////
    public interface BitmapReadyCallbacks {
        void onBitmapReady(Bitmap bitmap);
    }

    private void captureBitmap(final BitmapReadyCallbacks bitmapReadyCallbacks){
        if(gl_view==null)return;
        gl_view.queueEvent(new Runnable() {
            @Override
            public void run() {
                //EGL10 egl = (EGL10) EGLContext.getEGL();
                //GL10 gl = (GL10)egl.eglGetCurrentContext().getGL();
                //final Bitmap snapshotBitmap = createBitmapFromGLSurface(0, 0,
                //        glSurfaceView.getWidth(), glSurfaceView.getHeight(), gl);
                final Bitmap snapshotBitmap = createBitmapFromGLSurface2(0, 0,
                        gl_view.getWidth(), gl_view.getHeight(), null);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bitmapReadyCallbacks.onBitmapReady(snapshotBitmap);
                    }
                });
            }
        });
    }

    private Bitmap createBitmapFromGLSurface2(int x, int y, int w, int h, GL10 gl){
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);
        try {
            //int type_params[] = new int[10];
            //int format_params[] = new int[10];
            //GLES20.glGetIntegerv(GLES20.GL_IMPLEMENTATION_COLOR_READ_TYPE, type_params, 0);
            //GLES20.glGetIntegerv(GLES20.GL_IMPLEMENTATION_COLOR_READ_FORMAT, format_params, 0);
            //for(int i=0;i<10;i++) {
            //    Log.i(TAG, "GL_IMPLEMENTATION_COLOR_READ_TYPE : "+type_params[i]);
            //    Log.i(TAG, "GL_IMPLEMENTATION_COLOR_READ_FORMAT : "+format_params[i]);
            //}
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            boolean blackScreen = isBlackOrTransparent(w, h, bitmapBuffer);
            if(blackScreen) {
                Log.w(TAG, "createBitmapFromGLSurface2 is Black !!!");
                return null;
            }
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            Log.e(TAG, "createBitmapFromGLSurface2 : " + e.getMessage(), e);
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    private boolean isBlackOrTransparent(int w, int h, int[] bitmapBuffer) {
        //int i1 = bitmapBuffer[0]; 有符号数 -16777216
        //byte byte1 = (byte) (i1 & 0xff);// 最低位 0
        //byte byte2 = (byte) ((i1 >> 8) & 0xff);// 次低位 0
        //byte byte3 = (byte) ((i1 >> 16) & 0xff);// 次高位 0
        //byte byte4 = (byte) (i1 >>> 24);// 最高位 -1
        int w_middle = w/2;
        int h_middle = h/2;
        int w_third = w/3;
        int h_third = h/3;
        // 只抽取屏幕中间部分(横纵向1/3~2/3)的中间竖线取值
        boolean vertical = true;
        for(int i=h_third; i<h_third*2; i++) {
            vertical = bitmapBuffer[i * w + w_middle] == -16777216;
            if(!vertical)
                vertical = bitmapBuffer[i * w + w_middle] == 0;
        }
        //boolean horizontal = true;
        //for(int i=w_third; i<w_third*2; i++) {
        //    horizontal = bitmapBuffer[w * h_middle + i] == -16777216;
        //    if(!horizontal)
        //        horizontal = bitmapBuffer[w * h_middle + i] != 0;
        //}
        //return (vertical && horizontal);
        return vertical;
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

    private class CaptureFrameFile extends Thread {
        private final String fileName;
        private final byte[] bytes;
        private final int mRgbLen;
        private final int mWidth;
        private final int mHeight;
        CaptureFrameFile(String fileName,byte[] bytes, int len, int width,int height){
            this.fileName = fileName;
            this.mRgbLen = len;
            this.bytes = bytes;
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        public void run() {
            super.run();
            String filename = PanoramaScreenshot_path+"/"+fileName;
            Log.d(TAG, "capture frame file : "+filename);
            try {
                Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(buffer);
                File file = new File(filename);
                if(file.exists()) {
                    if(file.delete()) {
                        saveBitmapToPath(bitmap, filename);
                    }
                }else{
                    saveBitmapToPath(bitmap, filename);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void saveBitmapToPath(Bitmap mBitmap, String path_name)  {
            File f = new File(path_name);
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            try {
                fOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
