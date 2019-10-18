package com.langtao.ltpanorama;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.FrameBuffer;
import com.langtao.ltpanorama.shape.Cylinder;
import com.langtao.ltpanorama.shape.FishEye180;
import com.langtao.ltpanorama.shape.FishEye360;
import com.langtao.ltpanorama.shape.FishEye360Desktop;
import com.langtao.ltpanorama.shape.FourEye360;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.ltpanorama.shape.TwoRectangle;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zzr on 2017/12/8.
 */

public class LangTao360RenderMgr extends LTRenderManager   {
    private static final String TAG = LTRenderManager.TAG+"-360";
    private static final String PIC = "PIC";
    private static final String VIDEO = "VIDEO";

    public LangTao360RenderMgr() {
        super();
        RENDER_MODE = LTRenderMode.RENDER_MODE_360;
    }

    //private static volatile byte[] previewPicRawData;
    private static volatile int[] previewPicRawData;
    private String previewPicPathName;
    private volatile String PIC_OR_VIDEO = VIDEO; //default
    public void setPreviewFishEyePicture(final String previewPicPathName) {
        this.previewPicPathName = previewPicPathName;
        //注意bitmap位图读取byte[]的方式，1个int转4个byte，还有java基础变量都是有符号数
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = false;   //指定需要的是原始数据，非压缩数据
                    Bitmap bitmap = BitmapFactory.decodeFile(previewPicPathName, options);
                    if(bitmap == null){
                        throw new IllegalStateException("previewPicPathName not load in bitmap!");
                    }
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels,0,width, 0,0,width,height);
                    previewPicRawData = pixels;
                    PIC_OR_VIDEO = PIC;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private FishEye360Desktop desktop;
    private FishEye360 bowl;
    private FourEye360 fourEye;
    private TwoRectangle rectangle;
    private Cylinder cylinder;
    //180
    private FishEye180 curvedPlate;




    private float[] desktop_position_point = new float[2];
    public void setRotationPoint(float[] point) {
        desktop_position_point[0] = point[0];
        desktop_position_point[1] = point[1];
        if(desktop!=null ) {
            desktop.setRotationPoint(point);
        }
    }
    public float[] getRotationPoint() {
        if(desktop!=null ) {
            return desktop.getRotationPoint();
        }
        return null;
    }


    private float desktop_room_scale = 0;
    public void setCurrentScale(float scale) {
        this.desktop_room_scale = scale;
        if(desktop!=null ) {
            desktop.setScale(desktop_room_scale);
        }
    }
    public float getCurrentScale() {
        if(desktop!=null ) {
            return desktop.getCurrentScale();
        }
        return desktop_room_scale;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao360RenderMgr onSurfaceCreated");
        desktop = new FishEye360Desktop();
        desktop.setRotationPoint(desktop_position_point);
        desktop.setScale(desktop_room_scale);

        bowl = new FishEye360();

        fourEye = new FourEye360();
        rectangle = new TwoRectangle();
        cylinder = new Cylinder();

        curvedPlate = new FishEye180();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w(TAG, "LangTao360RenderMgr onSurfaceChanged");
        Log.w(TAG, "Render width X height : "+width+" x "+height);
        bowl.onSurfaceChange(width, height);
        desktop.onSurfaceChange(width, height);
        fourEye.onSurfaceChange(width, height);
        rectangle.onSurfaceChange(width, height);

        cylinder.onSurfaceChange(width, height);

        curvedPlate.onSurfaceChange(width, height);
    }

