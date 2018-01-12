package com.langtao.ltpanorama.program;

import android.opengl.GLES20;

/**
 * Created by ZZR on 2017/2/10.
 */

public class TextureShaderProgram extends ShaderProgram {


    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    public final int uMatrixLocation;
    public final int uTextureUnitLocation;

    protected static final String A_POSITION = "a_Position";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    public final int aPositionLocation;
    public final int aTextureCoordinatesLocation;


    public TextureShaderProgram() {
        super(vertexShaderResource, fragmentShaderResource);

        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX);
        uTextureUnitLocation = GLES20.glGetUniformLocation(programId, U_TEXTURE_UNIT);

        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
        aTextureCoordinatesLocation = GLES20.glGetAttribLocation(programId, A_TEXTURE_COORDINATES);
    }

    private final static String vertexShaderResource =
            "uniform mat4 u_Matrix;\n" +
            "\n" +
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TextureCoordinates;\n" +
            "\n" +
            "varying vec2 v_TextureCoordinates;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    v_TextureCoordinates = a_TextureCoordinates;\n" +
            "    gl_Position = u_Matrix * a_Position;\n" +
            "}";

    private final static String fragmentShaderResource =
                    "precision mediump float;\n" +
                    "\n" +
                    "uniform sampler2D u_TextureUnit;\n" +
                    "varying vec2 v_TextureCoordinates;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates);\n" +
                    "}";
}
