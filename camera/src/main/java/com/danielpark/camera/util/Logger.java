package com.danielpark.camera.util;

import android.util.Log;

/**
 * Copyright (C) 2014-2016 daniel@bapul.net
 * Created by Daniel on 2016-08-23.
 */
public class Logger {

    private static Logger sThis;

    private Logger(){}

    public synchronized static final Logger getInstance() {
        if (sThis == null)
            sThis = new Logger();

        return sThis;
    }

    private final String TAG = "CameraLogger";
    private boolean mLogFlag = CameraLogger.getLogState();

    public void v(String msg) {
        if (mLogFlag) {
            Log.v(TAG, "" + msg);
        }
    }

    public void d(String msg) {
        if (mLogFlag) {
            Log.d(TAG, "" + msg);
        }
    }

    public void e(String msg) {
        if (mLogFlag) {
            Log.e(TAG, "" + msg);
        }
    }

    public void i(String msg) {
        if (mLogFlag) {
            Log.i(TAG, "" + msg);
        }
    }

    public void w(String msg) {
        if (mLogFlag) {
            Log.w(TAG, "" + msg);
        }
    }

    public void wtf(String msg) {
        if (mLogFlag) {
            Log.wtf(TAG, "" + msg);
        }
    }
}
