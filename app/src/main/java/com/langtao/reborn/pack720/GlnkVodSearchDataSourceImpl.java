package com.langtao.reborn.pack720;

import android.os.Message;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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


    /*
	 *  响应下载pano文件回调
	 *  @param type  1:开始下载  2：下载中 3：下载完成  4: 从设备获取下载进度（注意：当type为1和3的时候，data的数据无意义,去掉错误 type = 4,仅供调试，为从设备下载数据进度。）
	 *  @param result   错误码
	 *  @param filename   文件名称
	 *  @param data   文件数据
	 *  @param datalen   type = 1时 为 文件总大小，type = 2时为dadta的长度 ,type = 4时，为从设备下载的进度，比如为50则为50%
	 *
	 *  */

    /* result 错误码为
     * enum FILE_DATA_ERR
     * {
     *     FILE_DATA_ERR_SUCCESS	= 0,  	//成功
     *     FILE_DATA_ERR_PARAM   	= 1,	//转入的参数有错
     *     FILE_DATA_ERR_FILE_LEN   = 2,	//APP的sdk接受到的数据跟设备统计的数据不一致。
     *     FILE_DATA_ERR_GID		= 3,	//下载的模板跟gid不一致。
     *     FILE_DATA_ERR_MEM		= 4,	//内存不足
     *     FILE_DATA_ERR_LEN		= 5,	//模板长度不对
     *     FILE_DATA_ERR_KEY		= 6,	//模板key不对
     *     FILE_DATA_ERR_DEV_ERR	= 7,	//设备返回下载失败。
     *     FILE_DATA_ERR_MOUDLE1	= 8,	//模板数据非法2 (出错时请提供错误码)
     *     FILE_DATA_ERR_MOUDLE2	= 9,	//模板数据非法3 (出错时请提供错误码)
     * };
     *
     *  */
    DataOutputStream out;
    int wirte_PanoDataLen = 0;
    int callback_TotalSize = 0;
    @Override
    protected void onPanoFileDownload(int type, int result, byte[] filename, byte[] data, int datalen) {
        if(result!=0){
            if(mHandler!=null) {
                Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
                message.obj = "VodSearch onPanoFileDownload resultCode : "+result+"\n";
                mHandler.sendMessage(message);
            }
            if(out!=null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mHandler.sendEmptyMessage(mHandler.VIDEO_DISCONNECT);
                }
            }
            return;
        }

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
            wirte_PanoDataLen += data.length;
            try {
                out.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(type ==3)//下载完成
        {
            callback_TotalSize = datalen;
            try {
                out.flush();
                out.close();
                if(mHandler!=null) {
                    Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
                    message.obj = "VodSearch onPanoFileDownload 下载结束 type: "+type+"\n" +
                            "type = 3 返回的文件总大小："+callback_TotalSize+"\n" +
                            "type = 2 共写入的数据大小："+wirte_PanoDataLen +"\n" ;
                    mHandler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mHandler.sendEmptyMessage(mHandler.VIDEO_DISCONNECT);
            }
        }
        else if(type ==4) //下载进度
        {
            if(mHandler!=null) {
                Message message = mHandler.obtainMessage(mHandler.LOG_MSG);
                message.obj = "VodSearch onPanoFileDownload 下载进度 type : "+datalen+"\n";
                mHandler.sendMessage(message);
            }
        }
    }

    @Override
    public void onPermision(int i) {

    }

}
