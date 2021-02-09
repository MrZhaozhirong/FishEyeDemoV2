package com.langtao.ltpanorama;

import android.opengl.GLES20;
import android.opengl.GLException;
import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.FrameBuffer;
import com.langtao.ltpanorama.shape.FishEye180;
import com.langtao.ltpanorama.shape.FishEye180Rectangle;
import com.langtao.ltpanorama.shape.LTRenderMode;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;



/**
 * Created by zzr on 2017/12/8.
 */

public class LangTao180RenderMgr extends LTRenderManager {
    private static final String TAG = LTRenderManager.TAG+"-180";

    public LangTao180RenderMgr() {
        super();
        RENDER_MODE = LTRenderMode.RENDER_MODE_180;
    }

    private FishEye180 curvedPlate;
    private FishEye180Rectangle pavedRect;

    public FishEye180 getCurvedPlate() {
        return curvedPlate;
    }
    public FishEye180Rectangle getPavedRect() {
        return pavedRect;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao180RenderMgr onSurfaceCreated");
        curvedPlate = new FishEye180();
        pavedRect = new FishEye180Rectangle();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w(TAG, "LangTao180RenderMgr onSurfaceChanged "+width+"x"+height);
        curvedPlate.onSurfaceChange(width, height);
        pavedRect.onSurfaceChange(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            YUVFrame frame = mCircularBuffer.getFrame();
            switch (RENDER_MODE) {
                case LTRenderMode.RENDER_MODE_180: {
                    if (!curvedPlate.isInitialized) {
                        curvedPlate.onSurfaceCreate(frame);
                    }
                    curvedPlate.onDrawFrame(frame);
                }break;
                case LTRenderMode.RENDER_MODE_180_PAVED: {
                    if (!pavedRect.isInitialized) {
                        pavedRect.onSurfaceCreate(frame);
                    }
                    pavedRect.onDrawFrame(frame);
                }break;
            }

            if( capture) {
                drawCaptureScreen(frame);
                capture = false;
            }
            if(frame!=null) frame.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private int x, y;
    private int w, h;
    private boolean capture = false;
    private FrameBuffer fbo;
    private CaptureScreenCallbacks callback;
    public interface CaptureScreenCallbacks {
        void onCaptureScreenReady(int w,int h,int[] data);
    }
    // 为解决华为等一些机型利用glReadPixel生成屏幕缩略图出现黑屏没数据的情况，利用fbo重draw一遍再glReadPixel
    public void requestCaptureScreen(int x,int y, int w,int h,
                                     LangTao180RenderMgr.CaptureScreenCallbacks callback){
        this.x = x;this.y = y;
        this.w = w;this.h = h;
        capture = true;
        this.callback = callback;
        Log.i(TAG, "requestCaptureScreen : "+w+"x"+h);
    }
    private void drawCaptureScreen(YUVFrame frame) {
        if( fbo==null) {
            fbo = new FrameBuffer();
            fbo.setup(w,h);
        }
        fbo.begin();
        switch (RENDER_MODE) {
            case LTRenderMode.RENDER_MODE_180: {
                if (!curvedPlate.isInitialized) {
                    curvedPlate.onSurfaceCreate(frame);
                }
                curvedPlate.onDrawFrame(frame);
            }break;
            case LTRenderMode.RENDER_MODE_180_PAVED: {
                if (!pavedRect.isInitialized) {
                    pavedRect.onSurfaceCreate(frame);
                }
                pavedRect.onDrawFrame(frame);
            }break;
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
        return vertical;
    }







    @Override
    public void setAutoCruise(boolean autoCruise) {
        if(curvedPlate!=null)
            curvedPlate.setAutoCruise(autoCruise);
    }

    @Override
    public void handleTouchUp(final float x, final float y, final float xVelocity, final float yVelocity) {
        switch (RENDER_MODE) {
            case LTRenderMode.RENDER_MODE_180: {
                if(curvedPlate!=null)
                    curvedPlate.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
            case LTRenderMode.RENDER_MODE_180_PAVED: {
                if(pavedRect!=null)
                    pavedRect.handleTouchUp(x, y, xVelocity, yVelocity);
            }break;
        }
    }
    @Override
    public void handleTouchDown(float x, float y) {
        switch (RENDER_MODE) {
            case LTRenderMode.RENDER_MODE_180: {
                if(curvedPlate!=null)
                    curvedPlate.handleTouchDown(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_180_PAVED: {
                if(pavedRect!=null)
                    pavedRect.handleTouchDown(x, y);
            }break;
        }
    }
    @Override
    public void handleTouchMove(float x, float y) {
        switch (RENDER_MODE) {
            case LTRenderMode.RENDER_MODE_180: {
                if(curvedPlate!=null)
                    curvedPlate.handleTouchMove(x, y);
            }break;
            case LTRenderMode.RENDER_MODE_180_PAVED: {
                if(pavedRect!=null)
                    pavedRect.handleTouchMove(x, y);
            }break;
        }
    }

    @Override
    public void handleMultiTouch(float distance) {
        if(curvedPlate!=null)
            curvedPlate.handleMultiTouch(distance);
    }
    @Override
    public void handleDoubleClick() {
        if(curvedPlate!=null)
            curvedPlate.handleDoubleClick();
    }
}
