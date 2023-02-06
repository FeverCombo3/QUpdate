package com.blockin.update.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blockin.update.R;
import com.blockin.update.entity.UpdateEntity;
import com.blockin.update.listener.UpdateDialogListener;
import com.blockin.update.util.UpdateStategyUtils;

import java.util.Objects;

/**
 * Created by YJQ.
 * Date: 2020-06-27
 */
public class UpdateDialogFragment extends BaseDialogFragment {

    /**
     * 8.0未知应用授权请求码
     */
    private static final int INSTALL_PACKAGES_REQUESTCODE = 1112;
    /**
     * 用户跳转未知应用安装的界面请求码
     */
    private static final int GET_UNKNOWN_APP_SOURCES = 1113;
    /**
     * Android 11 外部存储权限适配
     */
    private static final int REQUEST_CODE_STORAGE = 1114;

    //======顶部========//
    /**
     * 顶部图片
     */
    private ImageView mIvTop;
    /**
     * 标题
     */
    private TextView mTvTitle;
    //======更新内容========//
    /**
     * 版本更新内容
     */
    private TextView mTvUpdateInfo;
    /**
     * 版本更新
     */
    private Button mBtnUpdate;

    /**
     * 进度条
     */
    private NumberProgressBar mProgressBar;

    /**
     * 底部关闭
     */
    private LinearLayout mLlClose;
    private ImageView mIvClose;

    /**
     * 网页下载
     */
    private TextView mWebDownload;

    /**
     * 取消更新
     */
    private TextView mCancelUpdate;

    //======更新信息========//
    /**
     * 更新信息
     */
    private UpdateEntity updateEntity;

    /**
     * 监听接口
     */
    private UpdateDialogListener mUpdateDialogListener;

    /**
     * 初始化弹框
     *
     * @param params 参数
     * @return DownloadDialog
     */
    public static UpdateDialogFragment newInstance(Bundle params) {
        UpdateDialogFragment downloadDialog = new UpdateDialogFragment();
        if (params != null) {
            downloadDialog.setArguments(params);
        }
        return downloadDialog;
    }

    /**
     * 回调监听
     *
     * @param updateListener 监听接口
     * @return DownloadDialog
     */
    public UpdateDialogFragment addUpdateListener(UpdateDialogListener updateListener) {
        this.mUpdateDialogListener = updateListener;
        return this;
    }


