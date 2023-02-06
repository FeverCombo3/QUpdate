package com.blockin.update.download;

import android.app.DownloadManager;
import android.os.Handler;
import android.os.Message;

import com.blockin.update.UpdateManger;
import com.blockin.update.log.UpdateLog;

import java.lang.ref.WeakReference;
import java.util.logging.LogRecord;

/**
 * Created by YJQ.
 * Date: 2020-06-26
 * DownLoadObserver 传递数据到 DownloadHandler 再交给UpdateManager处理
 */
public class DownloadHandler extends Handler {

    private final WeakReference<UpdateManger> wrfUpdateManager;

    public DownloadHandler(UpdateManger updateManger){
        wrfUpdateManager = new WeakReference<>(updateManger);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case DownloadManager.STATUS_PAUSED:
                // 暂停
                break;
            case DownloadManager.STATUS_PENDING:
                // 开始
                break;
            case DownloadManager.STATUS_RUNNING:
                // 下载中
                if (wrfUpdateManager.get() != null) {
                    wrfUpdateManager.get().setProgress(msg.arg1);
                }
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                if (wrfUpdateManager.get() != null) {
                    wrfUpdateManager.get().setProgress(100);
                    wrfUpdateManager.get().unregisterContentObserver();
                }
                if (wrfUpdateManager.get() != null) {
                    wrfUpdateManager.get().installApp(wrfUpdateManager.get().getDownloadFile());
                }
                break;
            case DownloadManager.STATUS_FAILED:
                // 下载失败，清除本次的下载任务
                UpdateLog.d("Handler STATUS_FAILED removeTask");
                if (wrfUpdateManager.get() != null) {
                    wrfUpdateManager.get().clearCurrentTask();
                    wrfUpdateManager.get().unregisterContentObserver();
                    wrfUpdateManager.get().showFail();
                }
                break;
            default:
                break;
        }
    }
}
