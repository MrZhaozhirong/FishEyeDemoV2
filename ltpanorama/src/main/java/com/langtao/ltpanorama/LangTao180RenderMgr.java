package com.langtao.ltpanorama;

import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.shape.FishEye180;
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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w(TAG, "LangTao180RenderMgr onSurfaceCreated");
        curvedPlate = new FishEye180();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w(TAG, "LangTao180RenderMgr onSurfaceChanged");
        curvedPlate.onSurfaceChange(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            YUVFrame buffer = mCircularBuffer.getFrame();
            if(buffer != null){
                if(!curvedPlate.isInitialized)
                    curvedPlate.onSurfaceCreate(buffer);

                curvedPlate.updateTexture(buffer);
            }
            curvedPlate.updateCruise();
            curvedPlate.updateMatrix();
            curvedPlate.draw();
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
        if(curvedPlate!=null)
            curvedPlate.handleTouchUp(x, y, xVelocity, yVelocity);
    }
    @Override
    public void handleTouchDown(float x, float y) {
        if(curvedPlate!=null)
            curvedPlate.handleTouchDown(x, y);
    }
    @Override
    public void handleTouchMove(float x, float y) {
        if(curvedPlate!=null)
            curvedPlate.handleTouchMove(x, y);
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