    @Override
    int getLayoutId() {
        return R.layout.update_dialog_app;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            updateEntity = getArguments().getParcelable("updateEntity");
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (updateEntity == null) {
            dismiss();
            return;
        }

        setCancelable(false);

        //顶部图片
        mIvTop = view.findViewById(R.id.iv_top);
        //标题
        mTvTitle = view.findViewById(R.id.tv_title);
        //提示内容
        mTvUpdateInfo = view.findViewById(R.id.tv_update_info);
        //更新按钮
        mBtnUpdate = view.findViewById(R.id.btn_update);
        //进度
        mProgressBar = view.findViewById(R.id.nbpProgress);
        //网页下载
        mWebDownload = view.findViewById(R.id.web_download);

        //关闭更新弹窗整个布局(相当于取消更新)
        mLlClose = view.findViewById(R.id.ll_close);
        //关闭按钮
        mIvClose = view.findViewById(R.id.iv_close);
        //取消更新
        mCancelUpdate = view.findViewById(R.id.cancel_update);

        //新版本号(1.1.1)
        final String newVersion = updateEntity.getVersionCode();
        //更新内容
        if (!TextUtils.isEmpty(updateEntity.getUpdateInfo())) {
            mTvUpdateInfo.setText(updateEntity.getUpdateInfo());
        }

        if (!TextUtils.isEmpty(newVersion)) {
            mTvTitle.setText(String.format(getString(R.string.versionchecklib_ready_update), newVersion));
        } else {
            mTvTitle.setText(R.string.versionchecklib_newversion_update);
        }

        //强制更新,不显示关闭按钮
        if (updateEntity.getForceUpdate() == UpdateStategyUtils.TYPE_FORCE_UPDATE) {
            mLlClose.setVisibility(View.GONE);
        }

        //点击取消更新按钮，取消更新
        mCancelUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUpdateDialogListener != null) {
                    mUpdateDialogListener.cancelUpdate();
                }
            }
        });

        //点击关闭按钮也取消更新
        mLlClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUpdateDialogListener != null) {
                    mUpdateDialogListener.cancelUpdate();
                }
            }
        });

        /*

          外置sd卡：
          Context.getExternalFilesDir()：SDCard/Android/data/应用包名/files/
          Context.getExternalCacheDir()：SDCard/Android/data/应用包名/cache/
          API<19需要配置权限，API>=19不需要配置权限
          即对于配置了读写权限的app，使用"SDCard/Android/data/应用包名/"读写操作时，
          4.4系统以下因为配置了权限而能正常读写，4.4及以上系统因为不需要权限亦能正常读写。但是为了不配置多余的权限，建议如下写：
          <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
          android:maxSdkVersion="18"/>

          以上文件夹，当App卸载时会被系统自动删除。
          其余sd卡位置，6.0以上需要动态申请读写权限。

          更新使用路径为getExternalFilesDir，不需要动态申请读写权限
          直接进行下载
         */

        mBtnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUpdateDialogListener != null) {
                    toUpdateDownLoad();
                }
            }
        });

        //去网页下载
        mWebDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUpdateDialogListener != null) {
                    mUpdateDialogListener.downFromBrowser();
                    if (UpdateStategyUtils.TYPE_FORCE_UPDATE != updateEntity.getForceUpdate()) {
                        dismiss();
                    }
                }
            }
        });
    }

    /**
     * 更新下载的进度
     *
     * @param currentProgress 当前进度
     */
    public void setProgress(int currentProgress) {
        if (mProgressBar != null && currentProgress > 0) {
            mProgressBar.setProgress(currentProgress);
        }
    }

    /**
     * 开启进度条，强制更新只显示进度条
     */
    public void showProgressBtn() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(0);
        }

        if (UpdateStategyUtils.TYPE_FORCE_UPDATE == updateEntity.getForceUpdate()) {
            mBtnUpdate.setVisibility(View.GONE);
            mWebDownload.setVisibility(View.GONE);
            mCancelUpdate.setVisibility(View.GONE);
        } else {
            mBtnUpdate.setVisibility(View.GONE);
            mWebDownload.setVisibility(View.GONE);
            mCancelUpdate.setVisibility(View.VISIBLE); //非强制更新可取消
        }
    }

    /**
     * 下载失败按钮处理
     * 强制更新：浏览器下载或退出应用
     * 普通更新：浏览器下载和取消
     */
    public void showFailBtn() {
        Toast.makeText(getContext(), getString(R.string.versionchecklib_update_failed), Toast.LENGTH_SHORT).show();
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }

        mBtnUpdate.setVisibility(View.GONE);
        mWebDownload.setVisibility(View.VISIBLE);
        mCancelUpdate.setVisibility(View.GONE);

        if (UpdateStategyUtils.TYPE_FORCE_UPDATE == updateEntity.getForceUpdate()) {
            mCancelUpdate.setText(R.string.versionchecklib_exit);
        } else {
            mCancelUpdate.setText(R.string.versionchecklib_cancel);
        }
    }

    /**
     * 判断存储卡权限
     */
    private void requestPermission() {
        if (getActivity() == null) {
            return;
        }
        //权限判断是否有访问外部存储空间权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                toUpdateDownLoad();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_STORAGE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flag = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (flag != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
                    Toast.makeText(getActivity(), getResources().getString(R.string.versionchecklib_update_permission), Toast.LENGTH_LONG).show();
                }
                // 申请授权
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                //有权限直接去下载
                toUpdateDownLoad();
            }
        } else {
            //直接去下载
            toUpdateDownLoad();
        }
    }

    private void toUpdateDownLoad() {
        if (mUpdateDialogListener != null) {
            mUpdateDialogListener.updateDownLoad();
        }
    }

    /**
     * 申请android O 安装权限
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestInstallPermission() {
        requestPermissions(new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //8.0应用设置界面未知安装开源返回时候
        if (requestCode == GET_UNKNOWN_APP_SOURCES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean allowInstall = Objects.requireNonNull(getContext()).getPackageManager().canRequestPackageInstalls();
            if (allowInstall) {
                dismiss();
                if (mUpdateDialogListener != null) {
                    mUpdateDialogListener.installApkAgain();
                }
            } else {
                Toast.makeText(getContext(), getString(R.string.versionchecklib_unknown_resource_refuse), Toast.LENGTH_SHORT).show();
                exit();
            }
        } else if (requestCode == REQUEST_CODE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                toUpdateDownLoad();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.versionchecklib_update_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //6.0 存储权限授权结果回调
                toUpdateDownLoad();
            } else {
                //提示，并且关闭
                Toast.makeText(getActivity(), getResources().getString(R.string.versionchecklib_update_permission), Toast.LENGTH_LONG).show();
                exit();
            }
        } else if (requestCode == INSTALL_PACKAGES_REQUESTCODE) {
            // 8.0的权限请求结果回调,授权成功
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mUpdateDialogListener != null) {
                    mUpdateDialogListener.installApkAgain();
                }
            } else {
                // 授权失败，引导用户去未知应用安装的界面
                if (getContext() != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    //注意这个是8.0新API
                    Uri packageUri = Uri.parse("package:" + getContext().getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
                    startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                }
            }
        }
    }

    /**
     * 强制退出
     */
    private void exit() {
        if (UpdateStategyUtils.TYPE_FORCE_UPDATE == updateEntity.getForceUpdate()) {
            if (mUpdateDialogListener != null) {
                mUpdateDialogListener.forceExit();
            }
        } else {
            dismiss();
        }
    }
}
