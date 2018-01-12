package com.langtao.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by shanlin on 2016/3/24.
 */
public class _TLV_V_VideoModeRequest {
    public int deviceId = 0;
    public short channel;
    public short reserve = 0;
    public int mode;


    public _TLV_V_VideoModeRequest(){

    }

    public byte[] serialize(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(12);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(deviceId);
        byteBuffer.putShort(channel);
        byteBuffer.putShort(reserve);
        byteBuffer.putInt(mode);
        return byteBuffer.array();
    }
}
