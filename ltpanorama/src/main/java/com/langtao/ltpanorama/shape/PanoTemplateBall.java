package com.langtao.ltpanorama.shape;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
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

public class PanoTemplateBall {
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
    // 记录要画多少个三角形
    private int numElements = 0;
    // 模板参数
    private PanoTemplateOut m_templateParam;
    private int m_Map1TextureID = 0;
    private int m_Map2TextureID = 0;
    private int m_WeightTextureID = 0;
    private boolean m_templateIsOK = false;
    private int[] _yuvTextureIDs;
    private int render_mode;

    public PanoTemplateBall(int render_mode){
        this.render_mode = render_mode;
        resetMatrixStatus();

        if(currentEye==null)
            currentEye = new CameraViewport();
        if(targetEye==null)
            targetEye = new CameraViewport();
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

    //private boolean initTemplateConfigFile(String templateFileName) {
    //    ByteBuffer dataBuffer = null;
    //    try
    //    {
    //        File configFile = new File(templateFileName);
    //        if(!configFile.exists()) {
    //            return false;
    //        }
    //        FileInputStream fis = new FileInputStream(new File(templateFileName));
    //        byte[] dataArray = new byte[fis.available()];
    //        Log.d(TAG, "DEBUG: templateFile length "+dataArray.length);
    //        Log.d(TAG, "DEBUG: m_templateParam.width*height = "+m_templateParam.width+" x "+m_templateParam.height);
    //        Log.d(TAG, "DEBUG: m_templateParam width*height*4 length "+ m_templateParam.width * m_templateParam.height * 4);
    //        Log.d(TAG, "DEBUG: m_templateParam width*height*4*2 length "+ m_templateParam.width * m_templateParam.height * 4 * 2);
    //        if(dataArray.length < m_templateParam.width * m_templateParam.height*4*2) {
    //            throw new IllegalArgumentException("Error: Panorama Template Config File Wrong !!!");
    //        }
    //        fis.read(dataArray);
    //        dataBuffer = ByteBuffer.allocateDirect(dataArray.length)
    //                .order(ByteOrder.nativeOrder());
    //        dataBuffer.put(dataArray);
    //        dataBuffer.clear();
    //        loadTemplateTexture(dataBuffer);
    //        return true;
    //    }catch (Exception ex){
    //        ex.printStackTrace();
    //    }
    //    return false;
    //}

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
        //GLES20.glUseProgram( pbShader.getShaderProgramId() );
    }

