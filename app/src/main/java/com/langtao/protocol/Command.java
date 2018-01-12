package com.langtao.protocol;

public class Command {
	public final static int TLV_T_DEVICE_RESTART_REQ = 465;  //客户端向设备发送重启命令
	public final static int TLV_T_DEVICE_RESTART_RSP = 466;  //设备返回是否接收到重启命令
	public final static int TLV_T_VIDEOMODE_REQ = 431;//switch video stream mode request
	public final static int TLV_T_VIDEOMODE_RSP = 432;//switch video stream mode response

	public final static int TLV_T_CONTROL_VIDEOSTREAM_REQ = 1071;//control video audio stream
	public final static int TLV_T_CONTROL_VIDEOSTREAM_RSP = 1072;


	public final static int TLV_T_LOCK_REQ = 425;//unlock request
	public final static int TLV_T_LOCK_RSP = 426;//unlock request

//	public final static int TLV_T_RENEW_GLOCKPWD_REQ = 477;
//	public final static int TLV_T_RENEW_GLOCKPWD_RSP = 478;

	public final static int TLV_T_RENEW_GLOCKPWD_REQ = 477;
	public final static int TLV_T_RENEW_GLOCKPWD_RSP = 478;


}
