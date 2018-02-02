package com.langtao.ltpanorama;

import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.ltpanorama.shape.PanoTemplateBall;
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
    private static final String PIC = "PIC";
    private static final String VIDEO = "VIDEO";

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


    // 设置 申请2:1全景图回调
    private volatile boolean requestScreenShot = false;
    public void getPanoramaPicture(final PanoTemplateRectangleFBO.ScreenShotReadyCallback callback){
        if(panoTmRender != null && callback!=null){
            // 放线程处理，不要阻塞，否则会爆炸 重申第二次
            panoTmRender.requestScreenShot(callback);
            requestScreenShot = true;
        }
    }

    // 静态预览图
    private String bitmap_path = null;
    private String PIC_OR_VIDEO = VIDEO; //default
    public void setPreviewPanoramaPicture(String bitmap_path){
        this.bitmap_path = bitmap_path;
    }
    public void setPlayVideo() {
        PIC_OR_VIDEO = VIDEO;
    }
    public void setPreviewPic() {
        PIC_OR_VIDEO = PIC;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private PanoTemplateBall templateBall;//视频
    private PanoTemplateRectangleFBO panoTmRender;//截图
    private PanoramaNewBall picBall; //显示静态图

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao720RenderMgr onSurfaceCreated");
        templateBall = new PanoTemplateBall(RENDER_MODE);
        panoTmRender = new PanoTemplateRectangleFBO();

        picBall = new PanoramaNewBall(RENDER_MODE);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w(TAG, "LangTao720RenderMgr onSurfaceChanged");
        templateBall.onSurfaceChanged(width,height);
        picBall.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //Log.v(TAG, "LangTao720RenderMgr onDrawFrame "+PIC_OR_VIDEO);
        try {
            if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
                templateBall.updateBallControlMode();
                YUVFrame buffer = mCircularBuffer.getFrame();
                if(buffer != null){
                    if(!templateBall.isInitialized) {
                        templateBall.onSurfaceCreated(
                                panoTemplateConfigFile_gid,
                                panoTemplateConfigFileName_AbsolutePath);
                    }
                    templateBall.updateTexture(buffer);
                }
                templateBall.updateBallMatrix();
                templateBall.draw();

                //2:1全景图相关
                if(requestScreenShot ) {
                    if(!panoTmRender.isInitialized ){
                        panoTmRender.onEGLSurfaceCreated(
                                panoTemplateConfigFile_gid,
                                panoTemplateConfigFileName_AbsolutePath);
                    }
                    if(buffer!=null ){
                        panoTmRender.initFBO(buffer.getWidth(), buffer.getHeight());
                        panoTmRender.draw( buffer);
                        requestScreenShot = false;
                    }
                }
                if(buffer != null) buffer.release();
            }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
                picBall.updateBallControlMode();
                if(!picBall.isInitialized) {
                    picBall.onSurfaceCreated(bitmap_path);
                }
                picBall.updateBallMatrix();
                picBall.draw();
            }else {
                Log.e(TAG, "LangTao720RenderMgr onDrawFrame on Error PIC_OR_VIDEO.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void setRenderMode(int mode) {
        super.setRenderMode(mode);
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                templateBall.setRenderMode(mode);
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                picBall.setRenderMode(mode);
        }
    }


    @Override
    public void handleDoubleClick() {

    }

    @Override
    public void handleMultiTouch(float distance) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                templateBall.handleMultiTouch(distance);
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                picBall.handleMultiTouch(distance);
        }
    }
    @Override
    public void handleTouchUp(final float x, final float y, final float xVelocity, final float yVelocity) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                templateBall.handleTouchUp(x, y, xVelocity, yVelocity);
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                picBall.handleTouchUp(x, y, xVelocity, yVelocity);
        }
    }
    @Override
    public void handleTouchDown(float x, float y) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                templateBall.handleTouchDown(x, y);
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                picBall.handleTouchDown(x, y);
        }
    }
    @Override
    public void handleTouchMove(float x, float y) {
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                templateBall.handleTouchMove(x, y);
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                picBall.handleTouchMove(x, y);
        }
    }

    @Override
    public void setAutoCruise(boolean autoCruise) {
        //if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
        //    if(templateBall!=null)
        //        templateBall.autoRotated();
        //}else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
        //    if(picBall!=null)
        //        picBall.autoRotated();
        //}
    }



    // 水晶球->鱼眼->小行星
    public int nextModelShape() {
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                RENDER_MODE = templateBall.nextControlMode();
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                RENDER_MODE = picBall.nextControlMode();
        }
        return RENDER_MODE;
    }
    // 水晶球<-鱼眼
    public int fishEyeReturnToCrystal() {
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                RENDER_MODE = templateBall.fishEyeReturnToCrystal();
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                RENDER_MODE = picBall.fishEyeReturnToCrystal();
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
        if(PIC_OR_VIDEO.equalsIgnoreCase(VIDEO)) {
            if(templateBall!=null)
                templateBall.renderRotateVR(xAngle, yAngle, zAngle);
        }else if(PIC_OR_VIDEO.equalsIgnoreCase(PIC)){
            if(picBall!=null)
                picBall.renderRotateVR(xAngle, yAngle, zAngle);
        }
    }
}
