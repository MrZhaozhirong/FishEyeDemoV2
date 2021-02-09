package com.langtao.ltpanorama.shape;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;

import com.langtao.fisheye.FishEyeProc;
import com.langtao.fisheye.OneFisheye360Param;
import com.langtao.fisheye.OneFisheyeOut;
import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.IndexBuffer;
import com.langtao.ltpanorama.data.VertexBuffer;
import com.langtao.ltpanorama.program.OneFishEye360ShaderProgram;
import com.langtao.ltpanorama.utils.CameraViewport;
import com.langtao.ltpanorama.utils.Geometry;
import com.langtao.ltpanorama.utils.MatrixHelper;
import com.langtao.ltpanorama.utils.TextureHelper;

import java.nio.ByteBuffer;

/**
 * Created by zzr on 2017/8/16.
 */

public class FishEye360Desktop {

    private static final String TAG = "OneFishEye360";
    private double overture = 50;
    //================================建模视频帧相关==============================================================
    //================================建模视频帧相关==============================================================
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COORDINATE_COMPONENT_COUNT = 3; // 每个顶点的坐标数 x y z
    private static final int TEXTURE_COORDINATE_COMPONENT_COUNT = 2; // 每个纹理坐标为 S T两个

    static {
        System.loadLibrary("one_fisheye");
        System.loadLibrary("LTFishEyeProc");
    }

    public volatile boolean isInitialized = false;
    private float[] mProjectionMatrix = new float[16];// 4x4矩阵 存储投影矩阵
    private float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    private float[] mModelMatrix = new float[16];// 模型变换矩阵
    private float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mFrameWidth;
    private int mFrameHeight;
    private CameraViewport mCameraEye;
    private OneFisheyeOut out;
    private VertexBuffer verticesBuffer;
    private VertexBuffer texCoordsBuffer;
    private IndexBuffer indicesBuffer;
    private int numElements = 0;
    private int drawElementType;
    private OneFishEye360ShaderProgram fishShader;
    private int[] _yuvTextureIDs = new int[]{0};
    private float mLastX;
    private float mLastY;
    private float mFingerRotationX = 0;
    private float mFingerRotationY = 0;
    private float mFingerRotationZ = 0;
    private float[] mMatrixRotationX = new float[16];
    private float[] mMatrixRotationY = new float[16];
    private float[] mMatrixRotationZ = new float[16];
    private volatile boolean gestureInertia_isStop = true;
    private volatile boolean pullupInertia_isStop = true;
    //自动旋转相关
    private volatile boolean isNeedAutoScroll = false;
    private volatile int direction = 0;
    private volatile boolean operating = false;
    private float UPPER_VISION_VALUE = -10.0f;
    private float LOWER_VISION_VALUE = -5.0f;
    private float watch_position_z = 0.2f;
    private float watch_position_y = -0.3f;

    public float getForwardPosition() {
        return watch_position_y;
    }

    public void setForwardPosition(float position) {
        this.watch_position_y = position;
        if (mCameraEye!=null) {
            mCameraEye.setCameraVector(0, watch_position_y, watch_position_z);
            mCameraEye.setTargetViewVector(0.0f, 1.0f, 0.5f);
            mCameraEye.setCameraUpVector(0f, 0f, 1.0f);

            Matrix.setLookAtM(this.mViewMatrix, 0,
                    mCameraEye.cx, mCameraEye.cy, mCameraEye.cz, //摄像机位置
                    mCameraEye.tx, mCameraEye.ty, mCameraEye.tz, //摄像机目标视点
                    mCameraEye.upx, mCameraEye.upy, mCameraEye.upz);//摄像机头顶方向向量
        }
    }

    public float getWatchPosition() {
        return watch_position_z;
    }

    public void setWatchPosition(float watch_position) {
        this.watch_position_z = watch_position;
        if (mCameraEye!=null) {
            mCameraEye.setCameraVector(0, watch_position_y, watch_position_z);
            mCameraEye.setTargetViewVector(0.0f, 1.0f, 0.5f);
            mCameraEye.setCameraUpVector(0f, 0f, 1.0f);

            Matrix.setLookAtM(this.mViewMatrix, 0,
                    mCameraEye.cx, mCameraEye.cy, mCameraEye.cz, //摄像机位置
                    mCameraEye.tx, mCameraEye.ty, mCameraEye.tz, //摄像机目标视点
                    mCameraEye.upx, mCameraEye.upy, mCameraEye.upz);//摄像机头顶方向向量
        }
    }

