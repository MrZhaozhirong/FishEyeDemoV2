package com.langtao.codec;

/**
 * Created by zzr on 2017/12/22.
 */

public class videoFrame {
    private byte[] data;
    private int frameIndex;
    private long timestamp;
    private boolean isIFrame;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isIFrame() {
        return isIFrame;
    }

    public void setIFrame(boolean IFrame) {
        isIFrame = IFrame;
    }
}
