package com.langtao.ltpanorama.shape;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;

import com.langtao.fisheye.FishEyeProc;
import com.langtao.fisheye.OneFisheye360Param;
import com.langtao.fisheye.OneFisheyeOut;
import com.langtao.fisheye.TwoRectangleParam;
import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.IndexBuffer;
import com.langtao.ltpanorama.data.VertexBuffer;
import com.langtao.ltpanorama.program.OneFishEye360ShaderProgram;
import com.langtao.ltpanorama.utils.CameraViewport;
import com.langtao.ltpanorama.utils.MatrixHelper;
import com.langtao.ltpanorama.utils.TextureHelper;

import java.nio.ByteBuffer;

/**
 * Created by zzr on 2017/8/24.
 */

public class TwoRectangle  {

    static {
        System.loadLibrary("one_fisheye");
        System.loadLibrary("LTFishEyeProc");
    }
    private static final String TAG = "FishEye360";
    private final static double overture = 45;
    private float[] mProjectionMatrix = new float[16];// 4x4矩阵 存储投影矩阵
    private float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    private float[] mModelMatrix = new float[16];// 模型变换矩阵
    private float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵

    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }
    //自动旋转相关
    private volatile boolean isNeedAutoScroll = true;
    private volatile int direction = 0;
    public void setAutoCruise(boolean needAutoScroll) {
        isNeedAutoScroll = needAutoScroll;
    }

    public void setCruiseDirection(int direction) {
        this.direction = direction;
    }
    //***************************************************************
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mFrameWidth;
    private int mFrameHeight;

    public CameraViewport eye;

    public TwoRectangle() {
        resetMatrixStatus();
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);

        eye = new CameraViewport();
        eye.setCameraVector(0, 0, 3f);
        eye.setTargetViewVector(0f, 0f, 0.0f);
        eye.setCameraUpVector(0f, 1.0f, 0.0f);
    }

    //================================建模视频帧相关==============================================================
    //================================建模视频帧相关==============================================================
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COORDINATE_COMPONENT_COUNT = 3; // 每个顶点的坐标数 x y z
    private static final int TEXTURE_COORDINATE_COMPONENT_COUNT = 2; // 每个纹理坐标为 S T两个
    public volatile boolean isInitialized = false;
    private OneFisheyeOut out;
    private TwoRectangleParam twoRectangleOut;
    private VertexBuffer verticesBuffer;
    private VertexBuffer texCoordsBuffer;
    private IndexBuffer indicesBuffer;
    private int numElements = 0;
    private int drawElementType;
    private OneFishEye360ShaderProgram shader;
    private int[] _yuvTextureIDs = new int[]{0};


    private void createBufferData(int width, int height, YUVFrame frame) {
        if (out == null) {
            try {
                //InputStream is = context.getResources().openRawResource(R.raw.down);
                //byte[] dataArray = new byte[is.available()];
                //is.read(dataArray);

                OneFisheye360Param outParam = new OneFisheye360Param();
                //int ret = FishEyeProc.getOneFisheye360Param(dataArray, 1280, 1024, outParam);
                int ret = FishEyeProc.getOneFisheye360Param(frame.getYuvbyte(), width, height, outParam);
                if (ret != 0) {
                    return;
                }
                //Log.w(TAG, "OneFisheye360Param YUVFrame width&height : " + width + "  " + height);
                out = FishEyeProc.oneFisheye360TwoRectangleFunc(100, outParam);
                twoRectangleOut = FishEyeProc.oneFisheye360TwoRectangleShaderFunc(outParam);
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
        shader = new OneFishEye360ShaderProgram();
        //GLES20.glUseProgram(shader.getShaderProgramId());
    }

    private boolean initTexture(int width, int height, YUVFrame frame) {
        if(shader ==null) return false;
        GLES20.glUseProgram(shader.getShaderProgramId());
        //int[] yuvTextureIDs = TextureHelper.loadYUVTexture(context, R.raw.down, 1280, 1024);
        int[] yuvTextureIDs = TextureHelper.loadYUVTexture2(width, height,
                frame.getYDataBuffer(), frame.getUDataBuffer(), frame.getVDataBuffer());
        if (yuvTextureIDs == null || yuvTextureIDs.length != 3) {
            Log.w(TAG, "yuvTextureIDs object's length not equals 3 !");
            return false;
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[0]);
        GLES20.glUniform1i(shader.uLocationSamplerY, 0); // => GLES20.GL_TEXTURE0

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[1]);
        GLES20.glUniform1i(shader.uLocationSamplerU, 1); // => GLES20.GL_TEXTURE1

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[2]);
        GLES20.glUniform1i(shader.uLocationSamplerV, 2); // => GLES20.GL_TEXTURE2

        _yuvTextureIDs = yuvTextureIDs;
        return true;
    }

    private void setAttributeStatus() {
        if(shader ==null) return;
        GLES20.glUseProgram(shader.getShaderProgramId());

        float kColorConversion420[] = {
                1.0f, 1.0f, 1.0f,
                0.0f, -0.39465f, 2.03211f,
                1.13983f, -0.58060f, 0.0f
        };

        GLES20.glUniformMatrix3fv(shader.uLocationCCM, 1, false, kColorConversion420, 0);
        if (verticesBuffer == null||texCoordsBuffer==null) {
            return;
        }
        verticesBuffer.setVertexAttribPointer(shader.aPositionLocation,
                POSITION_COORDINATE_COMPONENT_COUNT,
                POSITION_COORDINATE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);

        texCoordsBuffer.setVertexAttribPointer(shader.aTexCoordLocation,
                TEXTURE_COORDINATE_COMPONENT_COUNT,
                TEXTURE_COORDINATE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);

        GLES20.glUniform1i(shader.uLocationImageMode, 0);
        GLES20.glUniform1i(shader.isTwoRectangle, 1);
        GLES20.glUniform1f(shader.cala, twoRectangleOut.cala);
        GLES20.glUniform1f(shader.factorA, twoRectangleOut.factorA);
        GLES20.glUniform1f(shader.factorB, twoRectangleOut.factorB);
        GLES20.glUniform1f(shader.factorC, twoRectangleOut.factorC);
        GLES20.glUniform1f(shader.factorD, twoRectangleOut.factorD);
        GLES20.glUniform1f(shader.factorE, twoRectangleOut.factorE);
        GLES20.glUniform1f(shader.factorF, twoRectangleOut.factorF);
        GLES20.glUniform1f(shader.factorG, twoRectangleOut.factorG);
        GLES20.glUniform1f(shader.factorH, twoRectangleOut.factorH);
        GLES20.glUniform1f(shader.factorI, twoRectangleOut.factorI);
        GLES20.glUniform1f(shader.factorJ, twoRectangleOut.factorJ);
        GLES20.glUniform1f(shader.factorK, twoRectangleOut.factorK);
        GLES20.glUniform1f(shader.factorL, twoRectangleOut.factorL);
    }







    public void onSurfaceCreate(@NonNull YUVFrame frame) {
        if(frame == null) return;
        createBufferData(frame.getWidth(), frame.getHeight(), frame);
        buildProgram();
        initTexture(frame.getWidth(), frame.getHeight(), frame);
        setAttributeStatus();
        isInitialized = true;
    }

    public void onSurfaceChange(int width, int height) {
        float ratio = (float) width / (float) height;
        mSurfaceWidth = width;
        mSurfaceHeight= height;
        MatrixHelper.perspectiveM(this.mProjectionMatrix,
                (float) overture, ratio, 0.1f, 400f);

        Matrix.setLookAtM(this.mViewMatrix, 0,
                0, 0, 3f, //摄像机位置
                0f, 0f, 0.0f, //摄像机目标视点
                0f, 1.0f, 0.0f);//摄像机头顶方向向量

    }

    public void onDrawFrame(@NonNull YUVFrame frame) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glViewport(0,0,mSurfaceWidth,mSurfaceHeight);
        if (isNeedAutoScroll) {
            if(direction == 0) mShaderOffsetX = mShaderOffsetX+1.0f;
            else  mShaderOffsetX = mShaderOffsetX-1.0f;
        }
        if (this.isInitialized) {
            updateTexture(frame);
            updateRectangleMatrix();
            setAttributeStatus();
            this.draw();
        }
        if (isNeedAutoScroll) {
            if(direction == 0) mShaderOffsetX = mShaderOffsetX-1.0f;
            else  mShaderOffsetX = mShaderOffsetX+1.0f;
        }
    }



    private boolean updateTexture(@NonNull YUVFrame yuvFrame) {
        if (yuvFrame == null || shader==null) return false;
        int width = yuvFrame.getWidth();
        int height = yuvFrame.getHeight();
        ByteBuffer yDatabuffer = yuvFrame.getYDataBuffer();
        ByteBuffer uDatabuffer = yuvFrame.getUDataBuffer();
        ByteBuffer vDatabuffer = yuvFrame.getVDataBuffer();

        GLES20.glUseProgram(shader.getShaderProgramId());
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
        GLES20.glUniform1i(shader.uLocationSamplerY, 0); // => GLES20.GL_TEXTURE0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[1]);
        GLES20.glUniform1i(shader.uLocationSamplerU, 1); // => GLES20.GL_TEXTURE1
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[2]);
        GLES20.glUniform1i(shader.uLocationSamplerV, 2); // => GLES20.GL_TEXTURE2
        return true;
    }

    private void updateRectangleMatrix() {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.scaleM(this.mModelMatrix, 0, 1.0f, 1.0f, 1.0f);

        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        Matrix.rotateM(this.mMatrixFingerRotationY, 0, this.mfingerRotationX, 0, 1, 0);
        Matrix.rotateM(this.mMatrixFingerRotationX, 0, this.mfingerRotationY, 1, 0, 0);
        Matrix.multiplyMM(this.mModelMatrix, 0, this.mMatrixFingerRotationX, 0, this.mMatrixFingerRotationY, 0);
    }

    private float offset = 0.0f;
    private float mShaderOffsetX = 0.0f;  // 手指移动X轴方向的偏移值
    private void draw() {
        if(shader ==null) return ;
        GLES20.glUseProgram(shader.getShaderProgramId());
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, getFinalMatrix(), 0);

        //float tempScreenWidth = 1200;
        // mOffsetX是手指移动在X轴的偏移值
        offset = FishEyeProc.getTwoRectangleOffset(offset, mSurfaceWidth, mShaderOffsetX);
        GLES20.glUniform1f(shader.offset, offset);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, drawElementType, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }



    //================================操作封装==================================================================
    //================================模型操作相关==============================================================
    private float mLastX;
    private float mLastY;
    private volatile boolean gestureInertia_isStop = true;
    private float mfingerRotationX = 0;
    private float mfingerRotationY = 0;
    private float mfingerRotationZ = 0;
    private float[] mMatrixFingerRotationX = new float[16];
    private float[] mMatrixFingerRotationY = new float[16];
    private float[] mMatrixFingerRotationZ = new float[16];
    public void resetMatrixStatus() {
        mfingerRotationX = 0;
        mfingerRotationY = 0;
        mfingerRotationZ = 0;
        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationZ, 0);
    }


    public void handleTouchDown(float x, float y) {
        this.mLastX = x;
        this.mLastY = y;
        this.gestureInertia_isStop = true;
    }

    public void handleTouchMove(float x, float y) {
        float offsetX = this.mLastX - x;
        float offsetY = this.mLastY - y;

        mShaderOffsetX = offsetX;

        this.mLastX = x;
        this.mLastY = y;
    }

    public void handleTouchUp(float x, float y, float xVelocity, float yVelocity) {
        this.mLastX = 0;
        this.mLastY = 0;
        mShaderOffsetX = 0;
        this.gestureInertia_isStop = false;
    }

}
