package com.danielpark.camera.listeners;

import android.support.annotation.CallSuper;

/**
 * Control Camera function interface
 * <br><br>
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-25.
 */
public interface ControlInterface {

    /**
     * Start Camera autoFocus
     */
    void autoFocus();

    /**
     * Start taking a picture
     */
    void takePicture();

    /**
     * Whether turn off the flash or not
     */
    void flashTorch();

    /**
     * Set listener to get taken picture file
     * @param listener
     */
    void setOnTakePictureListener(OnTakePictureListener listener);

    /**
     * Called this when Activity has finished <br>
     *     especially before super.onDestroy() is called
     */
    void finishCamera();
}
