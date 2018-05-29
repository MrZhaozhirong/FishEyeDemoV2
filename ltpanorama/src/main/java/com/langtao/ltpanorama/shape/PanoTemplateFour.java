package com.langtao.ltpanorama.shape;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.IndexBuffer;
import com.langtao.ltpanorama.data.VertexBuffer;
import com.langtao.ltpanorama.program.PanoTemplateSphereShaderProgram;
import com.langtao.ltpanorama.utils.CameraViewport;
import com.langtao.ltpanorama.utils.MatrixHelper;
import com.langtao.ltpanorama.utils.TextureHelper;
import com.langtao.tmpanorama.PanoTemplateOut;
import com.langtao.tmpanorama.PanoTemplateProc;
import com.langtao.tmpanorama.PanoramaOut;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Created by zzr on 2017/12/4.
 */

public class PanoTemplateFour {
    private final static String TAG = "PanoTemplateBall";
    static{
        System.loadLibrary("panoTemplate");
        System.loadLibrary("LTPanoTemProc");
    }
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COORDIANTE_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COORDIANTE_COMPONENT_COUNT = 2;

    private PanoramaOut out;
    private VertexBuffer verticesBuffer;
    private VertexBuffer texCoordsBuffer;
    private IndexBuffer indicesBuffer;
    private PanoTemplateSphereShaderProgram pbShader;
    // 图像源 0:yuv  1:rgba , glsl中需要
    private int m_textureType = 0;
    private int numElements = 0;
    private PanoTemplateOut m_templateParam;
    private int m_Map1TextureID = 0;
    private int m_Map2TextureID = 0;
    private int m_WeightTextureID = 0;
    private boolean m_templateIsOK = false;
    private int[] _yuvTextureIDs;


    public PanoTemplateFour(){
        resetMatrixStatus();
        initCameraEye();
    }


