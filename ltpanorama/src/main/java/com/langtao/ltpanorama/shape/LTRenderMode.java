package com.langtao.ltpanorama.shape;

/**
 * Created by zzr on 2017/8/21.
 */

public class LTRenderMode {

    public static final int CRUISE_LEFT = 0;
    public static final int CRUISE_RIGHT = 1;

    public static final int RENDER_MODE_360 = 360;
    public static final int RENDER_MODE_FOUR_EYE = 360*4;       //1440
    public static final int RENDER_MODE_TWO_RECTANGLE = 490;    //70*70
    public static final int RENDER_MODE_CYLINDER = 360*2/3;     //540

    public static final int RENDER_MODE_180 = 180;
    public static final int RENDER_MODE_180_PAVED = 180+1;

    public static final int RENDER_MODE_CRYSTAL = 720;  //水晶球
    public static final int RENDER_MODE_NORMAL = 720+1; //普通透视
    public static final int RENDER_MODE_FISHEYE = 720+2;//鱼眼
    public static final int RENDER_MODE_PLANET = 720+3; //小行星
    public static final int RENDER_MODE_VR = 720+5;     //vr 要占用4 5两个值

}
