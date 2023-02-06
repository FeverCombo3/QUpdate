package com.blockin.update;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.text.TextUtils;
import android.widget.Toast;

import com.blockin.update.download.DownloadHandler;
import com.blockin.update.download.DownloadObserver;
import com.blockin.update.entity.UpdateEntity;
import com.blockin.update.listener.UpdateDialogListener;
import com.blockin.update.log.UpdateLog;
import com.blockin.update.ui.UpdateDialogFragment;
import com.blockin.update.util.UpdateStategyUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Created by YJQ.
 * Date: 2020-06-26
 */
public class UpdateManger implements UpdateDialogListener {

    private static final String TAG = "UpdateManager";

    /**
     * 是否自动安装
     */
    public static boolean isAuoInstall;
    /**
     * context 弱引用
     */
    private WeakReference<Context> wrfContext;

    /**
     * 系统DownloadManager
     */
    private DownloadManager downloadManager;

    /**
     * 上次下载的Id
     */
    private long lastDownloadId = -1;

    /**
     * 更新实体类
     */
    private UpdateEntity updateEntity;
    /**
     * 下载监听
     */
    private DownloadObserver downloadObserver;

    /**
     * 更新提示弹窗
     */
    private UpdateDialogFragment updateDialog;

    /**
     * 开始更新
     * @param context context
     * @param updateEntity 更新实体类
     */
    public void startUpdate(Context context,UpdateEntity updateEntity){
        wrfContext = new WeakReference<>(context);
        if(context == null){
            throw new NullPointerException("UpdateManager======context cannot be null");
        }

        if(updateEntity == null){
            throw new NullPointerException("UpdateManager======updateEntity cannot be null");
        }

        this.updateEntity = updateEntity;
        isAuoInstall = updateEntity.isSilentMode();

        Bundle bundle = new Bundle();
        bundle.putParcelable("updateEntity", updateEntity);

        updateDialog = UpdateDialogFragment.newInstance(bundle).addUpdateListener(this);
        updateDialog.show(((FragmentActivity) context).getSupportFragmentManager(), "UpdateManager");
    }

