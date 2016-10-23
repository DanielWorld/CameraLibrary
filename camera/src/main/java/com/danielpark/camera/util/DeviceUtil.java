package com.danielpark.camera.util;

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

/**
 * Device's utility
 * <br><br>
 * Copyright (c) 2014-2016 daniel@bapul.net
 * Created by Daniel Park on 2016-08-23.
 */
public class DeviceUtil {

    /**
     * Get device's resolution size
     * @param context {@link Context}
     * @return {@link Display} : device resolution size as display
     */
    public static Display getResolutionSize(Context context) throws Exception{
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return display;
    }

    /**
     * Get device's resolution width. if it fails then return 0 <br>
     *     If Orientation is LANDSCAPE MODE, height will be width
     * @param context {@link Context}
     * @return device's resolution width
     */
    public static int getResolutionWidth(Context context){
        try {
            return getResolutionSize(context).getWidth();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     *  Get device's resolution height. if it fails then return 0 <br>
     *     If Orientation is LANDSCAPE MODE, width will be height
     * @param context {@link Context}
     * @return device's resolution height
     */
    public static int getResolutionHeight(Context context){
        try{
            return getResolutionSize(context).getHeight();
        } catch (Exception e) {
            return 0;
        }
    }
}
