package com.langtao.ltpanorama;

import android.opengl.GLES20;
import android.opengl.GLException;
import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.FrameBuffer;
import com.langtao.ltpanorama.shape.LTRenderMode;
import com.langtao.ltpanorama.shape.PanoTemplateBall;
import com.langtao.ltpanorama.shape.PanoTemplateFour;
import com.langtao.ltpanorama.shape.PanoTemplateRectangleFBO;
import com.langtao.ltpanorama.shape.PanoramaNewBall;

import java.io.File;
import java.nio.IntBuffer;

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


    // 2019.05.13
    // 是否打开启动动画
    private boolean isStartBootAnimation;
    public void startBootAnimation(boolean isStart) {
        isStartBootAnimation = isStart;
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

    private int x, y;
    private int w, h;
    private boolean capture = false;
    private FrameBuffer fbo;
    private CaptureScreenCallbacks callback;
    public interface CaptureScreenCallbacks {
        void onCaptureScreenReady(int w,int h,int[] data);
    }
    public void requestCaptureScreen(int x,int y, int w,int h,
                                     CaptureScreenCallbacks callback){
        this.x = x;this.y = y;
        this.w = w;this.h = h;
        capture = true;
        this.callback = callback;
    }
    private void drawCaptureScreen(YUVFrame frame) {
        if( fbo==null) {
            fbo = new FrameBuffer();
            fbo.setup(w,h);
        }
        fbo.begin();
        if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_ANIMATION_3)) {
            if(frame!=null) {
                panoTmBall.onDrawFrame(frame);
            }
        }else if(panoRenderType.equalsIgnoreCase(LT_PANORAMA_SCREEN_4)){
            if(frame!=null) {
                fourTmBall.onDrawFrame(frame);
            }
        }
        fbo.end();
        captureScreen();
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
        return vertical;
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
                                panoTemplateConfigFileName_AbsolutePath,
                                isStartBootAnimation);
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
                if( capture) {
                    drawCaptureScreen(buffer);
                    capture = false;
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
