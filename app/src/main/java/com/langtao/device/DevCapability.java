package com.langtao.device;

import java.io.Serializable;

/**
 * Created by huangyu on 2017/9/14.
 */
public class DevCapability implements Serializable {

    /** 设备编号 */
    protected String devno;
    /**SD卡 */
    private int SDCard;
    /**0--无，1--摇头，2--带光学变焦 */
    private int PTZ;
    /**鱼眼(0--无，1--有) */
    private int FishEye;
    /**720全景 (0--无，1--有) */
    private int Panorama;
    /**产品形态(1--IPC，2--NVR，3--门铃，4--报警网关) */
    private int ProductType;
    /**电池 */
    private int Battery;
    /**码流情况 */
    private int StreamingClass;
    /**视频编码类型 */
    private int VideoEncoderType;
    /**是否支持回音消除 */
    private int EchoCancel;
    /**全景参数 */
    private String PanoData;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
        .append("设备编号 : ").append(devno).append("\n")
        .append("SD卡 : ").append(SDCard).append("\n")
        .append("PTZ 0--无，1--摇头，2--带光学变焦 : ").append(PTZ).append("\n")
        .append("鱼眼(0--无，1--有) : ").append(FishEye).append("\n")
        .append("720全景 (0--无，1--有) : ").append(Panorama).append("\n")
        .append("产品形态(1--IPC，2--NVR，3--门铃，4--报警网关) : ").append(ProductType).append("\n")
        .append("电池 : ").append(Battery).append("\n")
        .append("码流情况 : ").append(StreamingClass).append("\n")
        .append("视频编码类型 : ").append(VideoEncoderType).append("\n")
        .append("是否支持回音消除 : ").append(EchoCancel).append("\n")
        .append("全景参数 : ").append(PanoData).append("\n");
        return sb.toString();
    }

    public String getDevno() {
        return devno;
    }

    public void setDevno(String devno) {
        this.devno = devno;
    }

    public int getSDCard() {
        return SDCard;
    }

    public void setSDCard(int SDCard) {
        this.SDCard = SDCard;
    }

    public int getPTZ() {
        return PTZ;
    }

    public void setPTZ(int PTZ) {
        this.PTZ = PTZ;
    }

    public int getFishEye() {
        return FishEye;
    }

    public void setFishEye(int fishEye) {
        FishEye = fishEye;
    }

    public int getPanorama() {
        return Panorama;
    }

    public void setPanorama(int panorama) {
        Panorama = panorama;
    }

    public int getProductType() {
        return ProductType;
    }

    public void setProductType(int productType) {
        ProductType = productType;
    }

    public int getBattery() {
        return Battery;
    }

    public void setBattery(int battery) {
        Battery = battery;
    }

    public int getStreamingClass() {
        return StreamingClass;
    }

    public void setStreamingClass(int streamingClass) {
        StreamingClass = streamingClass;
    }

    public int getVideoEncoderType() {
        return VideoEncoderType;
    }

    public void setVideoEncoderType(int videoEncoderType) {
        VideoEncoderType = videoEncoderType;
    }

    public int getEchoCancel() {
        return EchoCancel;
    }

    public void setEchoCancel(int echoCancel) {
        EchoCancel = echoCancel;
    }

    public String getPanoData() {
        return PanoData;
    }

    public void setPanoData(String panoData) {
        PanoData = panoData;
    }
}
