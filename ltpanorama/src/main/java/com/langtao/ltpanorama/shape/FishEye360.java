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
import com.langtao.ltpanorama.utils.MatrixHelper;
import com.langtao.ltpanorama.utils.TextureHelper;

import java.nio.ByteBuffer;

/**
 * Created by zzr on 2017/8/16.
 */

public class FishEye360 {

    static {
        System.loadLibrary("one_fisheye");
        System.loadLibrary("LTFishEyeProc");
    }

    private static final String TAG = "OneFishEye360";
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

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mFrameWidth;
    private int mFrameHeight;
    private CameraViewport eye;

    public FishEye360( ) {
        resetMatrixStatus();

        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);

        eye = new CameraViewport();

        eye.setCameraVector(0, 0, -2.6f);
        eye.setTargetViewVector(0f, 0f, 0.0f);
        eye.setCameraUpVector(0f, 1.0f, 0.0f);

        //if(timer==null)
        //    timer = new Timer();
        //if(autoScrollTimerTask==null) {
        //    autoScrollTimerTask = new TimerTask() {
        //        @Override
        //        public void run() {
        //            isNeedAutoScroll = true;
        //            operating = false;
        //        }
        //    };
        //}
        //timer.schedule(autoScrollTimerTask, 5000, 10000);
    }

    //================================建模视频帧相关==============================================================
    //================================建模视频帧相关==============================================================
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COORDINATE_COMPONENT_COUNT = 3; // 每个顶点的坐标数 x y z
    private static final int TEXTURE_COORDINATE_COMPONENT_COUNT = 2; // 每个纹理坐标为 S T两个
    public volatile boolean isInitialized = false;
    private OneFisheyeOut out;
    private VertexBuffer verticesBuffer;
    private VertexBuffer texCoordsBuffer;
    private IndexBuffer indicesBuffer;
    private int numElements = 0;
    private int drawElementType;
    private OneFishEye360ShaderProgram fishShader;
    private int[] _yuvTextureIDs = new int[]{0};

    public void onSurfaceCreate(String previewPicPathName,int[] previewPicData){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   //指定需要的是原始数据，非压缩数据
        Bitmap bitmap = BitmapFactory.decodeFile(previewPicPathName, options);
        if(bitmap == null){
            throw new IllegalStateException("previewPicPathName not load in bitmap!");
        }

        createBufferData(bitmap.getWidth(),bitmap.getHeight(), previewPicData);
        buildProgram();
        initTexture(bitmap);
        setAttributeStatus();
        isInitialized = true;
        bitmap.recycle();
        bitmap = null;
    }


    public void onSurfaceCreate(@NonNull YUVFrame frame){
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
                (float) overture, ratio, 0.1f, 100f);

        Matrix.setLookAtM(this.mViewMatrix, 0,
                0, 0, -2.6f, //摄像机位置
                0f, 0f, 0.0f, //摄像机目标视点
                0f, 1.0f, 0.0f);//摄像机头顶方向向量
    }

    private void createBufferData(int width, int height, int[] previewPicRawData) {
        if (out == null) {
            try {
                OneFisheye360Param outParam = new OneFisheye360Param();
                Log.w(TAG, "OneFisheye360Param rgb width&height : " + width+ "  " + height);
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
        if(fishShader ==null) return false;
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
        if(fishShader ==null) return false;
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
        if(fishShader ==null) return ;
        GLES20.glUseProgram(fishShader.getShaderProgramId());

        float kColorConversion420[] = {
                1.0f, 1.0f, 1.0f,
                0.0f, -0.39465f, 2.03211f,
                1.13983f, -0.58060f, 0.0f
        };

        GLES20.glUniformMatrix3fv(fishShader.uLocationCCM, 1, false, kColorConversion420, 0);
        if (verticesBuffer == null||texCoordsBuffer==null) {
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
        if(!isInitialized) return;
        GLES20.glViewport(0,0,mSurfaceWidth,mSurfaceHeight);
        GLES20.glClearColor(0.0f,0.0f,0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        GLES20.glUseProgram( fishShader.getShaderProgramId() );
        if(frame!=null){
            updateTexture(frame);
            GLES20.glUniform1i(fishShader.uLocationImageMode, 0);
        }else{
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
            GLES20.glUniform1i(fishShader.uLocationSamplerRGB, 0);
            GLES20.glUniform1i(fishShader.uLocationImageMode, 1);
        }

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

        GLES20.glUseProgram( fishShader.getShaderProgramId() );
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

    private void updateBallMatrix() {

        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.scaleM(this.mModelMatrix, 0, 1.0f, 1.0f, 1.0f);

        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationZ, 0);
        Matrix.rotateM(this.mMatrixFingerRotationZ, 0, this.mfingerRotationX, 0, 0, 1);
        Matrix.rotateM(this.mMatrixFingerRotationX, 0, this.mfingerRotationY, 1, 0, 0);
        Matrix.multiplyMM(this.mModelMatrix, 0, this.mMatrixFingerRotationX, 0, this.mMatrixFingerRotationZ, 0);
    }

    public void draw() {
        if(fishShader ==null) return ;
        GLES20.glUseProgram( fishShader.getShaderProgramId() );
        //将最终变换矩阵写入
        GLES20.glUniformMatrix4fv(fishShader.uMVPMatrixLocation, 1, false, getFinalMatrix(), 0);
		if(indicesBuffer==null){
			return;
		}
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, drawElementType, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }


    //================================操作封装==================================================================
    //================================模型操作相关==============================================================
    private float mLastX;
    private float mLastY;
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

    private volatile boolean gestureInertia_isStop = true;
    private volatile boolean pullupInertia_isStop = true;
    //自动旋转相关
    private volatile boolean isNeedAutoScroll = false;
    private volatile int direction = 0;
    private volatile boolean operating = false;
    //private Timer timer;
    //private TimerTask autoScrollTimerTask = new TimerTask() {
    //    @Override
    //    public void run() {
    //        isNeedAutoScroll = true;
    //        operating = false;
    //    }
    //};

    private void autoRotated() {
        if (operating) return;
        if(direction == 0)
            this.mfingerRotationX -= 0.2f;
        else
            this.mfingerRotationX += 0.2f;

        if (this.mfingerRotationX > 360 || this.mfingerRotationX < -360) {
            this.mfingerRotationX = this.mfingerRotationX % 360;
        }
    }


    public void handleTouchDown(float x, float y) {
        this.mLastX = x;
        this.mLastY = y;
        this.gestureInertia_isStop = true;
        operating = true;
        //if (timer != null) {
        //    timer.purge();
        //}
    }

    public void handleTouchUp(final float x, final float y,
                              final float xVelocity, final float yVelocity) {
        this.mLastX = 0;
        this.mLastY = 0;
        this.gestureInertia_isStop = false;

        if (this.mfingerRotationY > UPPER_VISION_VALUE) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        endoscopeBoundaryInertia(x, y, xVelocity, yVelocity);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

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

    private void endoscopeBoundaryInertia(float x, float y, float xVelocity, float yVelocity) throws
            InterruptedException {
        pullupInertia_isStop = false;
        while (!this.pullupInertia_isStop) {
            operating = true;
            this.mfingerRotationY -= 0.1f;
            if (this.mfingerRotationY < UPPER_VISION_VALUE) {
                pullupInertia_isStop = true;
                this.mfingerRotationY = UPPER_VISION_VALUE;
            }
            Thread.sleep(5);
        }
        operating = false;
    }

    private void handleGestureInertia(float x, float y, float xVelocity, float yVelocity)
            throws InterruptedException {

        this.gestureInertia_isStop = false;
        float mXVelocity = xVelocity / 8000f;
        float mYVelocity = yVelocity / 8000f;
        //Log.w(TAG,"xVelocity : "+xVelocity);
        while (!this.gestureInertia_isStop) {
            double offsetX = -mXVelocity;
            this.mfingerRotationX -= offsetX;
            //----------------------------------------------------------------------------
            if (Math.abs(mXVelocity - 0.995f * mXVelocity) < 0.00000001f) {
                if (this.pullupInertia_isStop) {
                    this.gestureInertia_isStop = true;
                }
            }
            mYVelocity = 0.995f * mYVelocity;
            mXVelocity = 0.995f * mXVelocity;
            Thread.sleep(2);
            operating = true;
        }
        operating = false;
    }

    private static int MODE_OVER_LOOK = 0;
    private static int MODE_ENDOSCOPE = 1;
    private int currentPerspectiveMode = MODE_OVER_LOOK;
    private final float DEFAULT_VISION_VALUE = 20f;
    private float UPPER_VISION_VALUE = DEFAULT_VISION_VALUE;
    private float LOWER_VISION_VALUE = -DEFAULT_VISION_VALUE;

    public void handleTouchMove(float x, float y) {
        float offsetX = this.mLastX - x;
        float offsetY = this.mLastY - y;
        this.mfingerRotationX -= offsetX / 10;
        this.mfingerRotationY -= offsetY / 10;

        //if (currentPerspectiveMode == MODE_ENDOSCOPE) {
        //    if (this.mfingerRotationY > 70f) {
        //        this.mfingerRotationY = 70f;
        //    }
        //    if (this.mfingerRotationY < 20f) {
        //        this.mfingerRotationY = 20f;
        //    }
        //} else {  //currentPerspectiveMode == CameraViewport.MODE_OVER_LOOK
        //    if (this.mfingerRotationY > 20f) {
        //        this.mfingerRotationY = 20f;
        //    }
        //    if (this.mfingerRotationY < -20f) {
        //        this.mfingerRotationY = -20f;
        //    }
        //}
        if (this.mfingerRotationY > UPPER_VISION_VALUE) {
            this.mfingerRotationY = UPPER_VISION_VALUE;
        }
        if (this.mfingerRotationY < LOWER_VISION_VALUE) {
            this.mfingerRotationY = LOWER_VISION_VALUE;
        }
        //Log.w(TAG, "currentPerspectiveMode : " + currentPerspectiveMode);
        //Log.w(TAG, "mfingerRotationY : " + this.mfingerRotationY);
        this.mLastX = x;
        this.mLastY = y;
    }



    private float zoomTimes = 0.0f;  //放大缩小

    public void handleDoubleClick() {
        //把放大缩小还原
        this.zoomTimes = 0;
        operating = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean transforming = true;
                boolean bIsNeedAutoScroll = isNeedAutoScroll;
                while (transforming) {
                    try {
                        Thread.sleep(1);
                        if (currentPerspectiveMode == MODE_ENDOSCOPE) {
                            transforming = transformToOverlook();
                        } else if (currentPerspectiveMode == MODE_OVER_LOOK) {
                            transforming = transformToEndoscope();
                        }
                        isNeedAutoScroll = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Log.w(TAG, "current mViewMatrix: " + "\n" +
                        eye.cx + " " + eye.cy + " " + eye.cz + "\n" +
                        eye.tx + " " + eye.ty + " " + eye.tz + "\n" +
                        eye.upx + " " + eye.upy + " " + eye.upz + "\n");
                updateVisionValue(eye.cz+3);
                Log.w(TAG, "currentPerspectiveMode : "+currentPerspectiveMode);
                isNeedAutoScroll = bIsNeedAutoScroll;
                operating = false;
            }
        }).start();
    }

    private boolean transformToOverlook() {
        boolean viewTransforming = true;
        if (eye.cz > -2.500001f) {
            Matrix.setLookAtM(mViewMatrix, 0,
                    eye.cx, eye.cy, eye.cz -= 0.002f,
                    eye.tx, eye.ty, eye.tz,
                    eye.upx, eye.upy, eye.upz);
        } else {
            viewTransforming = false;
        }

        boolean modelTransforming = true;
        if (this.mfingerRotationY > 0) {
            this.mfingerRotationY -= 0.04f;
        } else {
            modelTransforming = false;
        }
        this.mfingerRotationX -= 0.05f;

        if (viewTransforming || modelTransforming) {
            return true;
        } else {
            currentPerspectiveMode = MODE_OVER_LOOK;
            return false;
        }
    }

    private boolean transformToEndoscope() {
        boolean viewTransforming = true;
        if (eye.cz < -1.000f) {
            Matrix.setLookAtM(mViewMatrix, 0,
                    eye.cx, eye.cy, eye.cz += 0.002f,
                    eye.tx, eye.ty, eye.tz,
                    eye.upx, eye.upy, eye.upz);
        } else {
            viewTransforming = false;
        }

        boolean modelTransforming = true;
        if (this.mfingerRotationY < 35f) {
            this.mfingerRotationY += 0.04f;
        } else {
            modelTransforming = false;
        }

        this.mfingerRotationX -= 0.05f;

        if (viewTransforming || modelTransforming) {
            return true;
        } else {
            currentPerspectiveMode = MODE_ENDOSCOPE;
            return false;
        }
    }

    public void handleMultiTouch(float distance) {
        Log.w(TAG, "eye.cz current : "+eye.cz);

        float dis = distance / 10;
        float scale;
        if (dis < 0) {
            if(eye.cz > -2.6f) {
                // 还没到俯视最值，还能放大视野，缩小视图
                scale = -0.1f;
                this.zoomTimes -= 0.1;
            } else {
                return;
            }
        } else {
            if(eye.cz < 0.001f){
                // 还没到内窥最值，还能缩小视野，放大视图
                scale = 0.1f;
                this.zoomTimes += 0.1;
            } else {
                return;
            }
        }
        Log.w(TAG, "handleMultiTouch zoomTimes : "+zoomTimes);

        Matrix.translateM(this.mViewMatrix, 0, 0f, 0f, -scale);
        eye.setCameraVector(eye.cx, eye.cy, this.mViewMatrix[14]);
        Log.w(TAG, "current CameraEye : " + "\n" +
                eye.cx + " " + eye.cy + " " + eye.cz + "\n" +
                eye.tx + " " + eye.ty + " " + eye.tz + "\n" +
                eye.upx + " " + eye.upy + " " + eye.upz + "\n");
        if(eye.cz > 0.001f ) {
            // 内窥
            currentPerspectiveMode = MODE_ENDOSCOPE;
        }
        if(eye.cz < -2.6f ) {
            // 俯视
            currentPerspectiveMode = MODE_OVER_LOOK;
        }
        Log.w(TAG, "currentPerspectiveMode : "+currentPerspectiveMode);
        updateVisionValue(eye.cz+3);
    }

    private void updateVisionValue(float currentEyeZ) {
        UPPER_VISION_VALUE = (currentEyeZ+0.8f)* DEFAULT_VISION_VALUE;
        if(Math.abs(currentEyeZ-0.01f) < 0.001f ){
            UPPER_VISION_VALUE = (currentEyeZ+1f)* DEFAULT_VISION_VALUE;
            LOWER_VISION_VALUE = -(currentEyeZ+1f)* DEFAULT_VISION_VALUE;
        } else {
            LOWER_VISION_VALUE = 0;
        }
    }


    public void setAutoCruise(boolean autoCruise) {
        this.isNeedAutoScroll = autoCruise;
    }

    public void setCruiseDirection(int direction) {
        this.direction = direction;
    }

}
