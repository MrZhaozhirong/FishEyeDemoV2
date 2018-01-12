package com.langtao.ltpanorama.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Created by ZZR on 2017/2/10.
 */

public class TextureHelper {

    private static final String TAG = "TextureHelper";

    public static void updateTexture2(int textureId, int width, int height,ByteBuffer dataBuffer){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,0, 0,0,width,height,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, dataBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static int[] loadYUVTexture2(int width, int height,
                                        ByteBuffer yDatabuffer, ByteBuffer uDatabuffer, ByteBuffer vDatabuffer){
        if(yDatabuffer==null || uDatabuffer==null || vDatabuffer==null){
            return null;
        }
        //加载SamplerY
        final int[] _samplerYTexture = new int[1];
        GLES20.glGenTextures(1, _samplerYTexture, 0);
        if(_samplerYTexture[0] == 0){
            Log.w(TAG,"_samplerYTexture Could not generate a new OpenGL texture object!");
            return null;
        }
        yDatabuffer.position(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _samplerYTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, width, height,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                yDatabuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //加载SamplerU
        final int[] _samplerUTexture = new int[1];
        GLES20.glGenTextures(1, _samplerUTexture, 0);
        if(_samplerUTexture[0] == 0){
            Log.w(TAG,"_samplerUTexture Could not generate a new OpenGL texture object!");
            return null;
        }
        uDatabuffer.position(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _samplerUTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, width/2, height/2,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                uDatabuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //加载SamplerV
        final int[] _samplerVTexture = new int[1];
        GLES20.glGenTextures(1, _samplerVTexture, 0);
        if(_samplerVTexture[0] == 0){
            Log.w(TAG,"_samplerVTexture Could not generate a new OpenGL texture object!");
            return null;
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _samplerVTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        vDatabuffer.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, width/2, height/2,
                0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                vDatabuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //返回纹理ID
        int[] result = new int[3];
        result[0] = _samplerYTexture[0];
        result[1] = _samplerUTexture[0];
        result[2] = _samplerVTexture[0];
        return result;
    }

    public static int[] loadYUVTexture(Context context, int resourceId, int width, int height){
        ByteBuffer dataBuffer = null;
        try{
            InputStream is = context.getResources().openRawResource(resourceId);
            byte[] dataArray = new byte[is.available()];
            is.read(dataArray);
            //读出原始数据
            dataBuffer = ByteBuffer.allocateDirect(dataArray.length * 1)
                    .order(ByteOrder.nativeOrder());
            dataBuffer.put(dataArray);
            dataBuffer.position(0);
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }

        //加载SamplerY
        final int[] _samplerYTexture = new int[1];
        GLES20.glGenTextures(1, _samplerYTexture, 0);
        if(_samplerYTexture[0] == 0){
            Log.w(TAG,"_samplerYTexture Could not generate a new OpenGL texture object!");
            return null;
        }
        int idxY = 0;
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _samplerYTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        dataBuffer.clear();
        dataBuffer.position(idxY);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                            GLES20.GL_LUMINANCE, width, height,
                            0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                            dataBuffer);
        //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //加载SamplerU
        final int[] _samplerUTexture = new int[1];
        GLES20.glGenTextures(1, _samplerUTexture, 0);
        if(_samplerUTexture[0] == 0){
            Log.w(TAG,"_samplerUTexture Could not generate a new OpenGL texture object!");
            return null;
        }
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _samplerUTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        dataBuffer.clear();
        int idxU = width * height;
        dataBuffer.position(idxU);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                            GLES20.GL_LUMINANCE, width/2, height/2,
                            0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                            dataBuffer);
        //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //加载SamplerV
        final int[] _samplerVTexture = new int[1];
        GLES20.glGenTextures(1, _samplerVTexture, 0);
        if(_samplerVTexture[0] == 0){
            Log.w(TAG,"_samplerVTexture Could not generate a new OpenGL texture object!");
            return null;
        }
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _samplerVTexture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        dataBuffer.clear();
        int idxV = idxU + (idxU / 4);
        dataBuffer.position(idxV);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                            GLES20.GL_LUMINANCE, width/2, height/2,
                            0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                            dataBuffer);
        //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //返回纹理ID
        int[] result = new int[3];
        result[0] = _samplerYTexture[0];
        result[1] = _samplerUTexture[0];
        result[2] = _samplerVTexture[0];
        return result;
    }


    public static int loadBitmapTexture(String bitmap_path){
        final int[] textureObjectIds = new int[1];
        GLES20.glGenTextures(1, textureObjectIds, 0);
        if(textureObjectIds[0] == 0){
            Log.e(TAG,"Could not generate a new OpenGL texture object!");
            return 0;
        }

        try {
            FileInputStream fis = new FileInputStream(bitmap_path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap= BitmapFactory.decodeStream(fis,null,options);
            Matrix matrix = new Matrix();
            matrix.setRotate(180, (float) bitmap.getWidth()/2, (float) bitmap.getHeight()/2);
            Bitmap tempBm = Bitmap.createBitmap(bitmap, 0, 0,
                                                bitmap.getWidth(), bitmap.getHeight(),
                                                matrix, true);
            if (bitmap != tempBm) {
                // bitmap回收
                bitmap.recycle();
                bitmap = tempBm;
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            return textureObjectIds[0];
        }
    }

    /**
     * 从原生文件加载纹理图片
     * @param context
     * @param resourceId
     * @return
     */
    public static int loadTexture(Context context, int resourceId){
        final int[] textureObjectIds = new int[1];
        GLES20.glGenTextures(1, textureObjectIds, 0);

        if(textureObjectIds[0] == 0){
            Log.e(TAG,"Could not generate a new OpenGL texture object!");
            return 0;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   //指定需要的是原始数据，非压缩数据
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if(bitmap == null){
            Log.e(TAG, "Resource ID "+resourceId + "could not be decode");
            GLES20.glDeleteTextures(1, textureObjectIds, 0);
            return 0;
        }

        //告诉OpenGL后面纹理调用应该是应用于哪个纹理对象
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);

        //设置缩小的时候（GL_TEXTURE_MIN_FILTER）使用mipmap三线程过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        //设置放大的时候（GL_TEXTURE_MAG_FILTER）使用双线程过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //Android设备y坐标是反向的，正常图显示到设备上是水平颠倒的，解决方案就是设置纹理包装，纹理T坐标（y）设置镜面重复
        //ball读取纹理的时候  t范围坐标取正常值+1
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();

        //快速生成mipmap贴图
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        //解除纹理操作的绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureObjectIds[0];
    }

}
