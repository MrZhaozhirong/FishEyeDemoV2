package com.langtao.reborn.pack720;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.langtao.device.DevCapability;
import com.langtao.device.DeviceStatusManager;
import com.langtao.device.SDKinitUtil;
import com.langtao.ltpanorama.LTRenderManager;
import com.langtao.ltpanorama.LangTao720RenderMgr;
import com.langtao.ltpanorama.shape.PanoTemplateRectangleFBO;
import com.langtao.protocol.Command;
import com.langtao.protocol.OWSP_StreamType;
import com.langtao.protocol._TLV_V_VideoModeRequest;
import com.langtao.reborn.R;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.khronos.opengles.GL10;

import glnk.client.GlnkChannel;
import glnk.client.GlnkClient;
import glnk.media.AViewRenderer;
import glnk.media.AliOSSDataSource;
import glnk.media.GlnkDataSource;
import glnk.media.GlnkPlayer;
import glnk.media.VideoRenderer;

/**
 * Created by zzr on 2017/12/6.
 */
public class LangTao720Activity extends Activity {
    public static final String TAG = "LangTao720Activity";
    private TextView logView;
    private LT720Handler handler;
    //nStreamType实时流，码流类型， 0-主码流； 1-次码流
    //dataType流数据类型, 0-视频流, 1-音频流, 2-音视频流
    private int channelNo=0, streamType=0, dataType=2;

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
                        StringBuilder sb = new StringBuilder();
                        sb.append(devID+" status:"+deviceStatusObject.getDevStatus());
                        refreshLogView(sb.toString()+"\n");
                        //refreshLogView(devFunInfo.toString()+"\n");
                    }
                }
            }

        }
    }
    DeviceStatusReceiver deviceStatusReceiver = new DeviceStatusReceiver();

    private RelativeLayout gl_view_container;
    private GLSurfaceView gl_view;
    private LTRenderManager mLT720RenderMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, TAG+" onCreate");
        setContentView(R.layout.activity_720);
        logView=(TextView)findViewById(R.id.logView2);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logView.setTextColor(0x50000000);

        handler = new LT720Handler(LangTao720Activity.this);
        mLT720RenderMgr = new LangTao720RenderMgr();

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceStatusManager.DSM_ON_CHANGED_CALL);
        filter.addAction(DeviceStatusManager.DSM_ON_PUSH_SVRINFO_CALL);
        filter.addAction(DeviceStatusManager.DSM_ON_DEV_FUN_INFO_CALL);
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceStatusReceiver,filter);

        gl_view_container = (RelativeLayout) findViewById(R.id.gl_view_container);

        final ImageView someImageView = (ImageView) this.findViewById(R.id.someImageView);
        this.findViewById(R.id.capture_screen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureBitmap(new BitmapReadyCallbacks() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        someImageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(TAG, TAG+" onResume");
        //onPause时发了一个延时的CLOSE_CONNECT命令，去除掉
        handler.removeMessages(handler.CLOSE_CONNECT);
        if(gl_view!=null){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gl_view.onResume();
                }
            }, 5000);
        }else {
            //延时初始化，有效降低初始化时候主线程卡住导致无响应的问题。
            handler.sendEmptyMessageDelayed(handler.DELAY_INIT_GL, 5000);
        }
    }

    public void initGLSurfaceView() {
        gl_view_container.removeAllViews();
        if( SDKinitUtil.checkGLEnvironment() ){
            if(mLT720RenderMgr==null)
                mLT720RenderMgr = new LangTao720RenderMgr();
            gl_view = new GLSurfaceView(LangTao720Activity.this);
            gl_view.setEGLContextClientVersion(2);
            //glView.setPreserveEGLContextOnPause(true);
            gl_view.setRenderer(mLT720RenderMgr);
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
    protected void onPause() {
        super.onPause();
        Log.w(TAG, TAG+" onPause");
        if(gl_view!=null) gl_view.onPause();
        //黄守华 说 放后台的时候，不需要立即停止，可以延时时间才关闭。
        handler.sendEmptyMessageDelayed(handler.CLOSE_CONNECT, 10000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, TAG+" onDestroy");
        disconnectDevice();
        close_connect();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceStatusReceiver);
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
        if(mLT720RenderMgr != null) {
            //要先下载配置文件。
            if(mLT720RenderMgr instanceof LangTao720RenderMgr){
                ((LangTao720RenderMgr)mLT720RenderMgr).setPanoTemConfigFile(getPanoramaConfigFilePath(false));
                ((LangTao720RenderMgr)mLT720RenderMgr).setPanoTemSecretKey(gid.getText().toString());
            }
        }
    }

    public void downloadPanoramaConfig(@SuppressLint("USELESS") View view) {
        connectToDevice();
    }

    public void clickNextMode(@SuppressLint("USELESS") View view) {
        if(mLT720RenderMgr!=null){
            if(mLT720RenderMgr instanceof LangTao720RenderMgr)
                ((LangTao720RenderMgr)mLT720RenderMgr).nextModelShape();
        }
    }

    public void refreshLogView(String msg){
        logView.append(msg);
        int offset=logView.getLineCount()*logView.getLineHeight();
        if(offset>logView.getHeight()){
            logView.scrollTo(0,offset-logView.getHeight());
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
                            mLT720RenderMgr.handleTouchDown(x,y);
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
                    Log.w(TAG, "SurfaceView-GL double click in thread."+Thread.currentThread().getName());
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
                            mLT720RenderMgr.handleTouchMove(x,y);
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
                                mLT720RenderMgr.handleMultiTouch(distance);
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
                        mLT720RenderMgr.handleTouchUp(x, y, xVelocity, yVelocity);
                    }
                });
            }

            return true;
        }
    }


    ///////////////////glSurfaceView截图 start//////////////////////////////
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
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            boolean blackScreen = isBlackScreen(w, h, bitmapBuffer);
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

    private boolean isBlackScreen(int w, int h, int[] bitmapBuffer) {
        //int i1 = bitmapBuffer[0]; 有符号数 -16777216
        //byte byte1 = (byte) (i1 & 0xff);// 最低位 0
        //byte byte2 = (byte) ((i1 >> 8) & 0xff);// 次低位 0
        //byte byte3 = (byte) ((i1 >> 16) & 0xff);// 次高位 0
        //byte byte4 = (byte) (i1 >>> 24);// 最高位 -1
        int w_middle = w/2;
        int h_middle = h/2;
        boolean verticalIsBlack = true;
        for(int i=0; i<h; i++) {
            if(bitmapBuffer[w_middle+i*w] != -16777216){
                verticalIsBlack = false;
            }
        }
        boolean horizontalIsBlack = true;
        for(int i=0; i<w; i++) {
            if(bitmapBuffer[w*h_middle+i] != -16777216){
                horizontalIsBlack = false;
            }
        }
        return (verticalIsBlack && horizontalIsBlack);
        //return verticalIsBlack;
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h, GL10 gl) {

        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);
        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);
            //bitmapSource = intBuffer.array();
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
            Log.e(TAG, "createBitmapFromGLSurface: " + e.getMessage(), e);
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }

    public interface BitmapReadyCallbacks {
        void onBitmapReady(Bitmap bitmap);
    }



    /////////////////// 2:1全景图 start//////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void clickPreviewPicPanorama(@SuppressLint("USELESS") View view) {
        close_connect();
        disconnectDevice();
        if(mLT720RenderMgr!=null) {
            if(mLT720RenderMgr instanceof LangTao720RenderMgr){
                ((LangTao720RenderMgr)mLT720RenderMgr).setPreviewPanoramaPicture(
                        PanoramaScreenshot_path+File.separator+"_LTPanorama.JPG");
                ((LangTao720RenderMgr)mLT720RenderMgr).setPreviewPic();
            }
        }
    }

    public void clickGeneratePicPanorama(@SuppressLint("USELESS") View view) {
        if(mLT720RenderMgr!=null){
            if(mLT720RenderMgr instanceof LangTao720RenderMgr)
            ((LangTao720RenderMgr)mLT720RenderMgr).getPanoramaPicture(
                    new PanoTemplateRectangleFBO.ScreenShotReadyCallback() {
                        @Override
                        public void onScreenShotReady(int x, int y, int width, int height, int format, int type,
                                                      final IntBuffer imageBuf ) {
                            // 回调不要有阻塞的操作，最好是异步的。
                            // 放线程处理，不要阻塞，否则会爆炸。
                            if(width!=0 && height!=0 && imageBuf!=null){
                                new HandlePanoramaScreenshot(width, height, imageBuf).start();
                            }
                        }
                    });
        }
    }

    private volatile String PanoramaScreenshot_path = Environment.getExternalStorageDirectory().getPath();
    private class HandlePanoramaScreenshot extends Thread{
        int w, h;
        IntBuffer imageBuffer;
        public HandlePanoramaScreenshot(int width, int height, IntBuffer imageBuf){
            this.w = width;
            this.h = height;
            this.imageBuffer = imageBuf;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();
            long estimatedTime;
            // Convert upside down mirror-reversed image to right-side up normal image.
            int[] iat = new int[w * h];
            int[] ia = imageBuffer.array();
            for (int i = 0; i < h; i++) {
                //System.arraycopy(ia, i * w, iat, (h - i - 1) * w, w);
                //读取每行像素，逆序放置最后一行的位置
                for(int j=0; j<w; j++) {
                    iat[(h-i)*w-1-j] = ia[i*w+j];
                }
            }
            estimatedTime = System.nanoTime() - startTime;
            Log.w(TAG, "currentTime : mirror-reversed data "+ estimatedTime);

            startTime = System.nanoTime();
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));
            estimatedTime = System.nanoTime() - startTime;
            Log.w(TAG, "currentTime : copyPixelsFromBuffer "+estimatedTime);

            startTime = System.nanoTime();
            File f = new File(PanoramaScreenshot_path, "_LTPanorama.JPG");
            if (f.exists()) {
                f.delete();
            }
            try {
                FileOutputStream out = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                estimatedTime = System.nanoTime() - startTime;
                Log.w(TAG, "currentTime : compress bitmap time "+estimatedTime);
                Message message = handler.obtainMessage(handler.LOG_MSG);
                message.obj = "2:1全景图生成成功 !!!"+"\n"+"存放路径："+PanoramaScreenshot_path+"/"+w+"x"+h+"_LTPanorama.JPG";
                handler.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                bitmap.recycle();
                System.gc();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////下载全景模板配置文件///////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private GlnkChannel vodChannel;
    private GlnkVodSearchDataSourceImpl vodSource;
    private void connectToDevice() {
        EditText gid = (EditText) findViewById(R.id.gid);
        EditText account = (EditText) findViewById(R.id.account);
        EditText password = (EditText) findViewById(R.id.password);
        //开始下一次搜索之前，得关闭了上一次搜索,原因是设备可能不同
        if (vodChannel != null) {
            vodChannel.stop();
            vodChannel.release();
        }

        vodSource = new GlnkVodSearchDataSourceImpl(handler,LangTao720Activity.this);
        vodChannel = new GlnkChannel(vodSource);
        vodChannel.setMetaData(
                gid.getText().toString(), account.getText().toString(), password.getText().toString(),
                channelNo, streamType, dataType);
        vodChannel.start();
    }

    public void PanoFileDownload(String fileName) {
        if(vodChannel != null) {
            vodChannel.PanoFileDownload(fileName);
        }
    }
    public void disconnectDevice() {
        if (vodChannel != null) {
            vodChannel.stop();
            vodChannel.release();
            vodChannel = null;
        }
    }

    public String getPanoramaConfigFilePath(boolean deleteIfExists) {
        File filesDir = this.getFilesDir();
        Log.w(TAG, "getPanoramaConfigFilePath : "+filesDir.getAbsolutePath()+"/pano.tem");
        String s = filesDir.getAbsolutePath() + "/pano.tem";
        if(deleteIfExists) {
            File configFile = new File(s);
            if(configFile.exists()){
                if(configFile.delete()){
                    //delete需要要cpu周期的，不能忽略
                    return s;
                }else{
                    return null;
                }
            }else{
                return s;
            }
        }else {
            return s;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////   链接直播视频源   ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private VideoRenderer renderer;
    private GlnkPlayer player;
    private GlnkDataSource source;
    private AliOSSDataSource alisource; //云存同理，都是在yuv_callback回调 往mLT720RenderMgr里面addBuffer
    private GlnkDataSourceListenerImpl dataSourceListener;

    public void open_connect(String gid, String username, String password,
                             int channelNo,int streamType,int dataType){
        if( renderer!= null) {
            renderer.release();
            renderer = null;
        }

        renderer = new AViewRenderer(LangTao720Activity.this, null);
        ((AViewRenderer) renderer).setCallBackDataType(AViewRenderer.CB_DATA_TYPE_YUV);
        ((AViewRenderer) renderer).setValidateYUVCallback(new AViewRenderer.ValidateYUVCallback() {
            @Override
            public void yuv_Callback(int width, int height, byte[] byYdata, int nYLen, byte[] byUdata, int nULen, byte[] byVdata, int nVLen) {
                //Log.d(TAG, "Note: yuv_Callback !!! ");
                if( mLT720RenderMgr != null ){
                    //Log.i(TAG, "mLT720RenderMgr  add_buffer !!!");
                    mLT720RenderMgr.addBuffer(width,height,byYdata,byUdata,byVdata);
                }
            }
        });

        if(source!=null){
            source.stop();
            source.release();
            source = null;
        }
        dataSourceListener = new GlnkDataSourceListenerImpl(handler);
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
        if( mLT720RenderMgr!=null ){
            mLT720RenderMgr.clearBuffer();
        }
    }


    public void clickChangeRate(@SuppressLint("USELESS") View view) {
        if(source==null) {
            Message message = handler.obtainMessage(handler.LOG_MSG);
            message.obj = "Note：请先连接视频 !!!";
            handler.sendMessage(message);
            return ;
        }
        _TLV_V_VideoModeRequest switchStream = new _TLV_V_VideoModeRequest();
        streamType = streamType == OWSP_StreamType.OWSP_VIEWMODE_HD
                ? OWSP_StreamType.OWSP_VIEWMODE_SD
                : OWSP_StreamType.OWSP_VIEWMODE_HD;
        switchStream.mode = streamType;

        int res = source.getGlnkChannel()
                .sendData(Command.TLV_T_VIDEOMODE_REQ, switchStream.serialize());
        if(res == 0){
            Message message = handler.obtainMessage(handler.LOG_MSG);
            message.obj = "Note：请求切换码率 streamType "+streamType;
            handler.sendMessage(message);
        }
    }
}