    public double getOverture() {
        return overture;
    }

    public void setOverture(double overture) {
        this.overture = overture;
        if (mSurfaceWidth!=0) {
            float ratio = (float) mSurfaceWidth / (float) mSurfaceHeight;
            MatrixHelper.perspectiveM(this.mProjectionMatrix,
                    (float) overture, ratio, 0f, 1000f);
        }
    }

    public float getUpperVisionValue() {
        return UPPER_VISION_VALUE;
    }

    public void setUpperVisionValue(float UPPER_VISION_VALUE) {
        this.UPPER_VISION_VALUE = UPPER_VISION_VALUE;
    }

    public float getLowerVisionValue() {
        return LOWER_VISION_VALUE;
    }

    public void setLowerVisionValue(float LOWER_VISION_VALUE) {
        this.LOWER_VISION_VALUE = LOWER_VISION_VALUE;
    }

    //================================操作封装==================================================================
    //================================模型操作相关==============================================================
    public FishEye360Desktop() {
        resetMatrixStatus();
        mScale = 0;
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.scaleM(this.mModelMatrix, 0, 1.0f, 1.0f, 1.0f);

        mCameraEye = new CameraViewport();
        mCameraEye.setCameraVector(0, watch_position_y, watch_position_z);
        mCameraEye.setTargetViewVector(0.0f, 1.0f, 0.5f);
        mCameraEye.setCameraUpVector(0f, 0f, 1.0f);
    }

    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    public void onSurfaceCreate(String previewPicPathName, int[] previewPicData) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   //指定需要的是原始数据，非压缩数据
        Bitmap bitmap = BitmapFactory.decodeFile(previewPicPathName, options);
        if (bitmap == null) {
            throw new IllegalStateException("previewPicPathName not load in bitmap!");
        }

