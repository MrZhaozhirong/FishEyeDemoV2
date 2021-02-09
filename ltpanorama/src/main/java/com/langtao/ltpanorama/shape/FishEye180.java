package com.langtao.ltpanorama.shape;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;

import com.langtao.fisheye.FishEyeProc;
import com.langtao.fisheye.OneFisheye180Param;
import com.langtao.fisheye.OneFisheyeOut;
import com.langtao.ltpanorama.component.YUVFrame;
import com.langtao.ltpanorama.data.IndexBuffer;
import com.langtao.ltpanorama.data.VertexBuffer;
import com.langtao.ltpanorama.program.OneFishEye180ShaderProgram;
import com.langtao.ltpanorama.utils.CameraViewport;
import com.langtao.ltpanorama.utils.MatrixHelper;
import com.langtao.ltpanorama.utils.TextureHelper;

import java.nio.ByteBuffer;


public class FishEye180 {

    static{
        System.loadLibrary("one_fisheye");
        System.loadLibrary("LTFishEyeProc");
    }
    private static final String TAG = "Onefisheye180";
    //*****************************************************************
    private int numElements = 0;// 记录要画多少个三角形
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COORDIANTE_COMPONENT_COUNT = 3; // 每个顶点的坐标数 x y z
    private static final int TEXTURE_COORDIANTE_COMPONENT_COUNT = 2; // 每个纹理坐标为 S T两个
    private final static double overture = 45;
    //*****************************************************************
    private OneFishEye180ShaderProgram shader;
    private int drawElementType;
    private OneFisheyeOut out;
    private OneFisheye180Param outParam;
    private VertexBuffer verticesBuffer;
    private VertexBuffer texCoordsBuffer;
    private IndexBuffer indicesBuffer;
    private int mFrameWidth;
    private int mFrameHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int[] _yuvTextureIDs = new int[]{0};
    private CameraViewport eye;
    //***************************************************************
    public volatile boolean isInitialized = false;
    private float[] mProjectionMatrix = new float[16];// 4x4矩阵 存储投影矩阵
    private float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    private float[] mModelMatrix = new float[16];// 模型变换矩阵
    private float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵
    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }


    public FishEye180(){
        resetMatrixStatus();
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(this.mProjectionMatrix, 0);
        Matrix.setIdentityM(this.mViewMatrix, 0);

        eye = new CameraViewport();
        eye.setCameraVector(0, 0, -2.6f);
        eye.setTargetViewVector(0f, 0f, 0.0f);
        eye.setCameraUpVector(0f, 1.0f, 0.0f);

        //timer = new Timer();
        //timer.schedule(autoCruiseTimerTask, 5000, 10000);
    }

    public void onSurfaceCreate(String previewPicPathName, int[] previewPicRawData){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   //指定需要的是原始数据，非压缩数据
        Bitmap bitmap = BitmapFactory.decodeFile(previewPicPathName, options);
        if(bitmap == null){
            throw new IllegalStateException("previewPicPathName not load in bitmap!");
        }

        createBufferData(bitmap.getWidth(), bitmap.getHeight(), previewPicRawData);
        buildProgram();
        initTexture(bitmap);
        setAttributeStatus();
        isInitialized = true;
        bitmap.recycle();
        bitmap = null;
    }

    public void onSurfaceCreate(@NonNull YUVFrame frame){
        if(frame==null) return;
        createBufferData(frame.getWidth(),frame.getHeight(),frame);
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
                0, 0, -2.6f,    //摄像机位置
                0f, 0f, 0.0f,   //摄像机目标视点
                0f, 1.0f, 0.0f);//摄像机头顶方向向量
    }

    private void createBufferData(int width, int height, int[] previewPicRawData) {
        if(out == null){
            try{
                outParam = new OneFisheye180Param();
                Log.w(TAG, "OneFisheye180Param rgb width&height : " + width+ "  " + height);
                int ret = FishEyeProc.getOneFisheye180ParamIntRGBA(previewPicRawData, width, height, outParam);
                if (ret != 0) {
                    return;
                }
                out = FishEyeProc.oneFisheye180Func(100);

            }catch ( Exception e){
                e.printStackTrace();
                return;
            }finally {
            }
        }

        verticesBuffer = new VertexBuffer(out.vertices);
        texCoordsBuffer = new VertexBuffer(out.texCoords);

        numElements = out.indices.length;
        if(numElements < Short.MAX_VALUE){
            short[] element_index = new short[numElements];
            for (int i = 0; i < out.indices.length; i++) {
                element_index[i] = (short) out.indices[i];
            }
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_SHORT;
        }else{
            int[] element_index = new int[numElements];
            System.arraycopy(out.indices, 0, element_index, 0, out.indices.length);
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_INT;
        }
    }

    private void createBufferData(int width,int height,YUVFrame frame) {
        if(out == null){
            try{
                //InputStream is = context.getResources().openRawResource(R.raw.img_20170725_forward);
                //byte[] dataArray = new byte[is.available()];
                //is.read(dataArray);
                outParam = new OneFisheye180Param();
                //int ret = FishEyeProc.getOneFisheye180Param(dataArray, 1280, 720, outParam);
                Log.w(TAG, "OneFisheye180Param YUVFrame width&height : " + width + "  " + height);
                int ret = FishEyeProc.getOneFisheye180Param(frame.getYuvbyte(), width, height, outParam);
                if (ret != 0) {
                    return;
                }
                out = FishEyeProc.oneFisheye180Func(100);

            }catch ( Exception e){
                e.printStackTrace();
                return;
            }
        }

        verticesBuffer = new VertexBuffer(out.vertices);
        texCoordsBuffer = new VertexBuffer(out.texCoords);

        numElements = out.indices.length;
        if(numElements < Short.MAX_VALUE){
            short[] element_index = new short[numElements];
            for (int i = 0; i < out.indices.length; i++) {
                element_index[i] = (short) out.indices[i];
            }
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_SHORT;
        }else{
            int[] element_index = new int[numElements];
            System.arraycopy(out.indices, 0, element_index, 0, out.indices.length);
            indicesBuffer = new IndexBuffer(element_index);
            drawElementType = GLES20.GL_UNSIGNED_INT;
        }
    }

    private void buildProgram() {
        shader = new OneFishEye180ShaderProgram();
        //GLES20.glUseProgram( shader.getShaderProgramId() );
    }

    private boolean initTexture(Bitmap bitmap) {
        if(shader ==null) return false;
        GLES20.glUseProgram(shader.getShaderProgramId());
        int yuvTextureID = TextureHelper.loadTexture(bitmap);
        if (yuvTextureID == 0) {
            Log.w(TAG, "loadBitmapToTexture return TextureID=0 !");
            return false;
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureID);
        GLES20.glUniform1i(shader.uLocationSamplerRGB, 0);

        _yuvTextureIDs[0] = yuvTextureID;
        return true;
    }

    private boolean initTexture(int width,int height,YUVFrame frame) {
        if(shader == null) return false;
        GLES20.glUseProgram( shader.getShaderProgramId() );

        int[] yuvTextureIDs = TextureHelper.loadYUVTexture2(width, height,
                frame.getYDataBuffer(),frame.getUDataBuffer(),frame.getVDataBuffer());
        if(yuvTextureIDs == null || yuvTextureIDs.length != 3) {
            Log.w(TAG,"yuvTextureIDs object's length not equals 3 !");
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
        if(shader == null) return ;
        GLES20.glUseProgram( shader.getShaderProgramId() );
        float kColorConversion420[] = {
                1.0f, 1.0f, 1.0f,
                0.0f, -0.39465f, 2.03211f,
                1.13983f, -0.58060f, 0.0f
        };
        GLES20.glUniformMatrix3fv(shader.uLocationCCM, 1, false, kColorConversion420, 0);
        if(verticesBuffer==null||texCoordsBuffer==null){
            return;
        }
        verticesBuffer.setVertexAttribPointer(shader.aPositionLocation,
                POSITION_COORDIANTE_COMPONENT_COUNT,
                POSITION_COORDIANTE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);
        texCoordsBuffer.setVertexAttribPointer(shader.aTexCoordLocation,
                TEXTURE_COORDIANTE_COMPONENT_COUNT,
                TEXTURE_COORDIANTE_COMPONENT_COUNT * BYTES_PER_FLOAT, 0);

        float width = (float)outParam.width;
        float height = (float)outParam.height;
        GLES20.glUniform1f(shader.uLocationWidth, width);
        GLES20.glUniform1f(shader.uLocationHeight, height);
        GLES20.glUniform1f(shader.uLocationRectX, outParam.rectX);
        GLES20.glUniform1f(shader.uLocationRectY, outParam.rectY);
        GLES20.glUniform1f(shader.uLocationRectWidth, outParam.rectWidth);
        GLES20.glUniform1f(shader.uLocationRectHeight, outParam.rectHeight);
    }

    private void draw(){
        if(shader == null) return ;
        GLES20.glUseProgram(shader.getShaderProgramId());
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        //将最终变换矩阵写入
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, getFinalMatrix(),0);
		if(indicesBuffer==null){
			return;
		}
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.getIndexBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, drawElementType, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void onDrawFrame(YUVFrame frame) {
        if(!isInitialized) return;

        GLES20.glViewport(0,0,mSurfaceWidth,mSurfaceHeight);
        GLES20.glClearColor(0.0f,0.0f,0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram( shader.getShaderProgramId() );
        updateTexture(frame);
        GLES20.glUniform1i(shader.uLocationImageMode, 0);

        setAttributeStatus();
        this.updateCruise();
        this.updateMatrix();
        this.draw();
    }

    public void onDrawPreviewPic() {
        if(!isInitialized) return;

        GLES20.glViewport(0,0,mSurfaceWidth,mSurfaceHeight);
        GLES20.glClearColor(0.0f,0.0f,0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        GLES20.glUniform1i(shader.uLocationImageMode, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
        GLES20.glUniform1i(shader.uLocationSamplerRGB, 0);

        setAttributeStatus();
        this.updateCruise();
        this.updateMatrix();
        this.draw();
    }


    //================================操作封装==================================================================
    //================================模型操作相关==============================================================
    //*****************************************************************
    //** 单手双手操作相关
    private float mLastX;
    private float mLastY;
    private float mfingerRotationX = 0;
    private float mfingerRotationY = 0;
    private float mfingerRotationZ = 0;
    private float[] mMatrixFingerRotationX = new float[16];
    private float[] mMatrixFingerRotationY = new float[16];
    private float[] mMatrixFingerRotationZ = new float[16];
    private void resetMatrixStatus(){
        mfingerRotationX = 0;
        mfingerRotationY = 0;
        mfingerRotationZ = 0;
        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationZ, 0);
    }
    private final static float PLATE_SCALE_MAX_VALUE =2.2f;
    private final static float PLATE_SCALE_MIN_VALUE =0.0f;
    private float zoomTimes = 0.0f;
    //自动巡航相关
    private volatile boolean isAutoCruise = true;
    //private Timer timer;
    //private TimerTask autoCruiseTimerTask = new TimerTask(){
    //    @Override
    //    public void run() {
    //        isAutoCruise = true;
    //    }
    //};
    private int cruise_flag = 0;
    private void updateCruise() {
        if(isAutoCruise && getCurrentPerspectiveMode()==FishEye180.MODE_ENDOSCOPE){
            if(cruise_flag == 0){
                this.mfingerRotationX += 0.2f;
            }else{
                this.mfingerRotationX -= 0.2f;
            }
            if(this.mfingerRotationX > 55f){
                cruise_flag = 1;
            }else if(this.mfingerRotationX < -55f){
                cruise_flag = 0;
            }
        }
    }


    private boolean updateTexture(@NonNull YUVFrame yuvFrame ){
        if(yuvFrame==null || shader == null) return false;
        int width = yuvFrame.getWidth();
        int height = yuvFrame.getHeight();
        ByteBuffer yDatabuffer = yuvFrame.getYDataBuffer();
        ByteBuffer uDatabuffer = yuvFrame.getUDataBuffer();
        ByteBuffer vDatabuffer = yuvFrame.getVDataBuffer();

        GLES20.glUseProgram(shader.getShaderProgramId());
        //if(width != mFrameWidth || height!= mFrameHeight)
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
        //{
        //    //长宽没变，更新纹理，不重建
        //    TextureHelper.updateTexture2(_yuvTextureIDs[0], mFrameWidth, mFrameHeight, yDatabuffer);
        //    TextureHelper.updateTexture2(_yuvTextureIDs[1], mFrameWidth, mFrameHeight, uDatabuffer);
        //    TextureHelper.updateTexture2(_yuvTextureIDs[2], mFrameWidth, mFrameHeight, vDatabuffer);
        //}
        //重新加载纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[0]);
        GLES20.glUniform1i(shader.uLocationSamplerY, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[1]);
        GLES20.glUniform1i(shader.uLocationSamplerU, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _yuvTextureIDs[2]);
        GLES20.glUniform1i(shader.uLocationSamplerV, 2);
        return true;
    }

    private void updateMatrix() {

        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.scaleM(this.mModelMatrix,0,1.0f,1.0f,1.0f);

        Matrix.setIdentityM(this.mMatrixFingerRotationX, 0);
        Matrix.setIdentityM(this.mMatrixFingerRotationY, 0);
        Matrix.rotateM(this.mMatrixFingerRotationY, 0, this.mfingerRotationX, 0, 1, 0);
        Matrix.rotateM(this.mMatrixFingerRotationX, 0, this.mfingerRotationY, 1, 0, 0);
        Matrix.multiplyMM(this.mModelMatrix,0, this.mMatrixFingerRotationX,0, this.mMatrixFingerRotationY,0 );
    }



    //双击屏幕 切换视角
    public static int MODE_OVER_LOOK = 0;
    public static int MODE_ENDOSCOPE = 1;
    private int currentPerspectiveMode = MODE_OVER_LOOK;
    private volatile boolean mTransforming = false;

    public int getCurrentPerspectiveMode() {
        return currentPerspectiveMode;
    }

    public void handleDoubleClick() {
        mTransforming = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean transforming = true;
                while(transforming){
                    if(currentPerspectiveMode == MODE_OVER_LOOK){
                        transforming = transformToEndoscope();
                    }else if(currentPerspectiveMode == MODE_ENDOSCOPE){
                        transforming = transformToOverlook();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.w(TAG,"current mViewMatrix: "+"\n"+
                        eye.cx+" "+eye.cy+" "+eye.cz+"\n"+
                        eye.tx+" "+eye.ty+" "+eye.tz+"\n"+
                        eye.upx+" "+eye.upy+" "+eye.upz+"\n");
                currentPerspectiveMode =
                        (currentPerspectiveMode == MODE_OVER_LOOK ? MODE_ENDOSCOPE:MODE_OVER_LOOK);
                mTransforming = false;
            }
        }).start();
    }
    private boolean transformToOverlook() {
        if(eye.cz > -2.6f){
            Matrix.setLookAtM(this.mViewMatrix,0,
                    eye.cx, eye.cy, eye.cz-=0.02f,
                    eye.tx, eye.ty, eye.tz,
                    eye.upx, eye.upy, eye.upz);
            return true;
        }
        return false;
    }

    private boolean transformToEndoscope() {
        if(eye.cz < -1.0f){
            Matrix.setLookAtM(this.mViewMatrix,0,
                    eye.cx, eye.cy, eye.cz+=0.02f,
                    eye.tx, eye.ty, eye.tz,
                    eye.upx, eye.upy, eye.upz);
            return true;
        }
        return false;
    }

    public void handleTouchDown(float x, float y) {
        this.mLastX = x;
        this.mLastY = y;
        this.isAutoCruise = false;
    }

    public void handleTouchUp(float x, float y, float xVelocity, float yVelocity) {
        this.mLastX = 0;
        this.mLastY = 0;
        //if(currentPerspectiveMode == MODE_ENDOSCOPE)
        {
            if(this.mfingerRotationX > 45f){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(mfingerRotationX > 45f){
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mfingerRotationX -=0.2f;
                        }
                    }
                }).start();
            }
            if(this.mfingerRotationX < -45f){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(mfingerRotationX < -45f){
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mfingerRotationX +=0.2f;
                        }
                    }
                }).start();
            }
        }
    }


    public void handleTouchMove(float x, float y) {
        float offsetX = this.mLastX - x;
        float offsetY = this.mLastY - y;
        this.mfingerRotationX += offsetX/10;
        this.mfingerRotationY -= offsetY/10;
        if(this.mfingerRotationY > 10f){
            this.mfingerRotationY = 10f;
        }
        if(this.mfingerRotationY < -10f){
            this.mfingerRotationY = -10f;
        }

        if(this.mfingerRotationX > 55f){
            this.mfingerRotationX = 55f;
        }
        if(this.mfingerRotationX < -55f){
            this.mfingerRotationX = -55f;
        }
//            Log.w(TAG, "CurvedPlate mfingerRotationX : "+CurvedPlate.mfingerRotationX);
//            Log.w(TAG, "CurvedPlate mfingerRotationY : "+CurvedPlate.mfingerRotationY);
        this.mLastX = x;
        this.mLastY = y;
    }

    public void handleMultiTouch(float distance) {
        float dis = distance / 10;
        float scale;
        if(dis < 0 ){
            //小于0 两点距离比前一刻的两点距离短 在缩小
            scale = -0.1f;
            this.zoomTimes -= 0.1;
        } else {
            scale = 0.1f;
            this.zoomTimes += 0.1;
        }

        if(this.zoomTimes > PLATE_SCALE_MAX_VALUE) {
            scale = 0.0f;
            this.zoomTimes = PLATE_SCALE_MAX_VALUE;
        }
        if(this.zoomTimes < PLATE_SCALE_MIN_VALUE) {
            scale = 0.0f;
            this.zoomTimes = PLATE_SCALE_MIN_VALUE;
        }

        Matrix.translateM(this.mViewMatrix,0, 0f,0f,-scale);

        eye.setCameraVector(eye.cx,eye.cy, this.mViewMatrix[14]);
    }

    public void setAutoCruise(boolean autoCruise) {
        this.isAutoCruise = autoCruise;
    }


}
