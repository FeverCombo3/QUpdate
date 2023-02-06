package com.blockin.update.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * Author:YJQ
 * Time:2018/4/27  10:55
 * Description:
 */
public class UpdateStategyUtils {
    public static final int TYPE_NO_UPDATE = 0; //不需要升级
    public static final int TYPE_NORMAL_UPDATE = 1; //推荐升级
    public static final int TYPE_FORCE_UPDATE = 2; //强制升级

    /**
     * 获取升级策略
     * @param currentVer 最新版本
     * @param latestVer  最低版本
     * @return
     */
    public static int getUpdateStrategy(Context ctx,String currentVer,String latestVer){
        return getNormalUpdateStrategy(ctx,currentVer,latestVer);
    }

    /**
     * 普通升级逻辑
     * @param ctx context
     * @param currentVer 最新版本
     * @param latestVer 最低版本
     * @return
     */
    private static int getNormalUpdateStrategy(Context ctx,String currentVer,String latestVer){
        if(TextUtils.isEmpty(currentVer) || TextUtils.isEmpty(latestVer)){
            return TYPE_NO_UPDATE;
        }
        String[] curArray = currentVer.split("\\.");
        String[] lateArray = latestVer.split("\\.");
        String[] localArray = getVersionName(ctx).split("\\.");
        if(curArray.length > 3 || lateArray.length > 3 || localArray.length > 3){
            return TYPE_NO_UPDATE;
        }else if(curArray.length < 3){
            curArray = modifyVersion(curArray);
        }else if(lateArray.length < 3){
            lateArray = modifyVersion(lateArray);
        }else if(localArray.length < 3){
            localArray = modifyVersion(localArray);
        }

        boolean update = getVersionCompareResult(curArray,localArray);
        if(update){
            boolean force = getVersionCompareResult(lateArray,localArray);
            if(force){
                return TYPE_FORCE_UPDATE;
            }else {
                return TYPE_NORMAL_UPDATE;
            }
        }

        return TYPE_NO_UPDATE;
    }

    /**
     * 判断是否需要升级(true为需要)
     * @param ctx
     * @param currentVer 最新版本
     * @param latestVer  最低版本
     * @return
     */
    public static boolean update(Context ctx,String currentVer,String latestVer){
        return getUpdateStrategy(ctx,currentVer,latestVer) != TYPE_NO_UPDATE;
    }

    /**
     * 获取本地软件版本号名称
     */
    public static String getVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 比对两个版本号v1和v2
     * v1 > v2返回true,否则返回false
     * @param v1 版本1
     * @param v2 版本2
     * @return
     */
    private static boolean getVersionCompareResult(String[] v1 ,String[] v2){
        try{
            int v11 = Integer.parseInt(v1[0]);
            int v21 = Integer.parseInt(v2[0]);
            if(v11 > v21){
                return true;
            }else if(v11 < v21){
                return false;
            }else if (v11 == v21){
                int v12 = Integer.parseInt(v1[1]);
                int v22 = Integer.parseInt(v2[1]);
                if(v12 > v22){
                    return true;
                }else if(v12 < v22){
                    return false;
                }else if (v12 == v22){
                    int v13 = Integer.parseInt(v1[2]);
                    int v23 = Integer.parseInt(v2[2]);
                    return v13 > v23;
                }
            }

        }catch (NumberFormatException e){
            return false;
        }

        return false;
    }

    /**
     * 把不是三位的版本号变成三位
     * 如果是2位（2.1）变成（2.1.0）
     * 如果是1位（2）变成（2.0.0）
     * @param version
     * @return
     */
    private static String[] modifyVersion(String[] version){
        String[] vnew = new String[3];
        if(version.length == 1){
            vnew[0] = version[0];
            vnew[1] = "0";
        }else if(version.length == 2){
            vnew[0] = version[0];
            vnew[1] = version[1];
        }
        vnew[2] = "0";
        return vnew;
    }
}