        createBufferData(bitmap.getWidth(), bitmap.getHeight(), previewPicData);
        buildProgram();
        initTexture(bitmap);
        setAttributeStatus();
        isInitialized = true;
        bitmap.recycle();
        bitmap = null;
    }

    public void onSurfaceCreate(@NonNull YUVFrame frame) {
        if (frame == null) return;
        createBufferData(frame.getWidth(), frame.getHeight(), frame);
        buildProgram();
        initTexture(frame.getWidth(), frame.getHeight(), frame);
        setAttributeStatus();
        isInitialized = true;
    }

    public void onSurfaceChange(int width, int height) {
        float ratio = (float) width / (float) height;
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        MatrixHelper.perspectiveM(this.mProjectionMatrix,
                (float) overture, ratio, 0f, 1000f);

        Matrix.setLookAtM(this.mViewMatrix, 0,
                mCameraEye.cx, mCameraEye.cy, mCameraEye.cz, //摄像机位置
                mCameraEye.tx, mCameraEye.ty, mCameraEye.tz, //摄像机目标视点
                mCameraEye.upx, mCameraEye.upy, mCameraEye.upz);//摄像机头顶方向向量
    }

    private void createBufferData(int width, int height, int[] previewPicRawData) {
        if (out == null) {
            try {
                OneFisheye360Param outParam = new OneFisheye360Param();
                Log.w(TAG, "OneFisheye360Param rgb width&height : " + width + "  " + height);
                int ret = FishEyeProc.getOneFisheye360ParamIntRGBA(previewPicRawData, width, height, outParam);
                if (ret != 0) {
                    return;
                }

                out = FishEyeProc.oneFisheye360Func(100, outParam);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            } finally {

            }
        }

        verticesBuffer = new VertexBuffer(out.vertices);
        texCoordsBuffer = new VertexBuffer(out.texCoords);

        numElements = out.indices.length;
        if (numElements < Short.MAX_VALUE) {
            short[] element_index = new short[numElements];
            for (int i = 0; i < out.indices.length; i++) {
                element_index[i] = (short) out.indices[i];
            }
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_SHORT;
        } else {
            int[] element_index = new int[numElements];
            System.arraycopy(out.indices, 0, element_index, 0, out.indices.length);
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_INT;
        }
    }

    private void createBufferData(int width, int height, YUVFrame frame) {
        if (out == null) {
            try {
                OneFisheye360Param outParam = new OneFisheye360Param();
                Log.w(TAG, "OneFisheye360Param YUVFrame width&height : " + width + "  " + height);
                int ret = FishEyeProc.getOneFisheye360Param(frame.getYuvbyte(), width, height, outParam);
                if (ret != 0) {
                    return;
                }
                Log.w(TAG, "width : " + outParam.width);
                Log.w(TAG, "height : " + outParam.height);
                Log.w(TAG, "circleCenterX : " + outParam.circleCenterX);
                Log.w(TAG, "circleCenterY : " + outParam.circleCenterY);
                Log.w(TAG, "verticalRadius : " + outParam.verticalRadius);
                Log.w(TAG, "horizontalRadius : " + outParam.horizontalRadius);

                out = FishEyeProc.oneFisheye360Func(100, outParam);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        verticesBuffer = new VertexBuffer(out.vertices);
        texCoordsBuffer = new VertexBuffer(out.texCoords);

        numElements = out.indices.length;
        if (numElements < Short.MAX_VALUE) {
            short[] element_index = new short[numElements];
            for (int i = 0; i < out.indices.length; i++) {
                element_index[i] = (short) out.indices[i];
            }
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_SHORT;
        } else {
            int[] element_index = new int[numElements];
            System.arraycopy(out.indices, 0, element_index, 0, out.indices.length);
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_INT;
        }
    }

    private void buildProgram() {
        fishShader = new OneFishEye360ShaderProgram();
        //GLES20.glUseProgram(fishShader.getShaderProgramId());
    }

    private boolean initTexture(Bitmap bitmap) {
        if (fishShader == null) return false;
        GLES20.glUseProgram(fishShader.getShaderProgramId());
        int yuvTextureID = TextureHelper.loadTexture(bitmap);
        if (yuvTextureID == 0) {
            Log.w(TAG, "loadBitmapToTexture return TextureID=0 !");
            return false;
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureID);
        GLES20.glUniform1i(fishShader.uLocationSamplerRGB, 0); // => GLES20.GL_TEXTURE0

        _yuvTextureIDs[0] = yuvTextureID;
        return true;
    }

    private boolean initTexture(int width, int height, YUVFrame frame) {
        if (fishShader == null) return false;
        GLES20.glUseProgram(fishShader.getShaderProgramId());
        int[] yuvTextureIDs = TextureHelper.loadYUVTexture2(width, height,
                frame.getYDataBuffer(), frame.getUDataBuffer(), frame.getVDataBuffer());
        if (yuvTextureIDs == null || yuvTextureIDs.length != 3) {
            Log.w(TAG, "yuvTextureIDs object's length not equals 3 !");
            return false;
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[0]);
        GLES20.glUniform1i(fishShader.uLocationSamplerY, 0); // => GLES20.GL_TEXTURE0

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[1]);
        GLES20.glUniform1i(fishShader.uLocationSamplerU, 1); // => GLES20.GL_TEXTURE1

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[2]);
        GLES20.glUniform1i(fishShader.uLocationSamplerV, 2); // => GLES20.GL_TEXTURE2

        _yuvTextureIDs = yuvTextureIDs;
        return true;
    }

    private void setAttributeStatus() {
        if (fishShader == null) return;
        GLES20.glUseProgram(fishShader.getShaderProgramId());

        float kColorConversion420[] = {
                1.0f, 1.0f, 1.0f,
                0.0f, -0.39465f, 2.03211f,
                1.13983f, -0.58060f, 0.0f
        };

        GLES20.glUniformMatrix3fv(fishShader.uLocationCCM, 1, false, kColorConversion420, 0);
        if (verticesBuffer == null || texCoordsBuffer == null) {
            return;
        }
        verticesBuffer.setVertexAttribPointer(fishShader.aPositionLocation,
                POSITION_COORDINATE_COMPONENT_COUNT,
                POSITION_COORDINATE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);

        texCoordsBuffer.setVertexAttribPointer(fishShader.aTexCoordLocation,
                TEXTURE_COORDINATE_COMPONENT_COUNT,
                TEXTURE_COORDINATE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);
    }

    public void onDrawFrame(YUVFrame frame) {
        if (!isInitialized) return;
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        GLES20.glUseProgram(fishShader.getShaderProgramId());

        updateTexture(frame);
        GLES20.glUniform1i(fishShader.uLocationImageMode, 0);

        updateBallMatrix();
        if (isNeedAutoScroll) {
            autoRotated();
        }
        setAttributeStatus();
        this.draw();
    }

    public void onDrawPreviewPic() {
        if (!isInitialized) return;
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        GLES20.glUseProgram(fishShader.getShaderProgramId());

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
        GLES20.glUniform1i(fishShader.uLocationSamplerRGB, 0);
        GLES20.glUniform1i(fishShader.uLocationImageMode, 1);

        updateBallMatrix();
        if (isNeedAutoScroll) {
            autoRotated();
        }
        setAttributeStatus();
        this.draw();
    }

    private boolean updateTexture(YUVFrame yuvFrame) {
        if (yuvFrame == null || fishShader == null) return false;
        int width = yuvFrame.getWidth();
        int height = yuvFrame.getHeight();
        ByteBuffer yDatabuffer = yuvFrame.getYDataBuffer();
        ByteBuffer uDatabuffer = yuvFrame.getUDataBuffer();
        ByteBuffer vDatabuffer = yuvFrame.getVDataBuffer();

        GLES20.glUseProgram(fishShader.getShaderProgramId());
        //if (width != mFrameWidth || height != mFrameHeight)
        {
            //先去掉旧的纹理
            GLES20.glDeleteTextures(_yuvTextureIDs.length, _yuvTextureIDs, 0);
            //重新加载数据
            int[] yuvTextureIDs = TextureHelper.loadYUVTexture2(width, height,
                    yDatabuffer, uDatabuffer, vDatabuffer);
            _yuvTextureIDs = yuvTextureIDs;
            mFrameWidth = width;
            mFrameHeight = height;
        }
        //else
        //{//长宽没变，更新纹理，不重建
        //    TextureHelper.updateTexture2(_yuvTextureIDs[0], mFrameWidth, mFrameHeight, yDatabuffer);
        //    TextureHelper.updateTexture2(_yuvTextureIDs[1], mFrameWidth, mFrameHeight, uDatabuffer);
        //    TextureHelper.updateTexture2(_yuvTextureIDs[2], mFrameWidth, mFrameHeight, vDatabuffer);
        //}
        //重新加载纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
        GLES20.glUniform1i(fishShader.uLocationSamplerY, 0); // => GLES20.GL_TEXTURE0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[1]);
        GLES20.glUniform1i(fishShader.uLocationSamplerU, 1); // => GLES20.GL_TEXTURE1
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[2]);
        GLES20.glUniform1i(fishShader.uLocationSamplerV, 2); // => GLES20.GL_TEXTURE2
        return true;
    }

    public void draw() {
        if (fishShader == null) return;
        GLES20.glUseProgram(fishShader.getShaderProgramId());
        //将最终变换矩阵写入
        GLES20.glUniformMatrix4fv(fishShader.uMVPMatrixLocation, 1, false, getFinalMatrix(), 0);
        if (indicesBuffer == null) {
            return;
        }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, drawElementType, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public float[] getRotationPoint() {
        return new float[]{mFingerRotationX, mFingerRotationY};
    }

    public void setRotationPoint(float[] point) {
        if (point.length != 2) return;
        mFingerRotationX = point[0];
        mFingerRotationY = point[1];
    }

    private void updateBallMatrix() {
        Matrix.setIdentityM(this.mMatrixRotationX, 0);
        Matrix.setIdentityM(this.mMatrixRotationZ, 0);
        Matrix.rotateM(this.mMatrixRotationZ, 0, this.mFingerRotationX, 0, 0, 1);
        Matrix.rotateM(this.mMatrixRotationX, 0, this.mFingerRotationY, 1, 0, 0);
        Matrix.multiplyMM(this.mModelMatrix, 0, this.mMatrixRotationX, 0, this.mMatrixRotationZ, 0);
    }

    public void resetMatrixStatus() {
        mFingerRotationX = 0;
        mFingerRotationY = 0;
        mFingerRotationZ = 0;
        Matrix.setIdentityM(this.mMatrixRotationX, 0);
        Matrix.setIdentityM(this.mMatrixRotationY, 0);
        Matrix.setIdentityM(this.mMatrixRotationZ, 0);
    }

    private void autoRotated() {
        if (operating) return;
        if (direction == 0)
            this.mFingerRotationX -= 0.2f;
        else
            this.mFingerRotationX += 0.2f;

        if (this.mFingerRotationX > 360 || this.mFingerRotationX < -360) {
            this.mFingerRotationX = this.mFingerRotationX % 360;
        }
    }

    public void handleTouchDown(float x, float y) {
        this.mLastX = x;
        this.mLastY = y;
        this.gestureInertia_isStop = true;
    }

    public void handleTouchUp(final float x, final float y,
                              final float xVelocity, final float yVelocity) {
        this.mLastX = 0;
        this.mLastY = 0;
        this.gestureInertia_isStop = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleGestureInertia(x, y, xVelocity, yVelocity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleGestureInertia(float x, float y, float xVelocity, float yVelocity)
            throws InterruptedException {
        this.gestureInertia_isStop = false;
        float mXVelocity = xVelocity / 8000f;
        float mYVelocity = yVelocity / 8000f;
        while (!this.gestureInertia_isStop) {
            this.mFingerRotationY -= mYVelocity;
            if (this.mFingerRotationY > LOWER_VISION_VALUE) {
                this.mFingerRotationY = LOWER_VISION_VALUE;
            }
            if (this.mFingerRotationY < UPPER_VISION_VALUE) {
                this.mFingerRotationY = UPPER_VISION_VALUE;
            }

            this.mFingerRotationX -= mXVelocity;
            if (Math.abs(mXVelocity - 0.995f * mXVelocity) < 0.0000001f) {
                if (this.pullupInertia_isStop) {
                    this.gestureInertia_isStop = true;
                }
            }
            //----------------------------------------------------------------------------
            mYVelocity = 0.98f * mYVelocity;
            mXVelocity = 0.995f * mXVelocity;
            Thread.sleep(2);
            operating = true;
        }
        operating = false;
    }

    public void handleTouchMove(float x, float y) {
        float offsetX = this.mLastX - x;
        float offsetY = this.mLastY - y;
        this.mFingerRotationX += offsetX / 10;
        this.mFingerRotationY += offsetY / 10;

        if (this.mFingerRotationY > LOWER_VISION_VALUE) {
            this.mFingerRotationY = LOWER_VISION_VALUE;
        }
        if (this.mFingerRotationY < UPPER_VISION_VALUE) {
            this.mFingerRotationY = UPPER_VISION_VALUE;
        }
        Log.i(TAG, "RotationY : " + mFingerRotationY);

        this.mLastX = x;
        this.mLastY = y;
    }

    private float mScale;
    public void handleMultiTouch(float distance) {
        float present =   distance > 0 ? 0.99f : 1.01f;
        if(distance > 0) {
            if(mScale < 45)
                mScale ++;
            else
                return;
        } else {
            if(mScale > -1)
                mScale --;
            else
                return;
        }
        Log.w(TAG, "current scale : "+mScale);

        Geometry.Vector target = mCameraEye.getTarget();
        mCameraEye.scaleCameraByPos(target, present);

        Matrix.setLookAtM(this.mViewMatrix, 0,
                mCameraEye.cx, mCameraEye.cy, mCameraEye.cz,
                mCameraEye.tx, mCameraEye.ty, mCameraEye.tz,
                mCameraEye.upx, mCameraEye.upy, mCameraEye.upz);
    }

    public float getCurrentScale() {
        return this.mScale;
    }

    public void setScale(float scale) {
        this.mScale = scale;
        // 初始化 恢复之前的放大倍数
        float present = 0.99f;
        for(int i=0; i<scale; i++) {
            Geometry.Vector target = mCameraEye.getTarget();
            mCameraEye.scaleCameraByPos(target, present);

            Matrix.setLookAtM(this.mViewMatrix, 0,
                    mCameraEye.cx, mCameraEye.cy, mCameraEye.cz,
                    mCameraEye.tx, mCameraEye.ty, mCameraEye.tz,
                    mCameraEye.upx, mCameraEye.upy, mCameraEye.upz);
        }
    }



















    public void setAutoCruise(boolean autoCruise) {
        this.isNeedAutoScroll = autoCruise;
    }

    public void setCruiseDirection(int direction) {
        this.direction = direction;
    }

}
