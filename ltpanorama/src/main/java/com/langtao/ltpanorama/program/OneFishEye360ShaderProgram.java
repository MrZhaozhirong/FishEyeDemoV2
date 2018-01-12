package com.langtao.ltpanorama.program;

import android.opengl.GLES20;

/**
 * Created by zzr on 2017/7/25.
 */

public class OneFishEye360ShaderProgram extends ShaderProgram {

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
    private static final String SAMPLER_RGB = "SamplerRGB";
    private static final String COLOR_CONVERSION_MATRIX = "colorConversionMatrix";
    private static final String IMAGE_MODE = "imageMode";
    public int uLocationSamplerY;
    public int uLocationSamplerU;
    public int uLocationSamplerV;
    public int uLocationSamplerRGB;
    public int uLocationCCM;
    public int uLocationImageMode;

    //two rectangle 增加
    private static final String IS_TWO_RECTANGLE = "isTwoRectangle";
    private static final String OFFSET = "offset";
    private static final String CALA = "cala";
    private static final String FACTORA = "factorA";
    private static final String FACTORB = "factorB";
    private static final String FACTORC = "factorC";
    private static final String FACTORD = "factorD";
    private static final String FACTORE = "factorE";
    private static final String FACTORF = "factorF";
    private static final String FACTORG = "factorG";
    private static final String FACTORH = "factorH";
    private static final String FACTORI = "factorI";
    private static final String FACTORJ = "factorJ";
    private static final String FACTORK = "factorK";
    private static final String FACTORL = "factorL";

    public int isTwoRectangle;
    public int offset;
    public int cala ;
    public int factorA ;
    public int factorB ;
    public int factorC ;
    public int factorD ;
    public int factorE ;
    public int factorF ;
    public int factorG ;
    public int factorH ;
    public int factorI ;
    public int factorJ ;
    public int factorK ;
    public int factorL ;




    public OneFishEye360ShaderProgram() {
        super(vertexShaderResource, fragmentShaderResource);

        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
        aTexCoordLocation = GLES20.glGetAttribLocation(programId, A_TEXCOORD);
        uMVPMatrixLocation = GLES20.glGetUniformLocation(programId, U_MVP_MATRIX);

        uLocationSamplerY = GLES20.glGetUniformLocation(programId, SAMPLER_Y);
        uLocationSamplerU = GLES20.glGetUniformLocation(programId, SAMPLER_U);
        uLocationSamplerV = GLES20.glGetUniformLocation(programId, SAMPLER_V);
        uLocationSamplerRGB = GLES20.glGetUniformLocation(programId, SAMPLER_RGB);

        uLocationCCM = GLES20.glGetUniformLocation(programId, COLOR_CONVERSION_MATRIX);

        uLocationImageMode = GLES20.glGetUniformLocation(programId, IMAGE_MODE);

        isTwoRectangle = GLES20.glGetUniformLocation(programId, IS_TWO_RECTANGLE);
        offset = GLES20.glGetUniformLocation(programId, OFFSET);
        cala = GLES20.glGetUniformLocation(programId, CALA);
        factorA = GLES20.glGetUniformLocation(programId, FACTORA);
        factorB = GLES20.glGetUniformLocation(programId, FACTORB);
        factorC = GLES20.glGetUniformLocation(programId, FACTORC);
        factorD = GLES20.glGetUniformLocation(programId, FACTORD);
        factorE = GLES20.glGetUniformLocation(programId, FACTORE);
        factorF = GLES20.glGetUniformLocation(programId, FACTORF);
        factorG = GLES20.glGetUniformLocation(programId, FACTORG);
        factorH = GLES20.glGetUniformLocation(programId, FACTORH);
        factorI = GLES20.glGetUniformLocation(programId, FACTORI);
        factorJ = GLES20.glGetUniformLocation(programId, FACTORJ);
        factorK = GLES20.glGetUniformLocation(programId, FACTORK);
        factorL = GLES20.glGetUniformLocation(programId, FACTORL);
    }

    private final static String vertexShaderResource =
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
            "}\n";

    private final static String fragmentShaderResource =
            "precision highp float;\n" +
            "\n" +
            "uniform sampler2D SamplerY;\n" +
            "uniform sampler2D SamplerU;\n" +
            "uniform sampler2D SamplerV;\n" +
            "\n" +
            "uniform sampler2D SamplerRGB;\n" +
            "\n" +
            "uniform int imageMode;\n" +
            "\n" +
            "varying highp vec2 v_textureCoordinate;\n" +
            "\n" +
            "uniform mat3 colorConversionMatrix;\n" +
            "\n" +
            "// 是否双矩形(0:非双矩形 其他:是双矩形)\n" +
            "uniform int isTwoRectangle;\n" +
            "uniform float offset;\n" +
            "uniform float cala;\n" +
            "uniform float factorA;\n" +
            "uniform float factorB;\n" +
            "uniform float factorC;\n" +
            "uniform float factorD;\n" +
            "uniform float factorE;\n" +
            "uniform float factorF;\n" +
            "uniform float factorG;\n" +
            "uniform float factorH;\n" +
            "uniform float factorI;\n" +
            "uniform float factorJ;\n" +
            "uniform float factorK;\n" +
            "uniform float factorL;\n" +
            "\n" +
            "\n" +
            "vec3 yuv2rgb(vec2 textureCoordinate)\n" +
            "{\n" +
            "    vec3 yuv;\n" +
            "    yuv.x = texture2D(SamplerY, textureCoordinate).r;\n" +
            "    yuv.y = texture2D(SamplerU, textureCoordinate).r - 0.5;\n" +
            "    yuv.z = texture2D(SamplerV, textureCoordinate).r - 0.5;\n" +
            "\n" +
            "    return colorConversionMatrix * yuv;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 pos = vec2(0.0, 0.0);\n" +
            "    if (isTwoRectangle == 0)\n" +
            "    {\n" +
            "        pos = v_textureCoordinate;\n" +
            "    }\n" +
            "    else\n" +
            "    {\n" +
            "        if (v_textureCoordinate.y < 0.5) {\n" +
            "            float curr = offset + v_textureCoordinate.x * cala;\n" +
            "            pos.x = factorA + (factorB + factorC * v_textureCoordinate.y) * cos( curr );\n" +
            "            pos.y = factorD + (factorE + factorF * v_textureCoordinate.y) * sin( curr );\n" +
            "        }\n" +
            "        else {\n" +
            "            float curr = offset + (1.0 + v_textureCoordinate.x) * cala;\n" +
            "            pos.x = factorG + (factorH + factorI * v_textureCoordinate.y) * cos( curr );\n" +
            "            pos.y = factorJ + (factorK + factorL * v_textureCoordinate.y) * sin( curr );\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    highp vec3 rgb = vec3(0.0, 0.0, 0.0);\n" +
            "    if (imageMode == 0) {\n" +
            "        // yuv图像数据\n" +
            "        rgb = yuv2rgb(pos);\n" +
            "    } else if (imageMode == 1){\n" +
            "        // rgb图像数据\n" +
            "        rgb = texture2D(SamplerRGB, pos).rgb;\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = vec4(rgb, 1);\n" +
            "}";
}
