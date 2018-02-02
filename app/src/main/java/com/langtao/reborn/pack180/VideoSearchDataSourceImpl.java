package com.langtao.reborn.pack180;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import glnk.client.DataSourceListener2;


/**
 * Created by zzr on 2018/1/30.
 */

public class VideoSearchDataSourceImpl extends DataSourceListener2 {

    private static final String TAG = "VideoSearch-180";
    private Handler handler;
    public VideoSearchDataSourceImpl(LT180Handler handler){
        this.handler = handler;
    }

    @Override
    public void onPermision(int i) {

    }


    @Override
    public void onAuthorized(int result) {
        if (result == 1) {// 登录成功
            Log.w(TAG, "连接设备成功，开始搜索录像文件。");
            handler.sendEmptyMessage(LT180Handler.START_VIDEO_SEARCH);
        } else { // 登录失败，关闭连接
            Log.w(TAG, "连接设备失败。");
        }
    }



    private ArrayList<String> list = new ArrayList<String>();
    private int fileCount = -1;
    private int Count = -1;

    @Override
    public void onRemoteFileSearchResp2(int result, int count) {
        Count = 0;
        fileCount = count;
        Log.w(TAG, " 录像文件数为 ："+fileCount);
    }

    @Override
    protected void onRemoteFileSearchItem2(int recordType, int startYear, int startMonth, int startDay, int
            startHour, int startMinute, int startSecond, int startMs, int endYear, int endMonth, int endDay, int
            endHour, int endMinute, int endSecond, int endMs) {

        String fitem = String.format(Locale.getDefault(),
                        "%s,%d," +
                        "%d:%d:%d:%d:%d:%d," +
                        "%d:%d:%d:%d:%d:%d",
                        "" + Count, recordType,
                        startYear, startMonth, startDay, startHour, startMinute, startSecond,
                        endYear, endMonth, endDay, endHour, endMinute, endSecond );
        Log.w(TAG, "fitem=========" + fitem);
        Count++;
        list.add(fitem);

        if (fileCount != -1 && list.size() == fileCount) {
            Log.i(TAG, "已结完成了全部录像文件的回调，开始请求播放");
            handler.sendEmptyMessage(LT180Handler.STOP_VIDEO_SEARCH);

            Message msg1 = new Message();
            msg1.what = LT180Handler.REQUEST_PLAY_VIDEO;
            msg1.obj = list.get(0);
            handler.sendMessage(msg1);
        }
    }
}