    private int x, y;
    private int w, h;
    private boolean capture = false;
    private FrameBuffer fbo;
    private CaptureScreenCallbacks callback;
    public interface CaptureScreenCallbacks {
        void onCaptureScreenReady(int w,int h,int[] data);
    }
    //2019.10.17为解决华为等一些机型利用glReadPixel生成屏幕缩略图出现黑屏没数据的情况
    // 利用fbo重draw一遍再glReadPixel
    public void requestCaptureScreen(int x,int y, int w,int h,
                                     CaptureScreenCallbacks callback){
        if( fbo==null) {
            fbo = new FrameBuffer();
        } else {
            if(this.w!=w || this.h!=h) {
                fbo.reSize(w,h);
            }
        }
        this.x = x;this.y = y;
        this.w = w;this.h = h;
        capture = true;
        this.callback = callback;
    }
    private void captureScreen() {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);
        try {
            //GLES30.glReadBuffer(GLES20.GL_COLOR_ATTACHMENT0);
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            boolean blackScreen = isBlackOrTransparent(w, h, bitmapBuffer);
            if(blackScreen) {
                Log.w(TAG, "captureScreen is Black !!!");
                return;
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
            Log.e(TAG, "captureScreen : " + e.getMessage(), e);
        } finally {
            if(callback!=null) callback.onCaptureScreenReady(w,h,bitmapSource.clone());
        }
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
    private void drawCaptureScreen(YUVFrame frame) {
        fbo.begin();
        switch (RENDER_MODE) {
            case LTRenderMode.RENDER_MODE_DESKTOP: {
                if (!desktop.isInitialized) {
                    desktop.onSurfaceCreate(frame);
                }
                desktop.onDrawFrame(frame);
            }
            break;
            case LTRenderMode.RENDER_MODE_180: {
                if (!curvedPlate.isInitialized) {
                    curvedPlate.onSurfaceCreate(frame);
                }
                curvedPlate.onDrawFrame(frame);
            }
            break;
            case LTRenderMode.RENDER_MODE_360: {
                if (!bowl.isInitialized) {
                    bowl.onSurfaceCreate(frame);
                }
                bowl.onDrawFrame(frame);
            }
            break;
            case LTRenderMode.RENDER_MODE_FOUR_EYE: {
                if (!fourEye.isInitialized) {
                    fourEye.onSurfaceCreate(frame);
                }
                fourEye.onDrawFrame(frame);
            }
            break;
            case LTRenderMode.RENDER_MODE_TWO_RECTANGLE: {
                if (!rectangle.isInitialized) {
                    rectangle.onSurfaceCreate(frame);
                }
                rectangle.onDrawFrame(frame);
            }
            break;
            case LTRenderMode.RENDER_MODE_CYLINDER: {
                if (!cylinder.isInitialized) {
                    cylinder.onSurfaceCreate(frame);
                }
                cylinder.onDrawFrame(frame);
            }
            break;
            default:
                Log.w(TAG, "LangTao360RenderMgr RenderMode " + RENDER_MODE + " not recognized !!!");
                break;
        }
        captureScreen();
        fbo.end();
    }



    @Override
    public void onDrawFrame(GL10 gl) {
        try{
            if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
                YUVFrame frame = mCircularBuffer.getFrame();
                switch (RENDER_MODE){
                    case LTRenderMode.RENDER_MODE_DESKTOP:{
                        if( !desktop.isInitialized ){
                            desktop.onSurfaceCreate(frame);
                        }
                        desktop.onDrawFrame(frame);
                    }break;
                    case LTRenderMode.RENDER_MODE_180:{
                        if( !curvedPlate.isInitialized ){
                            curvedPlate.onSurfaceCreate(frame);
                        }
                        curvedPlate.onDrawFrame(frame);
                    }break;
                    case LTRenderMode.RENDER_MODE_360:{
                        if( !bowl.isInitialized ){
                            bowl.onSurfaceCreate(frame);
                        }
                        bowl.onDrawFrame(frame);
                    }break;
                    case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                        if( !fourEye.isInitialized ){
                            fourEye.onSurfaceCreate(frame);
                        }
                        fourEye.onDrawFrame(frame);
                    }break;
                    case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                        if( !rectangle.isInitialized ){
                            rectangle.onSurfaceCreate(frame);
                        }
                        rectangle.onDrawFrame(frame);
                    }break;
                    case LTRenderMode.RENDER_MODE_CYLINDER:{
                        if( !cylinder.isInitialized ){
                            cylinder.onSurfaceCreate(frame);
                        }
                        cylinder.onDrawFrame(frame);
                    }break;
                    default:
                        Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                        break;
                }
                if( capture) {
                    drawCaptureScreen(frame);
                    capture = false;
                }
                if(frame!=null) frame.release();
            }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
                switch (RENDER_MODE){
                    case LTRenderMode.RENDER_MODE_DESKTOP:{
                        if( !desktop.isInitialized ){
                            desktop.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        desktop.onDrawPreviewPic();
                    }break;
                    case LTRenderMode.RENDER_MODE_180:{
                        if( !curvedPlate.isInitialized ){
                            curvedPlate.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        curvedPlate.onDrawPreviewPic();
                    }break;
                    case LTRenderMode.RENDER_MODE_360:{
                        if( !bowl.isInitialized ){
                            bowl.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        bowl.onDrawPreviewPic();
                    }break;
                    case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                        if( !fourEye.isInitialized ){
                            fourEye.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        fourEye.onDrawPreviewPic();
                    }break;
                    case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                        if( !rectangle.isInitialized ){
                            rectangle.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        rectangle.onDrawPreviewPic();
                    }break;
                    case LTRenderMode.RENDER_MODE_CYLINDER:{
                        if( !cylinder.isInitialized ){
                            cylinder.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        cylinder.onDrawPreviewPic();
                    }break;
                    default:
                        Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                        break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void handleTouchUp(final float x, final float y, final float xVelocity, final float yVelocity) {
        switch (RENDER_MODE){
            case LTRenderMode.RENDER_MODE_DESKTOP:{
                if(desktop!=null )
                    desktop.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            case LTRenderMode.RENDER_MODE_180:{
                if(curvedPlate!=null)
                    curvedPlate.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            case LTRenderMode.RENDER_MODE_360:{
                if(bowl!=null)
                    bowl.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                if(fourEye!=null)
                    fourEye.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                if(rectangle!=null)
                    rectangle.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            case LTRenderMode.RENDER_MODE_CYLINDER:{
                if(cylinder!=null)
                    cylinder.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            default:
                Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                break;
        }
    }
    @Override
    public void handleTouchDown(float x, float y) {
        switch (RENDER_MODE){
            case LTRenderMode.RENDER_MODE_DESKTOP:{
                if(desktop!=null )
                    desktop.handleTouchDown(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_180:{
                if(curvedPlate!=null)
                    curvedPlate.handleTouchDown(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_360:{
                if(bowl!=null)
                    bowl.handleTouchDown(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                if(fourEye!=null)
                    fourEye.handleTouchDown(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                if(rectangle!=null)
                    rectangle.handleTouchDown(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_CYLINDER:{
                if(cylinder!=null)
                    cylinder.handleTouchDown(x, y);
            }break;
            default:
                Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                break;
        }
    }
    @Override
    public void handleTouchMove(float x, float y) {
        switch (RENDER_MODE){
            case LTRenderMode.RENDER_MODE_DESKTOP:{
                if(desktop!=null )
                    desktop.handleTouchMove(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_180:{
                if(curvedPlate!=null)
                    curvedPlate.handleTouchMove(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_360:{
                if(bowl!=null)
                    bowl.handleTouchMove(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                if(fourEye!=null)
                    fourEye.handleTouchMove(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                if(rectangle!=null)
                    rectangle.handleTouchMove(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_CYLINDER:{
                if(cylinder!=null)
                    cylinder.handleTouchMove(x, y);
            }break;
            default:
                Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                break;
        }
    }
    @Override
    public void handleMultiTouch(float distance) {
        switch (RENDER_MODE){
            case LTRenderMode.RENDER_MODE_DESKTOP:{
                if(desktop!=null )
                    desktop.handleMultiTouch(distance);
            }break;
            case LTRenderMode.RENDER_MODE_180:{
                if(curvedPlate!=null)
                    curvedPlate.handleMultiTouch(distance);
            }break;
            case LTRenderMode.RENDER_MODE_360:{
                if(bowl!=null)
                    bowl.handleMultiTouch(distance);
            }break;
            case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                //if(fourEye!=null)
                //    fourEye.handleMultiTouch(distance);
            }break;
            case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                //if(rectangle!=null)
                //    rectangle.handleMultiTouch(distance);
            }break;
            case LTRenderMode.RENDER_MODE_CYLINDER:{
                if(cylinder!=null)
                    cylinder.handleMultiTouch(distance);
            }break;
            default:
                Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                break;
        }
    }

    @Override
    public void handleDoubleClick() {
        switch (RENDER_MODE){
            case LTRenderMode.RENDER_MODE_DESKTOP:{
                //if(desktop!=null )
                //    desktop.handleDoubleClick();
            }break;
            case LTRenderMode.RENDER_MODE_180:{
                if(curvedPlate!=null)
                    curvedPlate.handleDoubleClick();
            }break;
            case LTRenderMode.RENDER_MODE_360:{
                if(bowl!=null)
                    bowl.handleDoubleClick();
            }break;
            case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                //if(fourEye!=null)
                //    fourEye.handleDoubleClick();
            }break;
            case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                //if(rectangle!=null)
                //    rectangle.handleDoubleClick();
            }break;
            case LTRenderMode.RENDER_MODE_CYLINDER:{
                //if(cylinder!=null)
                //    cylinder.handleDoubleClick();
            }break;
            default:
                Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                break;
        }
    }






    /**
     * 自动巡航设置
     * @param autoCruise
     */
    public void setAutoCruise(boolean autoCruise){
        if (desktop != null)
            desktop.setAutoCruise(autoCruise);
        if (curvedPlate != null)
            curvedPlate.setAutoCruise(autoCruise);
        if (bowl != null)
            bowl.setAutoCruise(autoCruise);
        if (fourEye != null)
            fourEye.setAutoCruise(autoCruise);
        if (cylinder != null)
            cylinder.setAutoCruise(autoCruise);
        if (rectangle != null)
            rectangle.setAutoCruise(autoCruise);
    }

    public void setCruiseDirection(int direction) {
        if (desktop != null)
            desktop.setCruiseDirection(direction);
        if (bowl != null)
            bowl.setCruiseDirection(direction);
        if (fourEye != null)
            fourEye.setCruiseDirection(direction);
        if (cylinder != null)
            cylinder.setCruiseDirection(direction);
        if (rectangle != null)
            rectangle.setCruiseDirection(direction);
    }



}
