package com.langtao.ltpanorama;

import android.opengl.GLSurfaceView;

import com.langtao.ltpanorama.component.CircularYUVBuffer;

/**
 * Created by zzr on 2017/12/12.
 */

public abstract class LTRenderManager implements GLSurfaceView.Renderer {

    public static final String TAG = "LTRenderManager";

    CircularYUVBuffer mCircularBuffer;

    public void addBuffer(int width, int height, byte[] byYdata, byte[] byUdata, byte[] byVdata) {
        mCircularBuffer.add(width,height,byYdata,byUdata,byVdata);
    }

    LTRenderManager() {
        mCircularBuffer = new CircularYUVBuffer();
    }

    int RENDER_MODE;

    public void setRenderMode(int mode) {
        this.RENDER_MODE = mode;
    }

    public void clearBuffer() {
        mCircularBuffer.clear();
    }

    public abstract void handleDoubleClick();

    public abstract void handleTouchDown(float x, float y);

    public abstract void handleTouchUp(float x, float y, float xVelocity, float yVelocity);

    public abstract void handleTouchMove(float x, float y);

    public abstract void handleMultiTouch(float distance);

    public abstract void setAutoCruise(boolean autoCruise);
}
