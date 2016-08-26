package com.danielpark.camera.listeners;

import android.graphics.SurfaceTexture;

/**
 * Control Camera function interface
 * <br><br>
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-25.
 */
public interface ControlInterface {

    /**
     * Open Camera
     * @param surfaceTexture
     * @param width
     * @param height
     */
    void openCamera(SurfaceTexture surfaceTexture, int width, int height);

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
     * Release Camera
     */
    void releaseCamera();

    /**
     * Called this when Activity has finished <br>
     *     especially before super.onDestroy() is called
     */
    void finishCamera();
}
