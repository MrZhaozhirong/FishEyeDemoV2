package com.langtao.ltpanorama.shape;

import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Log;

import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.FrameBuffer;
import com.langtao.ltpanorama.data.IndexBuffer;
import com.langtao.ltpanorama.data.VertexBuffer;
import com.langtao.ltpanorama.program.PanoTemplateRectangleShaderProgram;
import com.langtao.ltpanorama.utils.TextureHelper;
import com.langtao.tmpanorama.PanoTemplateOut;
import com.langtao.tmpanorama.PanoTemplateProc;
import com.langtao.tmpanorama.PanoramaOut;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Created by zzr on 2017/12/7.
 */

public class PanoTemplateRectangleFBO {
    private final static String TAG = "PanoTemRectangleFBO";
    static{
        System.loadLibrary("panoTemplate");
        System.loadLibrary("LTPanoTemProc");
    }
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COORDIANTE_COMPONENT_COUNT= 3;
    private static final int TEXTURE_COORDIANTE_COMPONENT_COUNT = 2;

    private PanoramaOut out;
    private VertexBuffer verticesBuffer;
    private VertexBuffer texCoordsBuffer;
    private IndexBuffer indicesBuffer;
    // 记录要画多少个三角形
    private int numElements = 0;
    private PanoTemplateOut m_templateParam;
    PanoTemplateRectangleShaderProgram pbShader;

    private int m_Map1TextureID = 0;
    private int m_Map2TextureID = 0;
    private int m_WeightTextureID = 0;
    private boolean m_templateIsOK = false;

    public PanoTemplateRectangleFBO( ){ }

    private void createBufferData() {
        if(out == null){
            try{
                out = PanoTemplateProc.panoramaRectangle();
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
    //        if(dataArray.length < m_templateParam.width * m_templateParam.height*4*2) {
    //            throw new IllegalArgumentException("Error: Panorama Template Config File Wrong !!!");
    //        }
    //        fis.read(dataArray);
    //        dataBuffer = ByteBuffer.allocateDirect(dataArray.length).order(ByteOrder.nativeOrder());
    //        dataBuffer.put(dataArray);
    //        dataBuffer.clear();
    //        loadTemplateTexture(dataBuffer);
    //        return true;
    //    }catch (Exception ex){
    //        ex.printStackTrace();
    //    }
    //    return false;
    //}

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
            return false;
        }
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

    private void buildProgram() {
        pbShader = new PanoTemplateRectangleShaderProgram();
        //GLES20.glUseProgram( pbShader.getShaderProgramId() );
    }

    private boolean initTexture(@NonNull YUVFrame frame) {
        int[] _yuvTextureIDs = TextureHelper.loadYUVTexture2(frame.getWidth(), frame.getHeight(),
                frame.getYDataBuffer(), frame.getUDataBuffer(), frame.getVDataBuffer());
        if(_yuvTextureIDs == null || _yuvTextureIDs.length != 3) {
            Log.w(TAG,"_yuvTextureIDs object's length not equals 3 !");
            return false;
        }
        return true;
    }

    private void setAttributeStatus() {
        if(verticesBuffer != null)
        verticesBuffer.setVertexAttribPointer(pbShader.mPositionLoc,
                POSITION_COORDIANTE_COMPONENT_COUNT,
                POSITION_COORDIANTE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);
        if(texCoordsBuffer != null)
        texCoordsBuffer.setVertexAttribPointer(pbShader.mTexCoordLoc,
                TEXTURE_COORDIANTE_COMPONENT_COUNT,
                TEXTURE_COORDIANTE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);
    }










    private FrameBuffer rectangleFbo;
    private IntBuffer imageBuf ;
    private int fboWidth;
    private int fboHeight;

    public void initFBO(int width, int height) {
        if(rectangleFbo!=null){
            rectangleFbo.release();
            //不用 置空
        }else {
            rectangleFbo = new FrameBuffer();
        }
        Log.w(TAG, "frame width*height : "+width+" x "+height);
        int[] fboSize = calculationFBOSize(width, height);
        fboWidth = fboSize[0];
        fboHeight = fboSize[1];
        Log.w(TAG, "initFBO fboWidth*fboHeight : "+fboWidth+" x "+fboHeight);
        rectangleFbo.setup(fboWidth, fboHeight);
        if(imageBuf != null) {
            if(imageBuf.capacity()<fboWidth*fboHeight){
                imageBuf = IntBuffer.allocate(fboWidth*fboHeight);
            }
        } else {
            imageBuf = IntBuffer.allocate(fboWidth*fboHeight);
        }
    }

    private int[] calculationFBOSize(int width, int height) {
        int[] rest = new int[2];
        float wh_per = 0.5f;
        float wh_total = width * height;
        double w_new = Math.sqrt(wh_total / wh_per);
        double h_new = w_new *  0.5;

        double total_new = w_new * h_new;
        while(total_new > 1800000){
            w_new = w_new / 1.3;
            h_new = h_new / 1.3;
            total_new = w_new * h_new;
        }
        rest[0] = (int) w_new;
        rest[1] = (int) h_new;
        return rest;
    }

    private void resizeFBO(int width, int height){
        if(rectangleFbo!=null){
            rectangleFbo.release();
            rectangleFbo = null;
        }
        initFBO(width, height);
    }

    // 截图申请一次，draw一次fbo
    public void draw(YUVFrame frame){
        if (frame == null)  return;
        if(!m_templateIsOK) return;

        rectangleFbo.begin();
        GLES20.glUseProgram( pbShader.getShaderProgramId() );
        //// start.updateTexture();
        int width = frame.getWidth();
        int height = frame.getHeight();
        ByteBuffer yDatabuffer = frame.getYDataBuffer();
        ByteBuffer uDatabuffer = frame.getUDataBuffer();
        ByteBuffer vDatabuffer = frame.getVDataBuffer();
        int[] yuvTextureIDs = TextureHelper.loadYUVTexture2(width, height,
                yDatabuffer, uDatabuffer, vDatabuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[0]);
        GLES20.glUniform1i(pbShader.mSamplerYLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[1]);
        GLES20.glUniform1i(pbShader.mSamplerULoc, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIDs[2]);
        GLES20.glUniform1i(pbShader.mSamplerVLoc, 2);
        //// end.updateTexture();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_Map1TextureID);
        GLES20.glUniform1i(pbShader.mSamplerMap1Loc, 3);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_Map2TextureID);
        GLES20.glUniform1i(pbShader.mSamplerMap2Loc, 4);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m_WeightTextureID);
        GLES20.glUniform1i(pbShader.mSamplerWeightLoc, 5);

        setAttributeStatus();
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
        GLES20.glViewport(0, 0, fboWidth, fboHeight);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_INT, 0);


