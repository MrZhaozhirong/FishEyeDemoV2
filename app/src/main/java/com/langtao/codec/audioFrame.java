package com.langtao.codec;

/**
 * Created by zzr on 2017/12/22.
 */

public class audioFrame {
    private byte[] data;
    private long timestamp;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
