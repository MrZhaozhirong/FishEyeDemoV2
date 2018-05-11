package com.langtao.ltpanorama;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.Cylinder;
import com.langtao.ltpanorama.shape.FishEye180;
import com.langtao.ltpanorama.shape.FishEye360;
import com.langtao.ltpanorama.shape.FourEye360;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.ltpanorama.shape.TwoRectangle;

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

    private static volatile byte[] previewPicRawData;
    private String previewPicPathName;
    private volatile String PIC_OR_VIDEO = VIDEO; //default
    public void setPreviewFishEyePicture(final String previewPicPathName) {
        this.previewPicPathName = previewPicPathName;
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
                    previewPicRawData = IntsToBytes(pixels);
                    PIC_OR_VIDEO = PIC;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private byte[] IntsToBytes(int[] pixels) {
        byte[] bytes = new byte[pixels.length * 4];
        int offset = 0;
        for (int pixel : pixels) {
            bytes[offset++] = (byte) (pixel & 0xff);// 最低位
            bytes[offset++] = (byte) ((pixel >> 8) & 0xff);// 次低位
            bytes[offset++] = (byte) ((pixel >> 16) & 0xff);// 次高位
            bytes[offset++] = (byte) (pixel >>> 24);// 最高位,无符号右移。
        }
        return bytes;
    }

    private FishEye360 bowl;
    private FourEye360 fourEye;
    private TwoRectangle rectangle;
    private Cylinder cylinder;
    //180
    private FishEye180 curvedPlate;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao360RenderMgr onSurfaceCreated");
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
        fourEye.onSurfaceChange(width, height);
        rectangle.onSurfaceChange(width, height);
        //1080 x 1794
        cylinder.onSurfaceChange(width, height);

        curvedPlate.onSurfaceChange(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try{
            if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
                YUVFrame frame = mCircularBuffer.getFrame();
                switch (RENDER_MODE){
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
                if(frame!=null) frame.release();
            }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
                switch (RENDER_MODE){
                    case LTRenderMode.RENDER_MODE_360:{
                        if( !bowl.isInitialized ){
                            bowl.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        bowl.onDrawFrame(null);
                    }break;
                    case LTRenderMode.RENDER_MODE_FOUR_EYE:{
                        if( !fourEye.isInitialized ){
                            fourEye.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        fourEye.onDrawFrame(null);
                    }break;
                    case LTRenderMode.RENDER_MODE_TWO_RECTANGLE:{
                        if( !rectangle.isInitialized ){
                            rectangle.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        rectangle.onDrawFrame(null);
                    }break;
                    case LTRenderMode.RENDER_MODE_CYLINDER:{
                        if( !cylinder.isInitialized ){
                            cylinder.onSurfaceCreate(previewPicPathName, previewPicRawData);
                        }
                        cylinder.onDrawFrame(null);
                    }break;
                    default:
                        Log.w(TAG, "LangTao360RenderMgr RenderMode "+RENDER_MODE+" not recognized !!!");
                        break;
                }
            }
            Log.w(TAG, "LangTao360RenderMgr onDrawFrame on RENDER_MODE:"+RENDER_MODE+" PIC_OR_VIDEO:"+PIC_OR_VIDEO);
        }catch (Exception e){
            e.printStackTrace();
        }
    }





    @Override
    public void handleTouchUp(final float x, final float y, final float xVelocity, final float yVelocity) {
        switch (RENDER_MODE){
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
