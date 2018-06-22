package com.langtao.ltpanorama;

import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.ltpanorama.shape.PanoTemplateBall;
import com.langtao.ltpanorama.shape.PanoTemplateFour;
import com.langtao.ltpanorama.shape.PanoTemplateRectangleFBO;
import com.langtao.ltpanorama.shape.PanoramaNewBall;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zzr on 2017/12/4.
 */

public class LangTao720RenderMgr extends LTRenderManager {
    private static final String TAG = LTRenderManager.TAG+"-720";
    public static final String LT_PANORAMA_PIC = "LT_PANORAMA_PIC";
    public static final String LT_PANORAMA_VIDEO = "LT_PANORAMA_VIDEO";
    public static final String LT_PANORAMA_ANIMATION_3 = "LT_PANORAMA_ANIMATION_3_";
    public static final String LT_PANORAMA_SCREEN_4 = "LT_PANORAMA_SCREEN_4";

    public LangTao720RenderMgr() {
        super();
        RENDER_MODE = LTRenderMode.RENDER_MODE_CRYSTAL;
    }

    // 设置 全景模板配置文件 绝对路径
    private String panoTemplateConfigFileName_AbsolutePath = null;

    public void setPanoTemConfigFile(String absolutePath_fileName) {
        this.panoTemplateConfigFileName_AbsolutePath = absolutePath_fileName;
        if(panoTemplateConfigFileName_AbsolutePath==null ||
                "".equalsIgnoreCase(panoTemplateConfigFileName_AbsolutePath) ||
                !new File(panoTemplateConfigFileName_AbsolutePath).exists() ) {
            Log.e(TAG, "Error: setPanoTemplateConfigFile is null or File not exists !!!");
            Log.e(TAG, "Error: It will throw Exceptions in init LangTao-GL !!!");
        }
    }

    // 设置 全景模板解密GID
    private String panoTemplateConfigFile_gid = null;

    public void setPanoTemSecretKey(String secretGIDStr) {
        this.panoTemplateConfigFile_gid = secretGIDStr;
        if(panoTemplateConfigFile_gid==null ||
                "".equalsIgnoreCase(panoTemplateConfigFile_gid) ) {
            Log.e(TAG, "Error: setPanoTemSecretKey is null, invalid !!!");
            Log.e(TAG, "Error: It will throw Exceptions in init LangTao-GL !!!");
        }
    }



    // 设置请求生成 2:1全景图 回调
    private volatile boolean requestScreenShot = false;
    public void getPanoramaPicture(final PanoTemplateRectangleFBO.ScreenShotReadyCallback callback){
        if(panoPicGenerator != null && callback!=null){
            // 放线程处理，不要阻塞，否则会爆炸 重申第二次
            panoPicGenerator.requestScreenShot(callback);
            requestScreenShot = true;
        }
    }

    // 静态预览图
    private String bitmap_path = null;
    public void setPreviewPanoramaPicture(String bitmap_path){
        this.bitmap_path = bitmap_path;
    }

