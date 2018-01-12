package com.langtao.reborn.pack720;

import android.os.Message;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import glnk.client.DataSourceListener2;

/**
 * Created by zzr on 2017/12/7.
 */

public class GlnkVodSearchDataSourceImpl extends DataSourceListener2 {

    private LT720Handler mHandler;
    WeakReference<LangTao720Activity> mActivity;

    public GlnkVodSearchDataSourceImpl(LT720Handler handler,LangTao720Activity activity) {
        mHandler =  handler;
        mActivity = new WeakReference<LangTao720Activity>(activity);
    }


    @Override
    public void onConnecting() {
        super.onConnecting();
        //回调函数不能做阻塞动作，所有数据都要通过Handler等方式抛到主线程执行。
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "VodSearch  onConnecting ... "+"\n";
            mHandler.sendMessage(message);
        }
    }

    // 连接成功，只是指网络跟设备连上了，尚未发送登录验证消息
    @Override
    public void onConnected(int mode, String ip, int port) {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "VodSearch  onConnected ... "+"\n";
            mHandler.sendMessage(message);
        }
    }

    // 连接成功，跟设备交互登录验证返回，是否密码正确
    @Override
    public void onAuthorized(int result) {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "VodSearch  onAuthorized result : "+result+"\n";
            mHandler.sendMessage(message);
        }
        Message msg = new Message();
        msg.arg1 = result;
        msg.what = mHandler.VIDEO_ONAUTH;// 这个消息会转发 VIDEO_SEARCH 搜索模板文件
        mHandler.sendMessage(msg);
    }

    @Override
    public void onDisconnected(int errcode) {
        if(mHandler!=null) {
            Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
            message.obj = "VodSearch  onDisconnected errCode : "+errcode+"\n";
            mHandler.sendMessage(message);
        }
    }

    DataOutputStream out;
    @Override
    protected void onPanoFileDownload(int type, int totalsize, byte[] filename, byte[] data, int datalen) {
        if(type ==1 )//开始下载
        {
            LangTao720Activity LangTao720Activity = mActivity.get();
            if(LangTao720Activity==null)
                return;
            String configFilePath = LangTao720Activity.getPanoramaConfigFilePath(true);
            if(TextUtils.isEmpty(configFilePath))
                return;
            try {
                out = new DataOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(configFilePath)));

                if(mHandler!=null) {
                    Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
                    message.obj = "VodSearch onPanoFileDownload 开始下载 type: "+type+"\n";
                    mHandler.sendMessage(message);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else if(type == 2)//写入数据
        {
            try {
                out.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(type ==3)//下载完成
        {
            try {
                out.close();
                if(mHandler!=null) {
                    Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
                    message.obj = "VodSearch onPanoFileDownload 下载结束 type: "+type+"\n";
                    mHandler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mHandler.sendEmptyMessage(mHandler.VIDEO_DISCONNECT);
            }
        }
        else if(type ==4) //下载失败
        {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mHandler.sendEmptyMessage(mHandler.VIDEO_DISCONNECT);
            }
        }
    }

    @Override
    public void onPermision(int i) {

    }

}
