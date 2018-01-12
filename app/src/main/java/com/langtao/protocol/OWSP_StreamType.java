package com.langtao.protocol;

/**
 * Created by shanlin on 2016/3/24.
 */
public class OWSP_StreamType {
    public final static int OWSP_STREAM_MAIN = 0; // 请求主码流
    public final static int OWSP_STREAM_SUB = 1; // 请求次码流
    public final static int OWSP_STREAM_VOD = 2; // 请求录像文件
    public final static int OWSP_MODE_SETTING = 3; // 远程配置模式，在此模式下设备不需要发送视频和音频流

    /**Very Low Display resolution  eg:QQVGA */
    public static int OWSP_VIEWMODE_LLD   = 4;	//
    /** Low Display resolution	  eg:QVGA QCIF*/
    public static int OWSP_VIEWMODE_LD    = 5;	//
    /**Stand Display resolution	eg:VGA D1 */
    public static int OWSP_VIEWMODE_SD    = 6;  //
    /** High Display resolution	  eg:720p 1080p*/
    public static int OWSP_VIEWMODE_HD    = 7;


    //一开始请求的类型//码流类型
    public static int GLNK_STREAM_MAIN	    = 0;//(高清)
    public static int GLNK_STREAM_SUB		= 1;//(标清）
    public static int GLNK_STREAM_VOD		= 2;//请求录像文件
    public static int GLNK_MODE_SETTING	    = 3;//通明通道
    public static int GLNK_VIEWMODE_LLD     = 4;//(流畅）
    //切换时是
    public static int GLNK_CHANGE_STREAM_MAIN   = 7;//(高清)
    public static int GLNK_CHANGE_SUB_MAIN      = 6;//(标清）
    public static int GLNK_CHANGE_VIEWMODE_LLD  = 5;//(流畅）

}