    private boolean initTexture(@NonNull YUVFrame frame) {
        //_yuvTextureIDs = TextureHelper.loadYUVTexture(context, R.raw.pic_20171121180105_14335961, 1920, 1080);
        if(frame == null) return false;
        _yuvTextureIDs = TextureHelper.loadYUVTexture2(frame.getWidth(),frame.getHeight(),
                frame.getYDataBuffer(),frame.getUDataBuffer(),frame.getVDataBuffer() );
        initFrameWidth = frame.getWidth();
        initFrameHeight= frame.getHeight();
        if(_yuvTextureIDs == null || _yuvTextureIDs.length != 3) {
            Log.w(TAG,"_yuvTextureIDs object's length not equals 3 !");
            return false;
        }
        return true;
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
        //GLES20.glUniform1i(pbShader.mSamplerRGBLoc, 6);
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
    private volatile boolean isAnimation = false;
    // 模板加密了
    public void onSurfaceCreated(String secretGIDStr, String templateFileName,
                                 boolean isStartBootAnimation) {
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
                //initTexture(frame);
                setAttributeStatus();
                this.isInitialized = true;

                if( isStartBootAnimation) {
                    initCameraEye(render_mode);
                    mfingerRotationX = -70f;    // ->0
                    updatingBallControlMode=true;
                    new bootAnimationWaitThread().start();
                } else {
                    setRenderMode(render_mode);
                    updatingBallControlMode=false;
                    onSurfaceChanged(mSurfaceWidth, mSurfaceHeight);
                }
            }
        }
    }

    // 模板没加密
    //public void onSurfaceCreated(String templateFileName) {
    //    // 获取模板参数
    //    m_templateParam = PanoTemplateProc.getPanoTemplateSize();
    //    if(templateFileName==null ||
    //            "".equalsIgnoreCase(templateFileName) ||
    //            !new File(templateFileName).exists() ) {
    //        Log.e(TAG, "Error: setPanoTemplateConfigFile is null or File not exists !!!");
    //        Log.e(TAG, "Error: It will error show Panorama in LangTao-GL !!!");
    //        throw new IllegalArgumentException("Error: Panorama Template Config File is null or File not exists !!!");
    //    }else {
    //        if( initTemplateConfigFile(templateFileName) ){
    //            createBufferData();
    //            buildProgram();
    //            //initTexture(frame);
    //            setAttributeStatus();
    //            this.isInitialized = true;
    //        }
    //    }
    //}

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
        //if(width != initFrameWidth || height!= initFrameHeight)
        {
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
        }
        //else
        //{//长宽没变，更新纹理，不重建
        //    TextureHelper.updateTexture2(_yuvTextureIDs[0], width, height, yDatabuffer);
        //    TextureHelper.updateTexture2(_yuvTextureIDs[1], width, height, uDatabuffer);
        //    TextureHelper.updateTexture2(_yuvTextureIDs[2], width, height, vDatabuffer);
        //}
        //重新加载纹理
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
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram( pbShader.getShaderProgramId() );
        setShaderUniform();
        setAttributeStatus();

        if(targetControlMode == LTRenderMode.RENDER_MODE_VR){
            float ratioVR =  (float)mSurfaceWidth /2.0f / (float)mSurfaceHeight;
            MatrixHelper.perspectiveM(this.mProjectionMatrix, currentOverture, ratioVR, 0.01f, 1000f);
            Matrix.setLookAtM(this.mViewMatrix,0,
                    currentEye.cx,  currentEye.cy,  currentEye.cz, //摄像机位置
                    currentEye.tx,  currentEye.ty,  currentEye.tz, //摄像机目标视点
                    currentEye.upx, currentEye.upy, currentEye.upz);//摄像机头顶方向向量
            // /////////////左边屏幕
            GLES20.glViewport(0, 0, (mSurfaceWidth/2), mSurfaceHeight);
            GLES20.glUniformMatrix4fv(pbShader.mMVPMatrixLoc, 1, false, getFinalMatrix(),0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_INT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

            // /////////////右边屏幕
            float tempFingerRotationX = this.mfingerRotationX + 0.15f;
            Matrix.setIdentityM(this.mModelMatrix, 0);
            Matrix.scaleM(this.mModelMatrix, 0, 1.0f, 1.0f, 1.0f);
            Matrix.setIdentityM(this.mModelMatrix, 0);
            Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
            Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
            Matrix.rotateM(this.mMatrixFingerRotationY, 0, this.mfingerRotationY, 0, 1, 0);
            Matrix.rotateM(this.mMatrixFingerRotationX, 0, tempFingerRotationX, 1, 0, 0);
            Matrix.multiplyMM(this.mModelMatrix,0, this.mMatrixFingerRotationX,0, this.mMatrixFingerRotationY,0 );

            GLES20.glViewport((mSurfaceWidth/2), 0, (mSurfaceWidth/2), mSurfaceHeight);
            GLES20.glUniformMatrix4fv(pbShader.mMVPMatrixLoc, 1, false, getFinalMatrix(),0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_INT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            if (isNeedAutoScroll) {
                autoRotated();
            }
            GLES20.glUniformMatrix4fv(pbShader.mMVPMatrixLoc, 1, false, getFinalMatrix(),0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_INT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    public void onDrawFrame(YUVFrame frame) {
        if(this.isInitialized && this.isAnimation) {
            this.updateBallControlMode();
            //不能放在frame！=null 因为buffer==null会造成状态切换动画不流畅
        }
        this.updateTexture(frame);
        this.updateBallMatrix();
        this.draw();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //================================模型变形相关=======================================================================
    private CameraViewport currentEye;
    private CameraViewport targetEye;
    private float currentOverture;
    private float targetOverture;
    private int currentControlMode = 0;
    private int targetControlMode = 0;
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
    // VR参数
    private static final float VR_OVERTURE = 90f;

    private void initCameraEye(int RENDER_MODE) {
        if(currentEye==null)
            currentEye = new CameraViewport();
        if(targetEye==null)
            targetEye = new CameraViewport();

        if(RENDER_MODE != LTRenderMode.RENDER_MODE_VR){
            //targetOverture = currentOverture = CRYSTAL_OVERTURE;
            //targetControlMode = currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
            //currentEye.setCameraVector(0, 0, -1.9f);
            //currentEye.setTargetViewVector(0f, 0f, 0.0f);
            //currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
            //currentEye.copyTo(targetEye);

            //2018.4.16 默认启动特效 从小行星 -> 转变成全景球
            updatingBallControlMode=true;
            // 初始化 小行星
            currentOverture = ASTEROID_MAX_OVERTURE;
            currentControlMode = LTRenderMode.RENDER_MODE_PLANET;
            currentEye.setCameraVector(0, 0, -1.0f);
            currentEye.setTargetViewVector(0f, 0f, 0.0f);
            currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
            // 目标 全景球
            targetOverture = CRYSTAL_OVERTURE;
            targetControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
            targetEye.setCameraVector(0, 0, -1.9f);
            targetEye.setTargetViewVector(0f, 0f, 0.0f);
            targetEye.setCameraUpVector(0f, 1.0f, 0.0f);
        } else {
            currentControlMode = targetControlMode = LTRenderMode.RENDER_MODE_VR;
            currentOverture = targetOverture = VR_OVERTURE;
            currentEye.setCameraVector(0, 0, 0.0f);
            currentEye.setTargetViewVector(0f, 0f, 1.0f);
            currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
            currentEye.copyTo(targetEye);
        }
    }



    private float calculateDist(float current, float target, float divisor) {
        if(divisor==0) return 0;
        float absCurrent = Math.abs(current);
        float absTarget = Math.abs(target);
        float diff = Math.abs(absCurrent - absTarget);
        float dist = (float) (Math.sqrt(Math.pow(diff, 2.0)) / divisor);
        return Math.abs(dist);
    }

    public int nextControlMode() {
        if(updatingBallControlMode)
            return targetControlMode;
        this.gestureInertia_isStop_sync = true;
        this.isAnimation = true;
        // 把惯性线程停掉
        if(currentControlMode == LTRenderMode.RENDER_MODE_CRYSTAL){
            targetOverture = ASTEROID_MIN_OVERTURE;
            targetEye.setCameraVector(0, 0, -1.0f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
        }

        if(currentControlMode == LTRenderMode.RENDER_MODE_FISHEYE){
            targetOverture = ASTEROID_MAX_OVERTURE;
            targetEye.setCameraVector(0, 0, -1.0f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_PLANET;
        }

        if(currentControlMode == LTRenderMode.RENDER_MODE_PLANET){
            targetOverture = CRYSTAL_OVERTURE;
            targetEye.setCameraVector(0, 0, -1.9f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
            deviationFromPlanetToCrystal = 10.0f;
        }
        return targetControlMode;
    }

    private volatile boolean updatingBallControlMode = false;
    private volatile float deviationFromPlanetToCrystal = 5.0f;

    public void updateBallControlMode() {
        if(currentControlMode != targetControlMode){
            updatingBallControlMode = true;
            isOperating = true;
            // 2018 01 26 新增鱼眼双击还原到水晶球
            if(currentControlMode == LTRenderMode.RENDER_MODE_FISHEYE &&
                    targetControlMode == LTRenderMode.RENDER_MODE_CRYSTAL){
                if( Math.abs(currentOverture-targetOverture) > 0.1f){
                    float diff = Math.abs(targetOverture - currentOverture) / 15f; //1.0f;
                    if(currentOverture < targetOverture)
                        currentOverture += diff;
                    else
                        currentOverture-=diff;
                }else{
                    currentOverture = CRYSTAL_OVERTURE;
                }

                if(!currentEye.equals(targetEye)){
                    float diff = calculateDist(currentEye.cz, targetEye.cz, 8f);
                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz-=diff);
                }else{
                    currentEye.setCameraVector(0, 0, -1.9f);
                }

                if(MatrixHelper.beEqualTo(currentOverture,targetOverture)
                        && currentEye.equals(targetEye)){
                    //切换完成
                    currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
                    this.zoomTimes = 0;
                }
            }
            //从水晶球切换成 鱼眼
            if(currentControlMode == LTRenderMode.RENDER_MODE_CRYSTAL &&
                    targetControlMode == LTRenderMode.RENDER_MODE_FISHEYE){

                if(!MatrixHelper.beEqualTo(currentOverture,targetOverture, 1.0f)){
                    currentOverture += (ASTEROID_MIN_OVERTURE - CRYSTAL_OVERTURE) / 20f; //1.0f;
                }else{
                    currentOverture = ASTEROID_MIN_OVERTURE;
                }
                if(!currentEye.equals(targetEye)){
                    float diff = calculateDist(currentEye.cz, targetEye.cz, 8f);
                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz+=diff);
                    if(currentEye.cz > -1.0f)
                        currentEye.setCameraVector(0, 0, -1.0f);
                }else{
                    currentEye.setCameraVector(0, 0, -1.0f);
                }

                if(MatrixHelper.beEqualTo(currentOverture,targetOverture)
                        && currentEye.equals(targetEye)){
                    //切换完成
                    currentControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
                }
            }
            //从鱼眼切换成 小行星
            if(currentControlMode == LTRenderMode.RENDER_MODE_FISHEYE &&
                    targetControlMode == LTRenderMode.RENDER_MODE_PLANET){

                if(!MatrixHelper.beEqualTo(currentOverture,targetOverture)){
                    currentOverture += (ASTEROID_MAX_OVERTURE-ASTEROID_MIN_OVERTURE) / 35f; //2.0f;
                }else{
                    currentOverture = ASTEROID_MAX_OVERTURE;
                }

                if(!currentEye.equals(targetEye)){
                    float diff = calculateDist(currentEye.cz, targetEye.cz, 8f);
                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz-=diff);
                }else{
                    currentEye.setCameraVector(0, 0, -1.0f);
                }

                if( Math.abs(this.mfingerRotationX%360) < 90.0f){
                    float diff = calculateDist(this.mfingerRotationX, 90f, 2.0f);
                    if(this.mfingerRotationX > 0){
                        this.mfingerRotationX += diff;
                    } else {
                        this.mfingerRotationX -= diff;
                    }

                    if(Math.abs(this.mfingerRotationX%360) > 90.00f){
                        //角度切换完毕
                        if(this.mfingerRotationX > 0)
                            this.mfingerRotationX = 90.0f;
                        else
                            this.mfingerRotationX = -90.0f;
                    }
                }
                if(direction == 0)
                    this.mfingerRotationY -= (ASTEROID_MAX_OVERTURE-currentOverture)*0.15f;
                else
                    this.mfingerRotationY += (ASTEROID_MAX_OVERTURE-currentOverture)*0.15f;
                //this.mfingerRotationY += (ASTEROID_MAX_OVERTURE-currentOverture)*0.15f;
                if(MatrixHelper.beEqualTo(currentOverture,targetOverture)
                        && currentEye.equals(targetEye)
                        && MatrixHelper.beEqualTo(Math.abs(this.mfingerRotationX),90.0f) ){
                    //切换完成
                    currentControlMode = LTRenderMode.RENDER_MODE_PLANET;
                }
            }
//            //旧的 从小行星切换成 水晶球
//            if(currentControlMode == LTRenderMode.RENDER_MODE_PLANET &&
//                    targetControlMode == LTRenderMode.RENDER_MODE_CRYSTAL){
//                currentOverture = CRYSTAL_OVERTURE;
//
//                if(!currentEye.equals(targetEye)){
//                    float diff = calculateDist(currentEye.cz, targetEye.cz, 8f);
//                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz-=diff);
//                }else{
//                    currentEye.setCameraVector(0, 0, -1.9f);
//                }
//                if( Math.abs(this.mfingerRotationX%360) > 0.0f){
//                    float diff = calculateDist(this.mfingerRotationX, 0.0f, 2.0f);
//                    if(this.mfingerRotationX > 0) {
//                        this.mfingerRotationX -= diff;
//                    } else {
//                        this.mfingerRotationX += diff;
//                    }
//                    if(Math.abs(this.mfingerRotationX%360) <= 1.0f){
//                        this.mfingerRotationX = 0.0f;
//                        //角度切换完毕
//                    }
//                }
//
//                this.mfingerRotationY += this.mfingerRotationX*0.25f;
//                if(MatrixHelper.beEqualTo(currentOverture,targetOverture)
//                        && currentEye.equals(targetEye)
//                        && MatrixHelper.beEqualTo(Math.abs(this.mfingerRotationX), 0.0f)  ){
//                    currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;//切换完成
//                    this.zoomTimes = 0;
//                }
//            }
            //从小行星切换成 水晶球
            if(currentControlMode == LTRenderMode.RENDER_MODE_PLANET &&
                    targetControlMode == LTRenderMode.RENDER_MODE_CRYSTAL){
                // 2018.4.18 新增初始化后的变换动画。
                // 垂直面的，沿着X轴的旋转角
                if( !MatrixHelper.beEqualTo(this.mfingerRotationX, 0.0f, deviationFromPlanetToCrystal)) {
                    float diff = calculateDist(this.mfingerRotationX, 0.0f, 40f);
                    if(Math.abs(this.mfingerRotationX) > deviationFromPlanetToCrystal) {
                        if(this.mfingerRotationX < 0.0f)
                            this.mfingerRotationX += diff;
                        else
                            this.mfingerRotationX -= diff;
                    }
                } else {
                    //this.mfingerRotationX = 2.0f;
                }
                // 水平面的，沿着Y轴的旋转角
                float targetRotationY = getFineRotation(this.mfingerRotationY);
                if( !MatrixHelper.beEqualTo(this.mfingerRotationY, targetRotationY, deviationFromPlanetToCrystal)) {
                    float diff = calculateDist(this.mfingerRotationY, targetRotationY, 30f);
                    this.mfingerRotationY += diff;
                    if(this.mfingerRotationY > targetRotationY) {
                        this.mfingerRotationY = targetRotationY-deviationFromPlanetToCrystal;
                    }
                } else {
                    //this.mfingerRotationY = targetRotationY-deviationFromPlanetToCrystal;
                }
                // 焦距
                if( !MatrixHelper.beEqualTo(currentOverture,targetOverture, 0.2f)) {
                    float diff = calculateDist(currentOverture,targetOverture, 40f);
                    this.currentOverture -= diff;
                } else {
                    currentOverture = CRYSTAL_OVERTURE;
                }
                // 距离
                if( !MatrixHelper.beEqualTo(currentEye.cz, targetEye.cz, 0.01f)) {
                    float diff = calculateDist(currentEye.cz, targetEye.cz, 30f);
                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz-=diff);
                } else {
                    currentEye.setCameraVector(0, 0, -1.9f);
                }
                Log.w(TAG, "deviationFromPlanetToCrystal : "+deviationFromPlanetToCrystal);
                if( MatrixHelper.beEqualTo(this.mfingerRotationX, 0.0f, deviationFromPlanetToCrystal)
                        && MatrixHelper.beEqualTo(this.mfingerRotationY, targetRotationY, deviationFromPlanetToCrystal)
                        && MatrixHelper.beEqualTo(currentOverture,targetOverture, 0.5f)
                        && MatrixHelper.beEqualTo(currentEye.cz, -1.9f, 0.01f) ) {
                    //切换完成
                    currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
                    this.zoomTimes = 0;
                }
            }

            Log.w(TAG, "targetControlMode : "+targetControlMode);
            Log.w(TAG, "currentControlMode : "+currentControlMode);
            Log.w(TAG, "mfingerRotationY : "+mfingerRotationY);
            Log.w(TAG, "mfingerRotationX : "+mfingerRotationX);
            Log.w(TAG, "currentOverture : "+currentOverture);
            Log.w(TAG, "current mViewMatrix: " + "\n" +
                    currentEye.cx + " " +  currentEye.cy + " " +  currentEye.cz + "\n" +
                    currentEye.tx + " " +  currentEye.ty + " " +  currentEye.tz + "\n" +
                    currentEye.upx + " " + currentEye.upy + " " + currentEye.upz + "\n");
            Log.w(TAG, "=========================  " + "\n");

            //矩阵生效
            float ratio = (float)mSurfaceWidth / (float)mSurfaceHeight;
            MatrixHelper.perspectiveM(this.mProjectionMatrix,
                    currentOverture, ratio, 0.01f, 1000f);
            Matrix.setLookAtM(this.mViewMatrix,0,
                    currentEye.cx,  currentEye.cy,  currentEye.cz, //摄像机位置
                    currentEye.tx,  currentEye.ty,  currentEye.tz, //摄像机目标视点
                    currentEye.upx, currentEye.upy, currentEye.upz);//摄像机头顶方向向量
        } else {
            //Log.w(TAG, "current mModelMatrix: " + "\n" +
            //        mModelMatrix[0]+" "+mModelMatrix[4]+" "+mModelMatrix[8]+ " "+mModelMatrix[12]+"\n" +
            //        mModelMatrix[1]+" "+mModelMatrix[5]+" "+mModelMatrix[9]+ " "+mModelMatrix[13]+"\n" +
            //        mModelMatrix[2]+" "+mModelMatrix[6]+" "+mModelMatrix[10]+" "+mModelMatrix[14]+"\n" +
            //        mModelMatrix[3]+" "+mModelMatrix[7]+" "+mModelMatrix[11]+" "+mModelMatrix[15]+"\n");
            //Log.w(TAG, "current mProjectionMatrix: " + "\n" +
            //        mProjectionMatrix[0]+" "+mProjectionMatrix[4]+" "+mProjectionMatrix[8]+ mProjectionMatrix[12]+"\n" +
            //        mProjectionMatrix[1]+" "+mProjectionMatrix[5]+" "+mProjectionMatrix[9]+ mProjectionMatrix[13]+"\n" +
            //        mProjectionMatrix[2]+" "+mProjectionMatrix[6]+" "+mProjectionMatrix[10]+mProjectionMatrix[14]+"\n" +
            //        mProjectionMatrix[3]+" "+mProjectionMatrix[7]+" "+mProjectionMatrix[11]+mProjectionMatrix[15]+"\n");
            //Log.w(TAG, "current mViewMatrix: " + "\n" +
            //        currentEye.cx + " " +  currentEye.cy + " " +  currentEye.cz + "\n" +
            //        currentEye.tx + " " +  currentEye.ty + " " +  currentEye.tz + "\n" +
            //        currentEye.upx + " " + currentEye.upy + " " + currentEye.upz + "\n");
            //Log.w(TAG, "=========================  " + "\n");
            updatingBallControlMode = false;
            isOperating = false;
        }
    }

    private float getFineRotation(float value) {
        if(value>270.0f || value<=90.0f)
            return 90.0f;
        if(value>90.0f && value<=270.0f)
            return 270.0f;
        return value;
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
        if(!updatingBallControlMode) {
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
        if(currentControlMode!=targetControlMode
                && updatingBallControlMode){
            //在变换的过程中，把误差值变大，加快结束变换过程
            this.deviationFromPlanetToCrystal = 20.0f;
        }
    }

    /**
     * 滑动 增加上下限限制
     * @param x
     * @param y
     */
    public void handleTouchMove(float x, float y) {
        if(updatingBallControlMode) return;
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
     * 特殊处理  从鱼眼双击还原到水晶球 外部调用
     */
    public int fishEyeReturnToCrystal() {
        // fishEye -> crystal
        if(updatingBallControlMode)
            return targetControlMode;
        if(currentControlMode == LTRenderMode.RENDER_MODE_FISHEYE){
            targetOverture = CRYSTAL_OVERTURE;
            targetEye.setCameraVector(0, 0, -1.9f);
            //targetEye.setTargetViewVector(0f, 0f, 0.0f);
            //targetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
        }
        return targetControlMode;
    }

    /**
     * 双手操作
     * @param distance
     */
    public void handleMultiTouch(float distance) {
        if(updatingBallControlMode) return;
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

    // 初始化开机画面等待线程
    private class bootAnimationWaitThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(1111);
                isAnimation = true;
                //Thread.sleep(5000);
                //isNeedAutoScroll = true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
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

    /**
     * 处理vr模式的感应操作
     * @param xAngle
     * @param yAngle
     * @param zAngle
     */
    public void renderRotateVR(float xAngle, float yAngle, float zAngle) {
        //this.mfingerRotationY = xAngle;
        //this.mfingerRotationY = yAngle;
        //this.mfingerRotationY = zAngle;
        if (currentEye != null && currentEye.horizontalRotation(180f + xAngle) != null)
            currentEye.horizontalRotation(180f+xAngle).verticalRotation(-yAngle-90f);
    }




    public void setRenderMode(int renderMode) {
        if(currentEye==null)
            currentEye = new CameraViewport();
        if(targetEye==null)
            targetEye = new CameraViewport();

        if(renderMode != LTRenderMode.RENDER_MODE_VR){
            if(renderMode == LTRenderMode.RENDER_MODE_CRYSTAL){
                // 水晶球->鱼眼
                currentOverture = CRYSTAL_OVERTURE;
                currentEye.setCameraVector(0, 0, -1.9f);
                currentEye.setTargetViewVector(0f, 0f, 0.0f);
                currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
                currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
                mfingerRotationX = 0f;
                targetOverture = ASTEROID_MIN_OVERTURE;
                targetEye.setCameraVector(0, 0, -1.0f);
                targetEye.setTargetViewVector(0f, 0f, 0.0f);
                targetEye.setCameraUpVector(0f, 1.0f, 0.0f);
                targetControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
            }

            if(renderMode == LTRenderMode.RENDER_MODE_FISHEYE){
                // 鱼眼->小行星
                currentOverture = ASTEROID_MIN_OVERTURE;
                currentEye.setCameraVector(0, 0, -1.0f);
                currentEye.setTargetViewVector(0f, 0f, 0.0f);
                currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
                currentControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
                mfingerRotationX = 0f;
                targetOverture = ASTEROID_MAX_OVERTURE;
                targetEye.setCameraVector(0, 0, -1.0f);
                targetEye.setTargetViewVector(0f, 0f, 0.0f);
                targetEye.setCameraUpVector(0f, 1.0f, 0.0f);
                targetControlMode = LTRenderMode.RENDER_MODE_PLANET;
            }

            if(renderMode == LTRenderMode.RENDER_MODE_PLANET){
                // 小行星->全景球
                currentOverture = ASTEROID_MAX_OVERTURE;
                currentEye.setCameraVector(0, 0, -1.0f);
                currentEye.setTargetViewVector(0f, 0f, 0.0f);
                currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
                currentControlMode = LTRenderMode.RENDER_MODE_PLANET;
                mfingerRotationX = -70f;
                targetOverture = CRYSTAL_OVERTURE;
                targetEye.setCameraVector(0, 0, -1.9f);
                targetEye.setTargetViewVector(0f, 0f, 0.0f);
                targetEye.setCameraUpVector(0f, 1.0f, 0.0f);
                targetControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
            }
        } else {
            currentControlMode = targetControlMode = LTRenderMode.RENDER_MODE_VR;
            currentOverture = targetOverture = VR_OVERTURE;
            currentEye.setCameraVector(0, 0, 0.0f);
            currentEye.setTargetViewVector(0f, 0f, 1.0f);
            currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
            currentEye.copyTo(targetEye);
        }
    }




}

