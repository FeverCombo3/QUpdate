package com.blockin.update.log;

import androidx.annotation.NonNull;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by YJQ.
 * Date: 2020-06-27
 */
public class LogcatLogger implements ILogger{

    /**
     * logcat里日志的最大长度.
     */
    private static final int MAX_LOG_LENGTH = 4000;


    @Override
    public void log(int priority, String tag, String message, Throwable t) {
        if(message != null && message.length() == 0){
            message = null;
        }

        if(message == null){
            if(t == null){
                return;
            }

            message = getStackTraceString(t);
        }else {
            if(t != null){
                message = "\n" + getStackTraceString(t);
            }
        }

        log(priority,tag,message);
    }

    private String getStackTraceString(Throwable t){
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    /**
     * 使用LogCat输出日志，字符长度超过4000则自动换行.
     *
     * @param priority 优先级
     * @param tag      标签
     * @param message  信息
     */
    public void log(int priority, String tag, String message) {
        int subNum = message.length() / MAX_LOG_LENGTH;
        if (subNum > 0) {
            int index = 0;
            for (int i = 0; i < subNum; i++) {
                int lastIndex = index + MAX_LOG_LENGTH;
                String sub = message.substring(index, lastIndex);
                logSub(priority, tag, sub);
                index = lastIndex;
            }
            logSub(priority, tag, message.substring(index, message.length()));
        } else {
            logSub(priority, tag, message);
        }
    }

    /**
     * 使用LogCat输出日志.
     *
     * @param priority 优先级
     * @param tag      标签
     * @param sub      信息
     */
    private void logSub(int priority, @NonNull String tag, @NonNull String sub) {
        switch (priority) {
            case Log.VERBOSE:
                Log.v(tag, sub);
                break;
            case Log.DEBUG:
                Log.d(tag, sub);
                break;
            case Log.INFO:
                Log.i(tag, sub);
                break;
            case Log.WARN:
                Log.w(tag, sub);
                break;
            case Log.ERROR:
                Log.e(tag, sub);
                break;
            case Log.ASSERT:
                Log.wtf(tag, sub);
                break;
            default:
                Log.v(tag, sub);
                break;
        }
    }

}
