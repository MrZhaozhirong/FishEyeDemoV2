package com.langtao.ltpanorama.component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by zzr on 2017/12/1.
 */

public class YUVFrame implements Cloneable {

    private int width;
    private int height;
    private ByteBuffer yDataBuffer;
    private int nYLen;
    private ByteBuffer uDataBuffer;
    private int nULen;
    private ByteBuffer vDataBuffer;
    private int nVLen;
    volatile boolean isFree;



    public YUVFrame() { isFree = true;}

    public void release() {
        this.width=0;
        this.height=0;
        yDataBuffer.clear();
        uDataBuffer.clear();
        vDataBuffer.clear();
        this.isFree = true;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getYDataLen() {
        return nYLen;
    }

    public int getUDataLen() {
        return nULen;
    }

    public int getVDataLen() {
        return nVLen;
    }

    public ByteBuffer getYDataBuffer() {
        return yDataBuffer;
    }

    public ByteBuffer getUDataBuffer() {
        return uDataBuffer;
    }

    public ByteBuffer getVDataBuffer() {
        return vDataBuffer;
    }

    public void setYDataBuffer(byte[] data){
        if(yDataBuffer ==null){
            yDataBuffer = ByteBuffer.allocate(data.length).order(ByteOrder.nativeOrder());
        } else if(data.length > yDataBuffer.capacity() ){
            yDataBuffer = ByteBuffer.allocate(data.length).order(ByteOrder.nativeOrder());
        }
        yDataBuffer.clear();
        yDataBuffer.put(data);
        yDataBuffer.position(0);
        this.nYLen = data.length;
    }

    public void setUDataBuffer(byte[] data){
        if(uDataBuffer ==null){
            uDataBuffer = ByteBuffer.allocate(data.length).order(ByteOrder.nativeOrder());
        } else if(data.length > uDataBuffer.capacity() ){
            uDataBuffer = ByteBuffer.allocate(data.length).order(ByteOrder.nativeOrder());
        }
        uDataBuffer.clear();
        uDataBuffer.put(data);
        uDataBuffer.position(0);
        this.nULen = data.length;
    }

    public void setVDataBuffer(byte[] data){
        if(vDataBuffer ==null){
            vDataBuffer = ByteBuffer.allocate(data.length).order(ByteOrder.nativeOrder());
        } else if(data.length > vDataBuffer.capacity() ){
            vDataBuffer = ByteBuffer.allocate(data.length).order(ByteOrder.nativeOrder());
        }
        vDataBuffer.clear();
        vDataBuffer.put(data);
        vDataBuffer.position(0);
        this.nVLen = data.length;
    }






    public int available() {
        return this.nYLen + this.nULen + this.nVLen;
    }

    public byte[] getYuvbyte(){
        byte[] ret = new byte[available()];
        System.arraycopy(yDataBuffer.array(),0, ret,0,nYLen);
        System.arraycopy(uDataBuffer.array(),0, ret,nYLen,nULen);
        System.arraycopy(vDataBuffer.array(),0, ret,nYLen+nULen,nVLen);
        return ret;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        YUVFrame clone = (YUVFrame) super.clone();
        clone.setYDataBuffer(this.getYDataBuffer().array());
        clone.setUDataBuffer(this.getUDataBuffer().array());
        clone.setVDataBuffer(this.getVDataBuffer().array());
        return clone;
    }
}