    private void downLoadApk(){
        try {
            Context context = wrfContext.get();
            if (context != null) {
                if (!downLoadMangerIsEnable(context)) {
                    downFromBrowser();
                    return;
                }
                // 获取下载管理器
                downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                clearCurrentTask();
                // 下载地址如果为null,抛出异常
                String downloadUrl = Objects.requireNonNull(updateEntity.getDownloadUrl());
                Uri uri = Uri.parse(downloadUrl);
                DownloadManager.Request request = new DownloadManager.Request(uri);

                // 下载中和下载完成显示通知栏
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                UpdateLog.d("Save Path : " + updateEntity.getSavePath());
                if (TextUtils.isEmpty(updateEntity.getSavePath())) {
                    //使用系统默认的下载路径 此处为应用内 /android/data/packages ,所以兼容7.0
                    request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, context.getPackageName() + ".apk");
                    deleteApkFile(Objects.requireNonNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + context.getPackageName() + ".apk")));
                } else {
                    // 自定义的下载目录,注意这是涉及到android Q的存储权限，建议不要用getExternalStorageDirectory（）
                    request.setDestinationInExternalFilesDir(context, updateEntity.getSavePath(), context.getPackageName() + ".apk");
                    deleteApkFile(Objects.requireNonNull(context.getExternalFilesDir(updateEntity.getSavePath() + File.separator + context.getPackageName() + ".apk")));
                }
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                // 部分机型（暂时发现Nexus 6P）无法下载，猜测原因为默认下载通过计量网络连接造成的，通过动态判断一下
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    boolean activeNetworkMetered = connectivityManager.isActiveNetworkMetered();
                    request.setAllowedOverMetered(activeNetworkMetered);
                }
                request.allowScanningByMediaScanner();
                // 设置通知栏的标题
                request.setTitle(getAppName());
                // 设置通知栏的描述
                request.setDescription(context.getString(R.string.versionchecklib_downloading));
                // 设置媒体类型为apk文件
                request.setMimeType("application/vnd.android.package-archive");
                // 开启下载，返回下载id
                lastDownloadId = downloadManager.enqueue(request);
                // 如需要进度及下载状态，增加下载监听
                if (!updateEntity.isSilentMode()) {
                    DownloadHandler downloadHandler = new DownloadHandler(this);
                    downloadObserver = new DownloadObserver(downloadHandler, downloadManager, lastDownloadId);
                    context.getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, downloadObserver);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 防止有些厂商更改了系统的downloadManager
            downloadFromBrowse();
        }
    }

    /**
     * 检查本地是否有已经下载的最新apk文件
     *
     * @param filePath 文件相对路劲
     */
    private File checkLocalUpdate(String filePath,String updateVersionName) {
        try {
            Context context = wrfContext.get();
            File apkFile;
            if (TextUtils.isEmpty(filePath)) {
                apkFile = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + context.getPackageName() + ".apk");
            } else {
                apkFile = context.getExternalFilesDir(filePath + File.separator + context.getPackageName() + ".apk");
            }
            // 注意系统的getExternalFilesDir（）方法如果找不到文件会默认当成目录创建
            if (apkFile != null && apkFile.isFile()) {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
                if (packageInfo != null) {
                    long apkVersionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? packageInfo.getLongVersionCode() : packageInfo.versionCode;

                    UpdateLog.d("apkVersionCode : " + apkVersionCode + " App" + getAppCode());
                    UpdateLog.d("apkVersionName : " + packageInfo.versionName + " UpdateVersionName" + updateVersionName);
                    if (apkVersionCode > getAppCode() && TextUtils.equals(updateVersionName,packageInfo.versionName)) {
                        return apkFile;
                    }
                }
            }
        } catch (Exception e) {
            UpdateLog.d("checkLocalUpdate:本地目录没有已经下载的新版本");
        }
        return null;
    }

    /**
     * downloadManager 是否可用
     *
     * @param context 上下文
     * @return true 可用
     */
    private boolean downLoadMangerIsEnable(Context context) {
        int state = context.getApplicationContext().getPackageManager()
                .getApplicationEnabledSetting("com.android.providers.downloads");
        return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED);
    }


    /**
     * 清除上一个任务，防止apk重复下载
     */
    public void clearCurrentTask() {
        try {
            if (lastDownloadId != -1) {
                downloadManager.remove(lastDownloadId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载前清空本地缓存的文件
     */
    private void deleteApkFile(File destFileDir) {
        if (!destFileDir.exists()) {
            return;
        }
        if (destFileDir.isDirectory()) {
            File[] files = destFileDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteApkFile(f);
                }
            }
        }
        destFileDir.delete();
    }

    /**
     * 获取应用程序名称
     *
     * @return 应用名称
     */
    private String getAppName() {
        try {
            Context context = wrfContext.get();
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wrfContext.get().getString(R.string.versionchecklib_update);
    }

    /**
     * 获取应用的版本号
     *
     * @return 应用版本号
     */
    private long getAppCode() {
        try {
            Context context = wrfContext.get();
            //获取包管理器
            PackageManager pm = context.getPackageManager();
            //获取包信息
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //返回版本号
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 从浏览器打开下载，如果浏览器地址是空就直接打开下载链接
     */
    private void downloadFromBrowse() {
        try {
            String downloadUrl = TextUtils.isEmpty(updateEntity.getDownBrowserUrl()) ? updateEntity.getDownloadUrl() : updateEntity.getDownBrowserUrl();
            Intent intent = new Intent();
            Uri uri = Uri.parse(downloadUrl);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            wrfContext.get().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            UpdateLog.d( "无法通过浏览器下载！");
        }
    }

    /**
     * 设置下载的进度
     *
     * @param progress 进度
     */
    public void setProgress(int progress) {
        if (updateDialog != null) {
            updateDialog.setProgress(progress);
        }
    }

    /**
     * 取消监听
     */
    public void unregisterContentObserver(){
        if(wrfContext.get() != null){
            wrfContext.get().getContentResolver().unregisterContentObserver(downloadObserver);
        }
    }

    /**
     * 显示错误界面
     */
    public void showFail(){
        if (updateDialog != null) {
            updateDialog.showFailBtn();
        }
    }

    /**
     * 关闭更新提示弹框
     */
    private void dismissDialog() {
        if (updateDialog != null && updateDialog.isShowing && wrfContext.get() != null && !((Activity) wrfContext.get()).isFinishing()) {
            updateDialog.dismiss();
        }
    }

    @Override
    public void forceExit() {
        dismissDialog();
        if (wrfContext.get() != null) {
            wrfContext.get().startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
            ((Activity) wrfContext.get()).finish();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    @Override
    public void updateDownLoad() {
        // 立即更新
        File apkFile = checkLocalUpdate(updateEntity.getSavePath(),updateEntity.getVersionCode());
        if (apkFile != null) {
            // 本地存在新版本，直接安装
            installApp(apkFile);
        } else {
            // 不存在新版本，需要下载
            if (!updateEntity.isSilentMode()) {
                // 非静默模式，直接在下载更新框内部显示下载进度
                updateDialog.showProgressBtn();
            } else {
                // 静默模式，不显示下载进度
                dismissDialog();
            }
            // 开启下载
            downLoadApk();
        }
    }

    @Override
    public void updateRetry() {

    }

    @Override
    public void downFromBrowser() {
        // 从浏览器下载
        downloadFromBrowse();
    }

    @Override
    public void cancelUpdate() {
        // 取消更新
        clearCurrentTask();
        dismissDialog();
        if (UpdateStategyUtils.TYPE_FORCE_UPDATE == updateEntity.getForceUpdate()) {
            forceExit();
        }
    }

    @Override
    public void installApkAgain() {
        Context context = wrfContext.get();
        if (context != null) {
            try {
                UpdateLog.d("installApkAgain");
                File downloadFile = checkLocalUpdate(updateEntity.getSavePath(),updateEntity.getVersionCode());
                if(downloadFile != null){
                    UpdateLog.d("installApkAgain downloadFile != NULL");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        intent.setDataAndType(Uri.fromFile(downloadFile), "application/vnd.android.package-archive");
                    } else {
                        //Android7.0之后获取uri要用contentProvider
                        Uri apkUri = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName() + ".fileProvider", downloadFile);
                        //Granting Temporary Permissions to a URI
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.versionchecklib_click_notification, Toast.LENGTH_SHORT).show();
            } finally {
                dismissDialog();
            }
        }
    }

    /**
     * 安装app
     *
     * @param apkFile 下载的文件
     */
    public void installApp(File apkFile) {
        try {
            Context context = wrfContext.get();
            // 安装
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    boolean allowInstall = context.getPackageManager().canRequestPackageInstalls();
                    if (!allowInstall) {
                        //不允许安装未知来源应用，请求安装未知应用来源的权限
                        if (updateDialog != null) {
                            updateDialog.requestInstallPermission();
                        }
                        return;
                    }
                }
                //Android7.0之后获取uri要用contentProvider
                Uri apkUri = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName() + ".fileProvider", apkFile);
                //Granting Temporary Permissions to a URI
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            dismissDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取下载的文件
     *
     * @return file
     */
    public File getDownloadFile() {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = downloadManager.query(query.setFilterById(lastDownloadId));
        if (cursor != null && cursor.moveToFirst()) {
            String fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String apkPath = Uri.parse(fileUri).getPath();
            if (!TextUtils.isEmpty(apkPath)) {
                return new File(apkPath);
            }
            cursor.close();
        }
        return null;
    }

}
