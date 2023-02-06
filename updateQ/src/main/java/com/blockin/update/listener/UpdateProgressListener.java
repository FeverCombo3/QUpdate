package com.blockin.update.listener;

/**
 * Created by YJQ.
 * Date: 2020-06-26
 */
public interface UpdateProgressListener {

    /**
     * onProgressChange
     *
     * @param current 当前进度
     * @param max     最大进度
     */
    void onProgressChange(int current, int max);
}