    private String panoRenderType = LT_PANORAMA_ANIMATION_3;
    private String PIC_OR_VIDEO = LT_PANORAMA_VIDEO;
    public void setPanoramaMode(String mode) {
        if(LT_PANORAMA_ANIMATION_3.equalsIgnoreCase(mode) ||
                LT_PANORAMA_SCREEN_4.equalsIgnoreCase(mode) )
            panoRenderType = mode;
    }
    public void setPanoRenderType(String type) {
        if(LT_PANORAMA_PIC.equalsIgnoreCase(type) ||
                LT_PANORAMA_VIDEO.equalsIgnoreCase(type) )
            PIC_OR_VIDEO = type;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private PanoTemplateBall panoTmBall;//视频3动态切换
    private PanoTemplateFour fourTmBall; //视频四分屏
    private PanoTemplateRectangleFBO panoPicGenerator; //用于生成2:1全景图
    private PanoramaNewBall picTmBall; //显示静态图

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao720RenderMgr onSurfaceCreated");
        panoTmBall = new PanoTemplateBall(RENDER_MODE);
        panoPicGenerator = new PanoTemplateRectangleFBO();

        picTmBall = new PanoramaNewBall(RENDER_MODE);
        fourTmBall = new PanoTemplateFour();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w(TAG, "LangTao720RenderMgr onSurfaceChanged "+width+" x "+height);
        panoTmBall.onSurfaceChanged(width,height);
        picTmBall.onSurfaceChanged(width, height);
        fourTmBall.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_VIDEO)) {
                YUVFrame buffer = mCircularBuffer.getFrame();
                if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)) {
                    if(!panoTmBall.isInitialized && buffer!=null) {
                        panoTmBall.onSurfaceCreated(
                                panoTemplateConfigFile_gid,
                                panoTemplateConfigFileName_AbsolutePath);
                    }
                    panoTmBall.onDrawFrame(buffer);
                    // 请求生成 2:1全景图
                    if(requestScreenShot && buffer!=null) {
                        generatePanoramaPic(buffer);
                    }
                }else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
                    if(!fourTmBall.isInitialized && buffer!=null) {
                        fourTmBall.onSurfaceCreated(
                                panoTemplateConfigFile_gid,
                                panoTemplateConfigFileName_AbsolutePath);
                    }
                    fourTmBall.onDrawFrame(buffer);
                }
                if(buffer != null) buffer.release();
            }
            else
            {//LT_PANORAMA_PIC
                if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)) {
                    if(!picTmBall.isInitialized) {
                        picTmBall.onSurfaceCreated(bitmap_path);
                    }
                    picTmBall.onDrawPreviewPic3();
                }else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
                    if(!picTmBall.isInitialized) {
                        picTmBall.onSurfaceCreated(bitmap_path);
                    }
                    picTmBall.onDrawPreviewPic4();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //生成全景图。
    private void generatePanoramaPic(YUVFrame frame) {
        if(!panoPicGenerator.isInitialized ){
            panoPicGenerator.onEGLSurfaceCreated(
                    panoTemplateConfigFile_gid,
                    panoTemplateConfigFileName_AbsolutePath);
        }
        if(frame !=null ){
            panoPicGenerator.initFBO(frame.getWidth(), frame.getHeight());
            panoPicGenerator.draw(frame);
            requestScreenShot = false;
        }
    }



    @Override
    public void setRenderMode(int mode) {
        super.setRenderMode(mode);
    }

    @Override
    public void handleDoubleClick() { }

    @Override
    public void handleMultiTouch(float distance) {
        if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)
                && PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                picTmBall.handleMultiTouch(distance);
        }
        else// LT_PANORAMA_VIDEO
        {
            if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)){
                if(panoTmBall !=null)
                    panoTmBall.handleMultiTouch(distance);
            }
            //else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
            //    if(fourTmBall !=null)
            //        fourTmBall.handleMultiTouch(distance);
            //}
        }
    }
    @Override
    public void handleTouchUp(final float x, final float y, final float xVelocity, final float yVelocity) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                picTmBall.handleTouchUp(x, y, xVelocity, yVelocity);
        }
        else// LT_PANORAMA_VIDEO
        {
            if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
                if(fourTmBall !=null)
                    fourTmBall.handleTouchUp(x, y, xVelocity, yVelocity);
            }
            else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)) {
                if(panoTmBall !=null)
                    panoTmBall.handleTouchUp(x, y, xVelocity, yVelocity);
            }
        }

    }
    @Override
    public void handleTouchDown(float x, float y) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                picTmBall.handleTouchDown(x, y);
        }
        else// LT_PANORAMA_VIDEO
        {
            if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)) {
                if(panoTmBall !=null)
                    panoTmBall.handleTouchDown(x, y);
            }
            else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
                if(fourTmBall !=null)
                    fourTmBall.handleTouchDown(x, y);
            }
        }
    }

    @Override
    public void handleTouchMove(float x, float y) {

        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                picTmBall.handleTouchMove(x, y);
        }
        else // LT_PANORAMA_VIDEO
        {
            if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)) {
                if(panoTmBall !=null)
                    panoTmBall.handleTouchMove(x, y);
            }
            else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
                if(fourTmBall !=null)
                    fourTmBall.handleTouchMove(x, y);
            }
        }
    }

    @Override
    public void setAutoCruise(boolean autoCruise) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                picTmBall.setAutoCruise(autoCruise);
        }
        else
        {
            if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_VIDEO)) {
                if(panoTmBall !=null)
                    panoTmBall.setAutoCruise(autoCruise);
            }
            else if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_VIDEO)) {
                if(fourTmBall !=null)
                    fourTmBall.setAutoCruise(autoCruise);
            }
        }
    }



    // 水晶球->鱼眼->小行星
    public int nextModelShape() {
        if(!panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3))
            return RENDER_MODE;
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_VIDEO)) {
            if(panoTmBall !=null)
                RENDER_MODE = panoTmBall.nextControlMode();
        }
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                RENDER_MODE = picTmBall.nextControlMode();
        }
        return RENDER_MODE;
    }
    // 水晶球<-鱼眼
    public int fishEyeReturnToCrystal() {
        if(!panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3))
            return RENDER_MODE;
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_VIDEO)) {
            if(panoTmBall !=null)
                RENDER_MODE = panoTmBall.fishEyeReturnToCrystal();
        }
        if(PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                RENDER_MODE = picTmBall.fishEyeReturnToCrystal();
        }
        return RENDER_MODE;
    }

    /**
     * 处理vr模式的感应操作
     * @param xAngle
     * @param yAngle
     * @param zAngle
     */
    public void renderRotateVR(float xAngle, float yAngle, float zAngle) {
        if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)
                && PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_VIDEO)) {
            if(panoTmBall !=null)
                panoTmBall.renderRotateVR(xAngle, yAngle, zAngle);
        }
        if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)
                && PIC_OR_VIDEO.equalsIgnoreCase(LT_PANORAMA_PIC)){
            if(picTmBall !=null)
                picTmBall.renderRotateVR(xAngle, yAngle, zAngle);
        }
    }
}
