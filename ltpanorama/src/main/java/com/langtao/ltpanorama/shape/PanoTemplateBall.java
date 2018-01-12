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

import static com.langtao.ltpanorama.utils.MatrixHelper.beEqualTo;


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

    //自动旋转相关
    private volatile boolean isNeedAutoScroll = false;
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

    public PanoTemplateBall(int render_mode){
        resetMatrixStatus();
        initCameraEye(render_mode);
    }





    private boolean initTemplateConfigFile(String templateFileName) {
        ByteBuffer dataBuffer = null;
        try
        {
            File configFile = new File(templateFileName);
            if(!configFile.exists()) {
                return false;
            }
            FileInputStream fis = new FileInputStream(new File(templateFileName));
            byte[] dataArray = new byte[fis.available()];
            Log.d(TAG, "DEBUG: templateFile length "+dataArray.length);
            Log.d(TAG, "DEBUG: m_templateParam width*height*4 length "+ m_templateParam.width * m_templateParam.height * 4);
            Log.d(TAG, "DEBUG: m_templateParam width*height*4*2 length "+ m_templateParam.width * m_templateParam.height * 4 * 2);
            if(dataArray.length < m_templateParam.width * m_templateParam.height*4*2) {
                throw new IllegalArgumentException("Error: Panorama Template Config File Wrong !!!");
            }
            fis.read(dataArray);
            dataBuffer = ByteBuffer.allocateDirect(dataArray.length)
                    .order(ByteOrder.nativeOrder());
            dataBuffer.put(dataArray);
            dataBuffer.clear();
            loadTemplateTexture(dataBuffer);
            return true;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

    //private boolean initTemplateTexture() {
    //    ByteBuffer dataBuffer = null;
    //    try{
    //        InputStream is = context.getResources().openRawResource(R.raw.pano_tem_file);
    //        if (is.available() != m_templateParam.dataSize) {
    //            return  false;
    //        }
    //        byte[] dataArray = new byte[is.available()];
    //        is.read(dataArray);
    //        //读出原始数据
    //        dataBuffer = ByteBuffer.allocateDirect(dataArray.length)
    //                .order(ByteOrder.nativeOrder());
    //        dataBuffer.put(dataArray);
    //        dataBuffer.clear();
    //    }catch (Exception ex){
    //        ex.printStackTrace();
    //        return false;
    //    }
    //    loadTemplateTexture(dataBuffer);
    //    return true;
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
                out = PanoTemplateProc.panoramaSphere();
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

    public void onSurfaceCreated(String templateFileName) {
        // 获取模板参数
        m_templateParam = PanoTemplateProc.getPanoTemplateSize();

        if(templateFileName==null ||
                "".equalsIgnoreCase(templateFileName) ||
                !new File(templateFileName).exists() ) {
            Log.e(TAG, "Error: setPanoTemplateConfigFile is null or File not exists !!!");
            Log.e(TAG, "Error: It will error show Panorama in LangTao-GL !!!");
            //initTemplateTexture();
            throw new IllegalArgumentException("Error: Panorama Template Config File is null or File not exists !!!");
        }else {
            if( initTemplateConfigFile(templateFileName) ){
                createBufferData();
                buildProgram();
                //initTexture(frame);
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
            GLES20.glUniformMatrix4fv(pbShader.mMVPMatrixLoc, 1, false, getFinalMatrix(),0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_INT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //================================模型变形相关=======================================================================
    private CameraViewport currentEye;
    private CameraViewport tartgetEye;
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
        if(tartgetEye==null)
            tartgetEye = new CameraViewport();

        if(RENDER_MODE != LTRenderMode.RENDER_MODE_VR){
            targetOverture = currentOverture = CRYSTAL_OVERTURE;
            targetControlMode = currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
            currentEye.setCameraVector(0, 0, -2f);
            currentEye.setTargetViewVector(0f, 0f, 0.0f);
            currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
            currentEye.copyTo(tartgetEye);
        } else {
            currentControlMode = targetControlMode = LTRenderMode.RENDER_MODE_VR;
            currentOverture = targetOverture = VR_OVERTURE;
            currentEye.setCameraVector(0, 0, 0.0f);
            currentEye.setTargetViewVector(0f, 0f, 1.0f);
            currentEye.setCameraUpVector(0f, 1.0f, 0.0f);
            currentEye.copyTo(tartgetEye);
        }
    }

    public int nextControlMode() {
        if(currentControlMode == LTRenderMode.RENDER_MODE_CRYSTAL){
            targetOverture = ASTEROID_MIN_OVERTURE;
            tartgetEye.setCameraVector(0, 0, -1.0f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
        }

        if(currentControlMode == LTRenderMode.RENDER_MODE_FISHEYE){
            targetOverture = ASTEROID_MAX_OVERTURE;
            tartgetEye.setCameraVector(0, 0, -1.0f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_PLANET;
        }

        if(currentControlMode == LTRenderMode.RENDER_MODE_PLANET){
            targetOverture = CRYSTAL_OVERTURE;
            tartgetEye.setCameraVector(0, 0, -2.0f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;
        }
        return targetControlMode;
    }

    private float calculateDist(float current, float target, float divisor) {
        if(divisor==0) return 0;
        float absCurrent = Math.abs(current);
        float absTarget = Math.abs(target);
        float diff = Math.abs(absCurrent - absTarget);
        float dist = (float) (Math.sqrt(Math.pow(diff, 2.0)) / divisor);
        return Math.abs(dist);
    }

    private volatile boolean updateingBallControlMode = false;

    public void updateBallControlMode() {
        if(currentControlMode != targetControlMode){
            updateingBallControlMode = true;
            //从水晶球切换成 鱼眼
            if(currentControlMode == LTRenderMode.RENDER_MODE_CRYSTAL &&
                    targetControlMode == LTRenderMode.RENDER_MODE_FISHEYE){

                if(!beEqualTo(currentOverture,targetOverture)){
                    currentOverture += (ASTEROID_MIN_OVERTURE - CRYSTAL_OVERTURE) / 20f; //1.0f;
                }else{
                    currentOverture = ASTEROID_MIN_OVERTURE;
                }
                if(!currentEye.equals(tartgetEye)){
                    float diff = calculateDist(currentEye.cz, tartgetEye.cz, 8f);
                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz+=diff);
                }else{
                    currentEye.setCameraVector(0, 0, -1.0f);
                }

                if(beEqualTo(currentOverture,targetOverture)
                        && currentEye.equals(tartgetEye)){
                    //切换完成
                    currentControlMode = LTRenderMode.RENDER_MODE_FISHEYE;
                }
            }
            //从鱼眼切换成 小行星
            if(currentControlMode == LTRenderMode.RENDER_MODE_FISHEYE &&
                    targetControlMode == LTRenderMode.RENDER_MODE_PLANET){

                if(!beEqualTo(currentOverture,targetOverture)){
                    currentOverture += (ASTEROID_MAX_OVERTURE-ASTEROID_MIN_OVERTURE) / 35f; //2.0f;
                }else{
                    currentOverture = ASTEROID_MAX_OVERTURE;
                }

                if(!currentEye.equals(tartgetEye)){
                    float diff = calculateDist(currentEye.cz, tartgetEye.cz, 8f);
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

                this.mfingerRotationY += (ASTEROID_MAX_OVERTURE-currentOverture)*0.15f;
                if(beEqualTo(currentOverture,targetOverture)
                        && currentEye.equals(tartgetEye)
                        && beEqualTo(Math.abs(this.mfingerRotationX),90.0f) ){
                    //切换完成
                    currentControlMode = LTRenderMode.RENDER_MODE_PLANET;
                }
            }
            //从小行星切换成 水晶球
            if(currentControlMode == LTRenderMode.RENDER_MODE_PLANET &&
                    targetControlMode == LTRenderMode.RENDER_MODE_CRYSTAL){

                //if(currentOverture > targetOverture){
                //    currentOverture -= (ASTEROID_MAX_OVERTURE - CRYSTAL_OVERTURE) / 30f;//2.5f;
                //    //currentOverture += currentEye.cz*ASTEROID_MAX_OVERTURE*0.01f;
                //}else{
                //    currentOverture = CRYSTAL_OVERTURE;
                //}
                currentOverture = CRYSTAL_OVERTURE;

                if(!currentEye.equals(tartgetEye)){
                    float diff = calculateDist(currentEye.cz, tartgetEye.cz, 8f);
                    currentEye.setCameraVector(currentEye.cx,currentEye.cy,currentEye.cz-=diff);
                }else{
                    currentEye.setCameraVector(0, 0, -2.0f);
                }

                if( Math.abs(this.mfingerRotationX%360) > 0.0f){
                    float diff = calculateDist(this.mfingerRotationX, 0.0f, 2.0f);
                    if(this.mfingerRotationX > 0) {
                        this.mfingerRotationX -= diff;
                    } else {
                        this.mfingerRotationX += diff;
                    }
                    if(Math.abs(this.mfingerRotationX%360) <= 1.0f){
                        this.mfingerRotationX = 0.0f;
                        //角度切换完毕
                    }
                }

                this.mfingerRotationY += this.mfingerRotationX*0.25f;
                if(beEqualTo(currentOverture,targetOverture)
                        && currentEye.equals(tartgetEye)
                        && beEqualTo(Math.abs(this.mfingerRotationX), 0.0f)  ){
                    currentControlMode = LTRenderMode.RENDER_MODE_CRYSTAL;//切换完成
                    this.zoomTimes = 0;
                }
            }

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
            updateingBallControlMode = false;
        }
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
        if(this.mfingerRotationY > 360f || this.mfingerRotationY < -360f){
            this.mfingerRotationY = this.mfingerRotationY % 360f;
        }
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
    }

    /**
     * 滑动 增加上下限限制
     * @param x
     * @param y
     */
    public void handleTouchMove(float x, float y) {
        if(updateingBallControlMode) return;

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
            this.mfingerRotationX -= offsetY/5 ;
        }

        this.mfingerRotationY += offsetX/8 ;

        updateBallBoundary();
        if(false){
            Log.w(TAG,"ball.mfingerRotationY : "+this.mfingerRotationY);
            Log.w(TAG,"ball.mfingerRotationX : "+this.mfingerRotationX);
        }
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
     * 双手操作
     * @param distance
     */
    public void handleMultiTouch(float distance) {
        if(updateingBallControlMode) return;
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
        currentEye.setCameraVector(currentEye.cx, currentEye.cy, this.mViewMatrix[14]);

        Log.w(TAG, "currentOverture : "+currentOverture);
        Log.w(TAG, "current mViewMatrix: " + "\n" +
                currentEye.cx + " " +  currentEye.cy + " " +  currentEye.cz + "\n" +
                currentEye.tx + " " +  currentEye.ty + " " +  currentEye.tz + "\n" +
                currentEye.upx + " " + currentEye.upy + " " + currentEye.upz + "\n");
        //Log.w(TAG, "mfingerRotationX : "+mfingerRotationX + "\n");
        Log.w(TAG, "=========================  " + "\n");
    }



    public void autoRotated() {
        this.mfingerRotationY -= 0.5f;
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
        initCameraEye(renderMode);
    }

}

