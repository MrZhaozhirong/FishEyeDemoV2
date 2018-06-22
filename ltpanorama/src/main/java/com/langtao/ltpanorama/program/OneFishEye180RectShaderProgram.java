package com.langtao.ltpanorama.program;

import android.opengl.GLES20;

/**
 * Created by zzr on 2017/7/26.
 */

public class OneFishEye180RectShaderProgram extends ShaderProgram{

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
    private static final String SAMPLER_RGB = "SamplerRGB";
    private static final String IMAGE_MODE = "imageMode";
    public int uLocationSamplerRGB;
    public int uLocationImageMode;


    public OneFishEye180RectShaderProgram() {
        super(fisheye_180_rect_vertex_shader, fisheye_180_rect_fragment_shader);

        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
        aTexCoordLocation = GLES20.glGetAttribLocation(programId, A_TEXCOORD);
        uMVPMatrixLocation = GLES20.glGetUniformLocation(programId, U_MVP_MATRIX);

        uLocationSamplerY = GLES20.glGetUniformLocation(programId, SAMPLER_Y);
        uLocationSamplerU = GLES20.glGetUniformLocation(programId, SAMPLER_U);
        uLocationSamplerV = GLES20.glGetUniformLocation(programId, SAMPLER_V);
        uLocationCCM = GLES20.glGetUniformLocation(programId, COLOR_CONVERSION_MATRIX);

        uLocationSamplerRGB = GLES20.glGetUniformLocation(programId, SAMPLER_RGB);
        uLocationImageMode = GLES20.glGetUniformLocation(programId, IMAGE_MODE);

    }

    private final static String fisheye_180_rect_vertex_shader =
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

    private final static String fisheye_180_rect_fragment_shader =
            "precision highp float;\n" +
            "\n" +
            "uniform sampler2D SamplerY;\n" +
            "uniform sampler2D SamplerU;\n" +
            "uniform sampler2D SamplerV;\n" +
            "\n" +
            "uniform sampler2D SamplerRGB;\n" +
            "uniform int imageMode;\n" +
            "\n" +
            "uniform mat3 colorConversionMatrix;\n" +
            "\n" +
            "varying highp vec2 v_textureCoordinate;\n" +
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
            "    highp vec3 rgb = vec3(0.0, 0.0, 0.0);\n" +
            "    if (imageMode == 0) {\n" +
            "        // yuv图像数据\n" +
            "        rgb = yuv2rgb(v_textureCoordinate);\n" +
            "    } else if (imageMode == 1){\n" +
            "        // rgb图像数据\n" +
            "        rgb = texture2D(SamplerRGB, v_textureCoordinate).rgb;\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = vec4(rgb, 1.0);\n" +
            "}";

}
