package com.langtao.ltpanorama.program;

import android.opengl.GLES20;

/**
 * Created by zzr on 2017/7/26.
 */

public class OneFishEye180ShaderProgram extends ShaderProgram{

    //vertex
    private static final String A_POSITION = "position";
    private static final String A_TEXCOORD = "texCoord";
    private static final String U_MVP_MATRIX = "modelViewProjectionMatrix";
    public int aPositionLocation;
    public int aTexCoordLocation;
    public int uMVPMatrixLocation;
    //fragment
    private static final String SAMPLER_Y = "SamplerY";
    private static final String SAMPLER_U = "SamplerU";
    private static final String SAMPLER_V = "SamplerV";
    private static final String COLOR_CONVERSION_MATRIX = "colorConversionMatrix";
    public int uLocationSamplerY;
    public int uLocationSamplerU;
    public int uLocationSamplerV;
    public int uLocationCCM;
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String RECT_X = "rectX";
    private static final String RECT_Y = "rectY";
    private static final String RECT_WIDTH = "rectWidth";
    private static final String RECT_HEIGHT = "rectHeight";
    public int uLocationWidth;
    public int uLocationHeight;
    public int uLocationRectX;
    public int uLocationRectY;
    public int uLocationRectWidth;
    public int uLocationRectHeight;


    public OneFishEye180ShaderProgram() {
        super(fisheye_180_vertex_shader, fisheye_180_fragment_shader);

        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
        aTexCoordLocation = GLES20.glGetAttribLocation(programId, A_TEXCOORD);
        uMVPMatrixLocation = GLES20.glGetUniformLocation(programId, U_MVP_MATRIX);

        uLocationSamplerY = GLES20.glGetUniformLocation(programId, SAMPLER_Y);
        uLocationSamplerU = GLES20.glGetUniformLocation(programId, SAMPLER_U);
        uLocationSamplerV = GLES20.glGetUniformLocation(programId, SAMPLER_V);
        uLocationCCM = GLES20.glGetUniformLocation(programId, COLOR_CONVERSION_MATRIX);

        uLocationWidth = GLES20.glGetUniformLocation(programId, WIDTH);
        uLocationHeight = GLES20.glGetUniformLocation(programId, HEIGHT);
        uLocationRectX = GLES20.glGetUniformLocation(programId, RECT_X);
        uLocationRectY = GLES20.glGetUniformLocation(programId, RECT_Y);
        uLocationRectWidth = GLES20.glGetUniformLocation(programId, RECT_WIDTH);
        uLocationRectHeight = GLES20.glGetUniformLocation(programId, RECT_HEIGHT);
    }

    private final static String fisheye_180_vertex_shader =
            "attribute vec4 position;\n" +
            "attribute vec2 texCoord;\n" +
            "\n" +
            "varying vec2 v_textureCoordinate;\n" +
            "\n" +
            "uniform mat4 modelViewProjectionMatrix;\n" +
            "\n" +
            "void main() {\n" +
            "    v_textureCoordinate = texCoord;\n" +
            "    gl_Position = modelViewProjectionMatrix * position;\n" +
            "}";

    private final static String fisheye_180_fragment_shader =
            "precision highp float;\n" +
            "\n" +
            "uniform sampler2D SamplerY;\n" +
            "uniform sampler2D SamplerU;\n" +
            "uniform sampler2D SamplerV;\n" +
            "\n" +
            "varying highp vec2 v_textureCoordinate;\n" +
            "\n" +
            "uniform mat3 colorConversionMatrix;\n" +
            "\n" +
            "\n" +
            "uniform float width;\n" +
            "uniform float height;\n" +
            "uniform float rectX;\n" +
            "uniform float rectY;\n" +
            "uniform float rectWidth;\n" +
            "uniform float rectHeight;\n" +
            "\n" +
            "\n" +
            "vec3 yuv2rgb(vec2 textureCoordinate)\n" +
            "{\n" +
            "    vec3 yuv;\n" +
            "    yuv.x = texture2D(SamplerY, textureCoordinate).r;\n" +
            "    yuv.y = texture2D(SamplerU, textureCoordinate).r - 0.5;\n" +
            "    yuv.z = texture2D(SamplerV, textureCoordinate).r - 0.5;\n" +
            "    \n" +
            "    return colorConversionMatrix * yuv;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    \n" +
            "    highp vec3 rgb;\n" +
            "    \n" +
            "    if (rectWidth > rectHeight) {\n" +
            "        float heightBegin = ((rectWidth - rectHeight) * 0.5) / rectWidth;\n" +
            "        float heightEnd = 1.0 - heightBegin;\n" +
            "        if (v_textureCoordinate.y < heightBegin || v_textureCoordinate.y > heightEnd)\n" +
            "        {\n" +
            "            rgb = vec3(0.0, 0.0, 0.0);\n" +
            "        }\n" +
            "        else\n" +
            "        {\n" +
            "            float dHeight = rectHeight * (v_textureCoordinate.y - heightBegin) / (heightEnd - " +
            "heightBegin);\n" +
            "            float u = (rectX + rectWidth * v_textureCoordinate.x) / width;\n" +
            "            float v = (rectY + dHeight) / height;\n" +
            "            rgb = yuv2rgb(vec2(u, v));\n" +
            "        }\n" +
            "    }\n" +
            "    else if (rectWidth < rectHeight) {\n" +
            "        float widthBegin = ((rectHeight - rectWidth) * 0.5) / rectHeight;\n" +
            "        float widthEnd = 1.0 - widthBegin;\n" +
            "        if (v_textureCoordinate.x < widthBegin || v_textureCoordinate.x > widthEnd)\n" +
            "        {\n" +
            "            rgb = vec3(0.0, 0.0, 0.0);\n" +
            "        }\n" +
            "        else\n" +
            "        {\n" +
            "            float dWidth = rectWidth * (v_textureCoordinate.x - widthBegin) / (widthEnd - widthBegin);\n" +
            "            float u = (rectX + dWidth) / width;\n" +
            "            float v = (rectY + rectHeight * v_textureCoordinate.y) / height;\n" +
            "            rgb = yuv2rgb(vec2(u, v));\n" +
            "        }\n" +
            "    }\n" +
            "    else {\n" +
            "        float u = (rectX + rectWidth * v_textureCoordinate.x) / width;\n" +
            "        float v = (rectY + rectHeight * v_textureCoordinate.y) / height;\n" +
            "        rgb = yuv2rgb(vec2(u, v));\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = vec4(rgb, 1.0);\n" +
            "}";
}