    private boolean initTemplateConfigFile(String key, String templateFileName) {
        ByteBuffer dataBuffer = null;
        try
        {
            File configFile = new File(templateFileName);
            if(!configFile.exists()) {
                return false;
            }
            FileInputStream fis = new FileInputStream(new File(templateFileName));
            byte[] dataArray = new byte[fis.available()];
            fis.read(dataArray);

            m_templateParam = PanoTemplateProc.decryptTemplate(dataArray, key);
            if (m_templateParam == null) {
                return false;
            }

            Log.d(TAG, "DEBUG: templateFile length "+m_templateParam.panoTem.length);
            Log.d(TAG, "DEBUG: m_templateParam.width*height = "+m_templateParam.width+" x "+m_templateParam.height);
            Log.d(TAG, "DEBUG: m_templateParam width*height*4 length "+ m_templateParam.width * m_templateParam.height * 4);
            Log.d(TAG, "DEBUG: m_templateParam width*height*4*2 length "+ m_templateParam.width * m_templateParam.height * 4 * 2);
            if(m_templateParam.panoTem.length < m_templateParam.width * m_templateParam.height*4*2) {
                throw new IllegalArgumentException("Error: Panorama Template Config File Wrong !!!");
            }

            dataBuffer = ByteBuffer.allocateDirect(m_templateParam.panoTem.length )
                    .order(ByteOrder.nativeOrder());
            dataBuffer.put(m_templateParam.panoTem);
            dataBuffer.clear();
            loadTemplateTexture(dataBuffer);
            return true;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

    private void loadTemplateTexture(ByteBuffer dataBuffer) {
        if(dataBuffer == null) return;
        //加载map1
        final int[] _samplerMAP1Texture = new int[1];
        GLES20.glGenTextures(1, _samplerMAP1Texture, 0);
        m_Map1TextureID = _samplerMAP1Texture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_Map1TextureID);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        dataBuffer.clear();
        dataBuffer.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, m_templateParam.width, m_templateParam.height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                dataBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //加载map2
        final int[] _samplerMAP2Texture = new int[1];
        GLES20.glGenTextures(1, _samplerMAP2Texture, 0);
        m_Map2TextureID = _samplerMAP2Texture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_Map2TextureID);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        dataBuffer.clear();
        dataBuffer.position(m_templateParam.width * m_templateParam.height * 4);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, m_templateParam.width, m_templateParam.height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                dataBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //加载weight
        final int[] _samplerWeightTexture = new int[1];
        GLES20.glGenTextures(1, _samplerWeightTexture, 0);
        m_WeightTextureID = _samplerWeightTexture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_WeightTextureID);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        dataBuffer.clear();
        dataBuffer.position(m_templateParam.width * m_templateParam.height * 4 * 2);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, m_templateParam.width, m_templateParam.height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                dataBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        m_templateIsOK = true;
    }

    private void createBufferData() {
        if(out == null){
            try{
                out = PanoTemplateProc.panoramaSphereES();
            }catch ( Exception e){
                e.printStackTrace();
                return;
            }
        }

        verticesBuffer = new VertexBuffer(out.vertices);
        texCoordsBuffer = new VertexBuffer(out.texCoords);

        numElements = out.indices.length;
        indicesBuffer = new IndexBuffer(out.indices);
    }

    private void buildProgram() {
        pbShader = new PanoTemplateSphereShaderProgram();
    }

    private void setAttributeStatus() {
        if(verticesBuffer!=null)
        verticesBuffer.setVertexAttribPointer(pbShader.mPositionLoc,
                POSITION_COORDIANTE_COMPONENT_COUNT,
                POSITION_COORDIANTE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);
        if(texCoordsBuffer!=null)
        texCoordsBuffer.setVertexAttribPointer(pbShader.mTexCoordLoc,
                TEXTURE_COORDIANTE_COMPONENT_COUNT,
                TEXTURE_COORDIANTE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);
    }

    private void setShaderUniform() {
        GLES20.glUniform1i(pbShader.mTextureTypeLoc, m_textureType);
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
        //GLES20.glUniform1i(pbShader.mSamplerYLoc, 0);
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[1]);
        //GLES20.glUniform1i(pbShader.mSamplerULoc, 1);
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[2]);
        //GLES20.glUniform1i(pbShader.mSamplerVLoc, 2);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_Map1TextureID);
        GLES20.glUniform1i(pbShader.mSamplerMap1Loc, 3);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_Map2TextureID);
        GLES20.glUniform1i(pbShader.mSamplerMap2Loc, 4);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_WeightTextureID);
        GLES20.glUniform1i(pbShader.mSamplerWeightLoc, 5);
    }


    private float[] mProjectionMatrix = new float[16];// 4x4矩阵 存储投影矩阵
    private float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    private float[] mModelMatrix = new float[16];// 模型变换矩阵
    private float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵

    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }
    private void resetMatrixStatus() {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);
        Matrix.setIdentityM(this.mMVPMatrix, 0);

        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationZ, 0);
    }
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int initFrameWidth;
    private int initFrameHeight;
    public volatile boolean isInitialized = false;
    // 模板加密了
    public void onSurfaceCreated(String secretGIDStr, String templateFileName) {
        if( (templateFileName==null || "".equalsIgnoreCase(templateFileName) )
            &&
            (secretGIDStr==null || "".equalsIgnoreCase(secretGIDStr) )
                ) {
            Log.e(TAG, "Error: setPanoTemplateConfigFile param invalid !!!");
            Log.e(TAG, "Error: It will error show Panorama in LangTao-GL !!!");
            throw new IllegalArgumentException("Error: Panorama Template Config File is null or File not exists !!!");
        }else {
            if( initTemplateConfigFile(secretGIDStr, templateFileName) ){
                createBufferData();
                buildProgram();
                setAttributeStatus();
                this.isInitialized = true;
            }
        }
    }

    public void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0,0,width,height);
        GLES20.glClearColor(0.0f,0.0f,0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        mSurfaceWidth = width;
        mSurfaceHeight= height;
        float ratio = (float) width / (float) height;

        MatrixHelper.perspectiveM(this.mProjectionMatrix,
                currentOverture, ratio, 0.01f, 1000f);
        // 调用此方法产生摄像机9参数位置矩阵
        Matrix.setLookAtM(this.mViewMatrix,0,
                currentEye.cx,  currentEye.cy,  currentEye.cz,  //摄像机位置
                currentEye.tx,  currentEye.ty,  currentEye.tz,  //摄像机目标视点
                currentEye.upx, currentEye.upy, currentEye.upz);//摄像机头顶方向向量
    }

    public boolean updateTexture(YUVFrame yuvFrame) {
        if(yuvFrame == null || pbShader == null)
            return false;
        int width = yuvFrame.getWidth();
        int height = yuvFrame.getHeight();
        ByteBuffer yDatabuffer = yuvFrame.getYDataBuffer();
        ByteBuffer uDatabuffer = yuvFrame.getUDataBuffer();
        ByteBuffer vDatabuffer = yuvFrame.getVDataBuffer();
        if(yDatabuffer==null || uDatabuffer==null || vDatabuffer==null)
            return false;
        GLES20.glUseProgram( pbShader.getShaderProgramId() );
        //先去掉旧的纹理
        if(_yuvTextureIDs!=null){
            GLES20.glDeleteTextures(_yuvTextureIDs.length, _yuvTextureIDs, 0);
        }
        //重新加载数据
        int[] yuvTextureIDs = TextureHelper.loadYUVTexture2(width, height,
                yDatabuffer, uDatabuffer, vDatabuffer);
        _yuvTextureIDs = yuvTextureIDs;
        initFrameWidth = width;
        initFrameHeight = height;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
        GLES20.glUniform1i(pbShader.mSamplerYLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[1]);
        GLES20.glUniform1i(pbShader.mSamplerULoc, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[2]);
        GLES20.glUniform1i(pbShader.mSamplerVLoc, 2);
        return true;
    }

    public void draw(){
        if (!m_templateIsOK) {
            return;
        }
        GLES20.glUseProgram( pbShader.getShaderProgramId() );
        setShaderUniform();
        setAttributeStatus();

        if (isNeedAutoScroll) {
            autoRotated();
        }
        GLES20.glUniformMatrix4fv(pbShader.mMVPMatrixLoc, 1, false, getFinalMatrix(),0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_INT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void onDrawFrame(YUVFrame frame) {
        if(this.isInitialized ) {
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            // GLES20.glViewport(0, 0, mSurfaceWidth/2, mSurfaceHeight/2);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glCullFace(GLES20.GL_BACK);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            this.updateTexture(frame);
            GLES20.glViewport(0, mSurfaceHeight/2, mSurfaceWidth/2, mSurfaceHeight/2);
            this.updateBallMatrix(0f, 0f, 0f);
            this.draw();
            GLES20.glViewport(mSurfaceWidth/2, mSurfaceHeight/2, mSurfaceWidth/2, mSurfaceHeight/2);
            this.updateBallMatrix(0f, 90f, 0f);
            this.draw();
            GLES20.glViewport(0, 0, mSurfaceWidth/2, mSurfaceHeight/2);
            this.updateBallMatrix(0f, 180f, 0f);
            this.draw();
            GLES20.glViewport(mSurfaceWidth/2, 0, mSurfaceWidth/2, mSurfaceHeight/2);
            this.updateBallMatrix(0f, 270f, 0f);
            this.draw();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //================================模型变形相关=======================================================================
    private CameraViewport currentEye;
    private float currentOverture;
    private int currentControlMode = 0;
    // 水晶球参数
    private static final float CRYSTAL_OVERTURE = 70f;
    // 水晶球双指放大
    private float zoomTimes = 0.0f;
    private final static float CRYSTAL_SCALE_MAX_VALUE=1.1f;
    private final static float CRYSTAL_SCALE_MIN_VALUE=0.0f;
    // 小行星/鱼眼参数
    private static final float ASTEROID_MAX_OVERTURE = 140f;
    private static final float ASTEROID_MIN_OVERTURE= 70f;
    private static final float ASTEROID_COMMON_OVERTURE = 30f;


    private void initCameraEye() {
        if(currentEye==null)
            currentEye = new CameraViewport();

        // 初始化 鱼眼
        currentOverture = ASTEROID_MIN_OVERTURE;
        currentControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
        currentEye.setCameraVector(0, 0, -1.0f);
        currentEye.setTargetViewVector(0f, 0f, 0.0f);
        currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //================================模型操作相关=======================================================================

    private float mLastX;
    private float mLastY;
    private float mfingerRotationX = 0;
    private float mfingerRotationY = 0;
    private float mfingerRotationZ = 0;
    private float[] mMatrixFingerRotationX = new float[16];
    private float[] mMatrixFingerRotationY = new float[16];
    private float[] mMatrixFingerRotationZ = new float[16];
    //** 惯性自滚标志
    private volatile boolean gestureInertia_isStop_sync = true;


    private enum BallRollBoundaryDirection {
        TOP,
        BOTTOM,
        NORMAL
    } //** 纵角度限制相关
    private BallRollBoundaryDirection boundaryDirection = BallRollBoundaryDirection.NORMAL;
    private double moving_count_auto_return = 0.0f;

    public void updateBallMatrix() {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        if(this.mfingerRotationY < 0) {
            this.mfingerRotationY = this.mfingerRotationY % 360.0f + 360.0f; // 使他变成正数
        }
        if(this.mfingerRotationY > 360f ){
            this.mfingerRotationY = this.mfingerRotationY % 360f;
        }

        //if(this.mfingerRotationY > 360f || this.mfingerRotationY < -360f){
        //    this.mfingerRotationY = this.mfingerRotationY % 360f;
        //}
        Matrix.rotateM(this.mMatrixFingerRotationY, 0, this.mfingerRotationY, 0, 1, 0);
        Matrix.rotateM(this.mMatrixFingerRotationX, 0, this.mfingerRotationX, 1, 0, 0);
        Matrix.multiplyMM(this.mModelMatrix,0, this.mMatrixFingerRotationX,0, this.mMatrixFingerRotationY,0 );
    }

    private void updateBallMatrix(float mOffsetFingerRotationX,
                                  float mOffsetFingerRotationY,
                                  float mOffsetFingerRotationZ) {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationZ, 0);
        if(this.mfingerRotationY < 0) {
            this.mfingerRotationY = this.mfingerRotationY % 360.0f + 360.0f; // 使他变成正数
        }
        if(this.mfingerRotationY > 360f ){
            this.mfingerRotationY = this.mfingerRotationY % 360f;
        }
        Matrix.rotateM(this.mMatrixFingerRotationY, 0, this.mfingerRotationY+mOffsetFingerRotationY, 0, 1, 0);
        Matrix.rotateM(this.mMatrixFingerRotationX, 0, this.mfingerRotationX+mOffsetFingerRotationX, 1, 0, 0);

        Matrix.multiplyMM(this.mModelMatrix,0, this.mMatrixFingerRotationX,0, this.mMatrixFingerRotationY,0 );
    }
    /**
     * 手指离开屏幕 处理惯性滑动
     * @param x
     * @param y
     * @param xVelocity
     * @param yVelocity
     */
    public void handleTouchUp(final float x, final float y, final float xVelocity, final float yVelocity) {
        this.mLastX = 0;
        this.mLastY = 0;
        isOperating = false;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        handleGestureInertia(x,y, xVelocity, yVelocity);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    private void handleGestureInertia(float upX, float upY, float xVelocity, float yVelocity) throws InterruptedException {
        //因为是action_up的时候调用的，此时ball.mLast=0
        boolean isInertiaX = true;
        boolean isInertiaY = true;
        this.gestureInertia_isStop_sync = false;
        float mXVelocity = xVelocity;
        float mYVelocity = yVelocity;
        while(!this.gestureInertia_isStop_sync){
            isOperating = true;
//--------------------------------------------------------------------------------
            float offsetY = -mYVelocity / 2000;
            if(this.boundaryDirection == BallRollBoundaryDirection.NORMAL ) {
                this.mfingerRotationX = this.mfingerRotationX - offsetY;
                if(this.mfingerRotationX%360 > 90 ){
                    this.mfingerRotationX = 90;
                }else
                if(this.mfingerRotationX%360 < -90 ) {
                    this.mfingerRotationX = -90;
                }
            } else if(this.boundaryDirection == BallRollBoundaryDirection.BOTTOM ){
                double temp = seriesMoveReturn(this.mfingerRotationX);
                this.mfingerRotationX -= temp;
            } else if(this.boundaryDirection == BallRollBoundaryDirection.TOP ){
                double temp = seriesMoveReturn(this.mfingerRotationX);
                this.mfingerRotationX += temp;
            } else{
            }

            if(isInertiaX){
                float offsetX = -mXVelocity / 2000;
                this.mfingerRotationY = this.mfingerRotationY + offsetX;
            }
            updateBallBoundary();
            if(false) {
                Log.i(TAG,"Inertia mXVelocity : "+mXVelocity);
                Log.i(TAG,"Inertia mYVelocity : "+mYVelocity);
                Log.i(TAG,"Inertia ball.mfingerRotationX : "+this.mfingerRotationX);
                Log.i(TAG,"Inertia ball.mfingerRotationY : "+this.mfingerRotationY);
                Log.i(TAG,"//------------------------------------------");
            }
//--------------------------------------------------------------------------------
            if(Math.abs(mYVelocity - 0.97f*mYVelocity) < 0.00001f
                    || Math.abs(mXVelocity - 0.97f*mXVelocity) < 0.00001f){
                if(this.boundaryDirection == BallRollBoundaryDirection.NORMAL){
                    this.gestureInertia_isStop_sync = true;
                }
            }
            mYVelocity = 0.975f*mYVelocity;
            mXVelocity = 0.975f*mXVelocity;
            Thread.sleep(5);
        }
        isOperating = false;
    }

    private double seriesMoveReturn(float mFingerRotation){
        //mFingerRotation > 0 = BOTTOM
        //mFingerRotation < 0 = TOP
        float absRotation = Math.abs(mFingerRotation);
        this.moving_count_auto_return = absRotation - 90;
        this.moving_count_auto_return = (float) (Math.sqrt(Math.pow(this.moving_count_auto_return, 2.0)) / 15.0);
        if(this.moving_count_auto_return <= 0.000015){
            this.moving_count_auto_return = 0.000015;
        }
        //Log.d(TAG, "moving_count_auto_return : "+this.moving_count_auto_return);
        return this.moving_count_auto_return;
    }

    /**
     * 单指操作触碰事件
     * @param x
     * @param y
     */
    public void handleTouchDown(float x, float y) {
        this.gestureInertia_isStop_sync = true;
        this.mLastX = x;
        this.mLastY = y;
        isOperating = true;
    }

    /**
     * 滑动 增加上下限限制
     * @param x
     * @param y
     */
    public void handleTouchMove(float x, float y) {
        this.gestureInertia_isStop_sync = true;
        float offsetX = this.mLastX - x;
        float offsetY = this.mLastY - y;
        if(this.mfingerRotationX%360 > 90 ){
            float temp = seriesDampDrag(this.mfingerRotationX, offsetY/2);
            //Log.w(TAG,"ball.mfingerRotationX offset : "+temp);
            this.mfingerRotationX -= temp;
        }else
        if(this.mfingerRotationX%360 < -90 ) {
            float temp = seriesDampDrag(this.mfingerRotationX, offsetY/2);
            //Log.w(TAG,"ball.mfingerRotationX offset : "+temp);
            this.mfingerRotationX -= temp;
        }else
        {
            this.mfingerRotationX -= offsetY/10 ;
        }

        this.mfingerRotationY += offsetX/8 ;

        updateBallBoundary();

        this.mLastX = x;
        this.mLastY = y;
    }

    private void updateBallBoundary() {
        if(this.mfingerRotationX > 90) {
            this.boundaryDirection = BallRollBoundaryDirection.BOTTOM;
        }else if(this.mfingerRotationX < -90){
            this.boundaryDirection = BallRollBoundaryDirection.TOP;
        }else{
            this.boundaryDirection = BallRollBoundaryDirection.NORMAL;
        }
    }

    private float seriesDampDrag(float mfingerRotationX, float offset) {
        float absRotation = Math.abs(mfingerRotationX);
        float level = absRotation - 90;
        if(mfingerRotationX * offset> 0){
            //Log.w(TAG, "反方向往回滚");
            if(level < 10){
                //   90°~100°
                return offset * 0.8f;
            }else if(level < 20){
                //   100°~110°
                return offset * 0.6f;
            }else if(level < 30){
                //   110°~120°
                return offset * 0.4f;
            }else{
                //   >120°
                return offset * 0.2f;
            }
        }
        else
        {
            //Log.w(TAG, "同方向一直拖着");
            if(level < 10){
                //   90°~100°
                return offset * 0.5f;
            }else if(level < 20){
                //   100°~110°
                return offset * 0.3f;
            }else if(level < 30){
                //   110°~120°
                return offset * 0.1f;
            }else{
                //   >120°
                return offset * 0.1f;
            }
        }
    }

    /**
     * 双击操作
     */
    public void handleDoubleClick() {

    }

    /**
     * 双手操作
     * @param distance
     */
    public void handleMultiTouch(float distance) {
        switch (currentControlMode){
            case LTRenderMode.RENDER_MODE_CRYSTAL:
                crystal_MultiTouch(distance);
                break;
            case LTRenderMode.RENDER_MODE_FISHEYE:
                fisheye_MultiTouch(distance);
                break;
            case LTRenderMode.RENDER_MODE_PLANET:
                planet_MultiTouch(distance);
                break;
        }
    }

    private void planet_MultiTouch(float distance) {
        float dis = distance / 10;
        float scale;
        if(dis > 0 ){
            scale = -2.0f;
        }else{
            scale = 2.0f;
        }
        currentOverture += scale;
        if(currentOverture < ASTEROID_MAX_OVERTURE-20){
            currentOverture = ASTEROID_MAX_OVERTURE-20;
        }
        if(currentOverture > ASTEROID_MAX_OVERTURE){
            currentOverture = ASTEROID_MAX_OVERTURE;
        }
        float ratio = (float)mSurfaceWidth / (float)mSurfaceHeight;
        MatrixHelper.perspectiveM(this.mProjectionMatrix,
                currentOverture, ratio, 0.01f, 1000f);
    }

    private void fisheye_MultiTouch(float distance) {
        float dis = distance / 10;
        float scale;
        if(dis > 0 ){
            scale = -2.0f;
        }else{
            scale = 2.0f;
        }
        float temp = currentOverture + scale;
        if(temp < ASTEROID_COMMON_OVERTURE){
            currentOverture = ASTEROID_COMMON_OVERTURE;
        }else if(temp > ASTEROID_MAX_OVERTURE-10){
            currentOverture = ASTEROID_MAX_OVERTURE-10;
        }else {
            currentOverture = temp;
        }
        Log.w(TAG, "currentOverture : "+currentOverture);
        float ratio = (float)mSurfaceWidth / (float)mSurfaceHeight;
        MatrixHelper.perspectiveM(this.mProjectionMatrix,
                currentOverture, ratio, 0.01f, 1000f);
    }

    private void crystal_MultiTouch(float distance) {
        float dis = distance / 50;
        float scale;
        if(dis < 0 ){
            scale = -0.1f;
            this.zoomTimes -= 0.1;
        }else{
            scale = 0.1f;
            this.zoomTimes += 0.1;
        }
        if(this.zoomTimes > CRYSTAL_SCALE_MAX_VALUE){
            scale = 0.0f;
            this.zoomTimes = CRYSTAL_SCALE_MAX_VALUE;
        }
        if(this.zoomTimes < CRYSTAL_SCALE_MIN_VALUE){
            scale = 0.0f;
            this.zoomTimes = CRYSTAL_SCALE_MIN_VALUE;
        }
        //在原本的基础上添加增值，不需要置零
        Matrix.translateM(this.mViewMatrix,0, 0f,0f,-scale);
        if(this.mViewMatrix[14] < -1.0f) {
            currentEye.setCameraVector(currentEye.cx, currentEye.cy, this.mViewMatrix[14]);
        } else {
            currentEye.setCameraVector(currentEye.cx, currentEye.cy, -1.0f);
        }


        Log.w(TAG, "currentOverture : "+currentOverture);
        Log.w(TAG, "current mViewMatrix: " + "\n" +
                currentEye.cx + " " +  currentEye.cy + " " +  currentEye.cz + "\n" +
                currentEye.tx + " " +  currentEye.ty + " " +  currentEye.tz + "\n" +
                currentEye.upx + " " + currentEye.upy + " " + currentEye.upz + "\n");
        Log.w(TAG, "=========================  " + "\n");
    }



    //自动旋转相关
    private boolean isNeedAutoScroll = false;
    private int direction = 1;
    private volatile boolean isOperating = false;
    public void setAutoCruise(boolean autoCruise) {
        this.isNeedAutoScroll = autoCruise;
    }

    public void setCruiseDirection(int direction) {
        this.direction = direction;
    }

    private void autoRotated() {
        if (isOperating) return;
        if(direction == 0)
            this.mfingerRotationY -= 0.1f;
        else
            this.mfingerRotationY += 0.1f;

        if (this.mfingerRotationY > 360 || this.mfingerRotationY < -360) {
            this.mfingerRotationY = this.mfingerRotationY % 360;
        }
    }

}

