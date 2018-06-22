package com.langtao.ltpanorama;

import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.FishEye180;
import com.langtao.ltpanorama.shape.FishEye180Rectangle;
import com.langtao.ltpanorama.shape.LTRenderMode;

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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao180RenderMgr onSurfaceCreated");
        curvedPlate = new FishEye180();
        pavedRect = new FishEye180Rectangle();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w(TAG, "LangTao180RenderMgr onSurfaceChanged");
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

            if(frame!=null) frame.release();
        }catch (Exception e){
            e.printStackTrace();
        }
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
