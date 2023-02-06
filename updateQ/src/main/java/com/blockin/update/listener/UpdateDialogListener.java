package com.blockin.update.listener;

/**
 * Created by YJQ.
 * Date: 2020-06-26
 */
public interface UpdateDialogListener {

    /**
     * 强制退出，回调给app处理退出应用
     */
    void forceExit();

    /**
     * 点击立即更新，用来开启权限检查和下载服务
     */
    void updateDownLoad();

    /**
     * 重试按钮，进行重新下载
     */
    void updateRetry();

    /**
     * 若应用下载失败，可以选择去应用市场下载或者去浏览器下载
     */
    void downFromBrowser();

    /**
     * 取消更新
     */
    void cancelUpdate();

    /**
     * 重新安装
     */
    void installApkAgain();
}
