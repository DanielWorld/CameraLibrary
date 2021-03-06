package com.danielpark.camera.listeners;

//import android.support.annotation.NonNull;

import java.io.File;

/**
 * Copyright (C) 2014-2016 daniel@bapul.net
 * Created by Daniel on 2016-08-26.
 */
public interface OnTakePictureListener {

    /**
     * Send a capture image file
     * @param file
     */
    void onTakePicture(File file);

    /**
     * Check if camera lens is focused successfully
     * @param isFocused
     */
    void onLensFocused(boolean isFocused);
}