        if( requestScreenShot ){
            GLES20.glReadPixels(0, 0, fboWidth, fboHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, imageBuf);
            if( screenShotReadyCallback != null){
                //Note: 放线程处理，不要阻塞，否则会爆炸
                screenShotReadyCallback.onScreenShotReady(0, 0, fboWidth, fboHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, imageBuf);
            }
            Log.w(TAG, "requestScreenShot "+requestScreenShot);
            requestScreenShot = false;
        }
        //////////////////////////////////////////////////////////////////////////////////////////////
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        rectangleFbo.end();
    }

    public volatile boolean isInitialized = false;

    //public void onEGLSurfaceCreated(String templateFileName) {
    //    // 获取模板参数
    //    m_templateParam = PanoTemplateProc.getPanoTemplateSize();
    //    if( initTemplateConfigFile(templateFileName) ){
    //        createBufferData();
    //        buildProgram();
    //        //initTexture(frame);
    //        //setAttributeStatus();
    //        isInitialized = true;
    //    }
    //}

    public void onEGLSurfaceCreated(String secretGIDStr, String templateFileName) {
        if( initTemplateConfigFile(secretGIDStr, templateFileName) ){
            createBufferData();
            buildProgram();
            //initTexture(frame);
            //setAttributeStatus();
            isInitialized = true;
        }
    }
    /////////// interface 截图对外接口 //////////////////////////////////
    private volatile boolean requestScreenShot = false;
    private ScreenShotReadyCallback screenShotReadyCallback;
    public void requestScreenShot(final ScreenShotReadyCallback callback){
        this.screenShotReadyCallback = callback;
        requestScreenShot = true;
    }

    public interface ScreenShotReadyCallback {
        void onScreenShotReady(int x,
                               int y,
                               int width,
                               int height,
                               int format,
                               int type,
                               IntBuffer imageBuf );
    }

}
