package com.danielpark.camera.util;

/**
 * Camera Library switch log
 * <br><br>
 * Copyright (C) 2014-2016 daniel@bapul.net
 * Created by Daniel on 2016-08-23.
 */
public class CameraLogger {

    private static boolean mLogFlag = false;

    public static void enable() {
        mLogFlag = true;
    }

    public static void disable() {
        mLogFlag = false;
    }

    public static boolean getLogState(){
        return mLogFlag;
    }
}
