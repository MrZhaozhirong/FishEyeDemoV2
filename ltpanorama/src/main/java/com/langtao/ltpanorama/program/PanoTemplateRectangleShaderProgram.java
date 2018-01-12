package com.langtao.ltpanorama.program;

import android.opengl.GLES20;

/**
 * Created by zzr on 2017/12/7.
 */

public class PanoTemplateRectangleShaderProgram extends ShaderProgram {

    private static final String pano_template_rectangle_vs =
            "attribute vec4 position;\n" +
            "attribute vec2 texCoord;\n" +
            "\n" +
            "varying vec2 v_textureCoordinate;\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    v_textureCoordinate = texCoord;\n" +
            "    gl_Position = position;\n" +
            "}\n";

    private static final String pano_template_fs =
            "precision highp float;\n" +
            "\n" +
            "uniform int textureType;\n" +
            "uniform sampler2D SamplerY;\n" +
            "uniform sampler2D SamplerU;\n" +
            "uniform sampler2D SamplerV;\n" +
            "uniform sampler2D SamplerRGB;\n" +
            "\n" +
            "uniform sampler2D SamplerMap1;\n" +
            "uniform sampler2D SamplerMap2;\n" +
            "uniform sampler2D SamplerWeight;\n" +
            "\n" +
            "varying highp vec2 v_textureCoordinate;\n" +
            "\n" +
            "mat3 colorConversionMatrix = mat3(1.0, 1.0, 1.0,\n" +
            "                                  0.0, -0.39465, 2.03211,\n" +
            "                                  1.13983, -0.58060, 0.0);\n" +
            "\n" +
            "\n" +
            "vec3 getTexRGB(vec2 textureCoordinate)\n" +
            "{\n" +
            "    vec3 rgb = vec3(0.0, 0.0, 0.0);\n" +
            "    if (textureType == 0)\n" +
            "    {\n" +
            "        vec3 yuv;\n" +
            "        yuv.x = texture2D(SamplerY, textureCoordinate).r;\n" +
            "        yuv.y = texture2D(SamplerU, textureCoordinate).r - 0.5;\n" +
            "        yuv.z = texture2D(SamplerV, textureCoordinate).r - 0.5;\n" +
            "        \n" +
            "        rgb = colorConversionMatrix * yuv;\n" +
            "    }\n" +
            "    else if (textureType == 1)\n" +
            "    {\n" +
            "        rgb = texture2D(SamplerRGB, textureCoordinate).rgb;\n" +
            "    }\n" +
            "    return rgb;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 p1 = texture2D(SamplerMap1, v_textureCoordinate);\n" +
            "    vec4 p2 = texture2D(SamplerMap2, v_textureCoordinate);\n" +
            "    vec4 w  = texture2D(SamplerWeight, v_textureCoordinate);\n" +
            "    \n" +
            "    vec2 pos1 = vec2((p1.x * 256.0 + p1.y) / 256.0, (p1.z * 256.0 + p1.w) / 256.0);\n" +
            "    vec2 pos2 = vec2((p2.x * 256.0 + p2.y) / 256.0, (p2.z * 256.0 + p2.w) / 256.0);\n" +
            "    vec2 weight = vec2((w.x * 256.0 + w.y) / 256.0, (w.z * 256.0 + w.w) / 256.0);\n" +
            "    \n" +
            "    vec3 clr1 = getTexRGB(pos1);\n" +
            "    vec3 clr2 = getTexRGB(pos2);\n" +
            "\n" +
            "    gl_FragColor = vec4(weight.x * clr1 + weight.y * clr2, 1.0);\n" +
            "}\n";


    public int mPositionLoc;
    public int mTexCoordLoc;

    public int mTextureTypeLoc;
    public int mSamplerYLoc;
    public int mSamplerULoc;
    public int mSamplerVLoc;
    public int mSamplerRGBLoc;

    public int mSamplerMap1Loc;
    public int mSamplerMap2Loc;
    public int mSamplerWeightLoc;

    public PanoTemplateRectangleShaderProgram() {
        super(pano_template_rectangle_vs, pano_template_fs);

        mPositionLoc = GLES20.glGetAttribLocation(programId, "position");
        mTexCoordLoc = GLES20.glGetAttribLocation(programId, "texCoord");

        mTextureTypeLoc = GLES20.glGetUniformLocation(programId, "textureType");
        mSamplerYLoc = GLES20.glGetUniformLocation(programId, "SamplerY");
        mSamplerULoc = GLES20.glGetUniformLocation(programId, "SamplerU");
        mSamplerVLoc = GLES20.glGetUniformLocation(programId, "SamplerV");
        mSamplerRGBLoc = GLES20.glGetUniformLocation(programId, "SamplerRGB");

        mSamplerMap1Loc = GLES20.glGetUniformLocation(programId, "SamplerMap1");
        mSamplerMap2Loc = GLES20.glGetUniformLocation(programId, "SamplerMap2");
        mSamplerWeightLoc = GLES20.glGetUniformLocation(programId, "SamplerWeight");
    }

}
