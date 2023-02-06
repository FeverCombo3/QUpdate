package com.blockin.update.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.blockin.update.R;

/**
 * Created by YJQ.
 * Date: 2020-06-26
 */
public class UpdateEntity implements Parcelable {

    /**
     * 新版本的下载地址
     */
    private String downloadUrl;
    /**
     * 新版本号
     */
    private String versionCode;
    /**
     * 是否采取强制更新，默认为0，不采取强制更新，否则强制更新
     */
    private int forceUpdate;
    /**
     * 新版本更新的内容
     */
    private String updateInfo;

    /**
     * 新版本文件的大小,单位一般为M，需要自己换算，因为不知道保留的位数，根据自己需求吧
     */
    private String fileSize;

    /**
     * 文件下的保存路径 以/开头 比如 /A/B
     */
    private String savePath;

    /**
     * 浏览器的下载地址，如果下载失败，通过浏览器下载
     */
    private String downBrowserUrl;

    /**
     * 下载进度条的颜色，二级进度
     */
    private int updateProgressColor;

    /**
     * 风格：true代表默认静默下载模式，只弹出下载更新框,下载完毕自动安装， false 代表配合使用进度框与下载失败弹框
     */
    private boolean isSilentMode;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public int getForceUpdate() {
        return forceUpdate;
    }

    public String getUpdateInfo() {
        return updateInfo;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getDownBrowserUrl() {
        return downBrowserUrl;
    }

    public int getUpdateProgressColor() {
        return updateProgressColor;
    }

    public boolean isSilentMode() {
        return isSilentMode;
    }

    private UpdateEntity(Builder builder) {
        this.downloadUrl = builder.downloadUrl;
        this.versionCode = builder.versionCode;
        this.forceUpdate = builder.forceUpdate;
        this.updateInfo = builder.updateInfo;
        this.fileSize = builder.fileSize;
        this.savePath = builder.savePath;
        this.downBrowserUrl = builder.downBrowserUrl;
        this.updateProgressColor = builder.updateProgressColor;
        this.isSilentMode = builder.isSilentMode;
    }

    /**
     * 构造者模式，链式调用，构建和表示分离，可读性好
     */
    public static class Builder {

        private String downloadUrl;

        private String versionCode;

        /**
         * 默认不采取强制更新
         */
        private int forceUpdate = 0;

        private String updateInfo;

        private String fileSize;

        /**
         * 默认的更新进度条颜色
         */
        private int updateProgressColor = R.color.color_blue;

        /**
         * 默认的保存路径
         */
        private String savePath = "/download/";

        /**
         * 默认的Web下载地址
         */
        private String downBrowserUrl = "";

        /**
         * 风格：true代表默认静默下载模式，只弹出下载更新框,下载完毕自动安装， false 代表配合使用进度框与下载失败弹框
         */
        private boolean isSilentMode = true;


        public Builder downLoadUrl(String downLoadUrl) {
            this.downloadUrl = downLoadUrl;
            return this;
        }

        public Builder versionCode(String newVersionCode) {
            this.versionCode = newVersionCode;
            return this;
        }

        public Builder forceUpdate(int forceUpdate) {
            this.forceUpdate = forceUpdate;
            return this;
        }

        public Builder updateInfo(String updateInfo) {
            this.updateInfo = updateInfo;
            return this;
        }

        public Builder fileSize(String fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder savePath(String saveFilePath) {
            this.savePath = saveFilePath;
            return this;
        }

        public Builder downBrowserUrl(String downBrowserUrl) {
            this.downBrowserUrl = downBrowserUrl;
            return this;
        }

        public Builder isSilentMode(boolean isSilentMode) {
            this.isSilentMode = isSilentMode;
            return this;
        }

        public Builder updateProgressColor(int updateProgressColor){
            this.updateProgressColor = updateProgressColor;
            return this;
        }

        public UpdateEntity build() {
            return new UpdateEntity(this);
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(downloadUrl);
        dest.writeString(versionCode);
        dest.writeInt(forceUpdate);
        dest.writeString(updateInfo);
        dest.writeString(fileSize);
        dest.writeString(savePath);
        dest.writeString(downBrowserUrl);
        dest.writeInt(updateProgressColor);
        dest.writeByte((byte) (isSilentMode ? 1 : 0));
    }

    protected UpdateEntity(Parcel in) {
        downloadUrl = in.readString();
        versionCode = in.readString();
        forceUpdate = in.readInt();
        updateInfo = in.readString();
        fileSize = in.readString();
        savePath = in.readString();
        downBrowserUrl = in.readString();
        updateProgressColor = in.readInt();
        isSilentMode = in.readByte() != 0;
    }

    public static final Creator<UpdateEntity> CREATOR = new Creator<UpdateEntity>() {
        @Override
        public UpdateEntity createFromParcel(Parcel in) {
            return new UpdateEntity(in);
        }

        @Override
        public UpdateEntity[] newArray(int size) {
            return new UpdateEntity[size];
        }
    };
}
