package com.langtao.ltpanorama.program;

import android.opengl.GLES20;

/**
 * Created by nicky on 2017/6/8.
 */

public class PanoNewShaderProgram extends ShaderProgram{



    private static final String A_POSITION = "position";
    public int aPositionLocation;
    private static final String A_TEXCOORD = "texCoord";
    public int aTexCoordLocation;
    private static final String U_MVP_MATRIX = "modelViewProjectionMatrix";
    public int uMVPMatrixLocation;


    private static final String SAMPLER_RGB = "SamplerRGB";
    public int uLocationSamplerRGB;

    public PanoNewShaderProgram() {
        super(vertexShaderResource, fragmentShaderResource);

        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
        aTexCoordLocation = GLES20.glGetAttribLocation(programId, A_TEXCOORD);
        uMVPMatrixLocation= GLES20.glGetUniformLocation(programId, U_MVP_MATRIX);

        uLocationSamplerRGB = GLES20.glGetUniformLocation(programId, SAMPLER_RGB);
    }

    final static String vertexShaderResource =
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

    final static String fragmentShaderResource =
            "precision highp float;\n" +
            "\n" +
            "uniform sampler2D SamplerRGB;\n" +
            "\n" +
            "varying highp vec2 v_textureCoordinate;\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    highp vec3 rgb = texture2D(SamplerRGB, v_textureCoordinate).rgb;\n" +
            "    gl_FragColor = vec4(rgb, 1.0);\n" +
            "}";
}
