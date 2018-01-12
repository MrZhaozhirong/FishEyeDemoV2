package com.langtao.ltpanorama.component;

import com.langtao.ltpanorama.LTRenderManager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by zzr on 2017/12/1.
 */

public class CircularYUVBuffer {
    private boolean DEBUG = false;
    private static final String TAG = LTRenderManager.TAG+"-FrameBuffer";
    private final int BUFF_SIZE = 2;

    private YUVFrame[] framePool = new YUVFrame[BUFF_SIZE+1];
    //private ConcurrentLinkedQueue<YUVFrame> queue = new ConcurrentLinkedQueue<>();
    private LinkedBlockingQueue<YUVFrame> queue = new LinkedBlockingQueue<YUVFrame>(BUFF_SIZE);

    private ReentrantReadWriteLock lock0 = new ReentrantReadWriteLock();

    public CircularYUVBuffer() {
        for(int i =0; i<framePool.length; i++){
            framePool[i] = new YUVFrame();
        }
    }

    private YUVFrame requestFrameFromPool() {
        for (YUVFrame buffer : framePool) {
            if (buffer.isfree) {
                return buffer;
            }
        }
        return null;
    }



    public boolean add(int width, int height,
                       byte[] byYdata, byte[] byUdata, byte[] byVdata) {

        //YUVFrame frame = new YUVFrame();
        YUVFrame frame = requestFrameFromPool();
        if(frame == null) return false;
        frame.setWidth(width);
        frame.setHeight(height);
        frame.setYDataBuffer(byYdata);
        frame.setUDataBuffer(byUdata);
        frame.setVDataBuffer(byVdata);
        frame.isfree = false;
        queue.offer(frame);
        return true;
    }

    public YUVFrame getFrame() {
        if(queue!=null && !queue.isEmpty()){
            //YUVFrame poll = queue.poll();
            //poll.beUsed = true;
            return queue.poll();
        }
        return null;
    }

    public void clear() {
        while (queue!=null && !queue.isEmpty()) {
            YUVFrame frame = queue.poll();
            frame.release();
        }
    }
}
