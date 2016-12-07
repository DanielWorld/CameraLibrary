package com.danielpark.camera.listeners;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;

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
    void openCamera(SurfaceTexture surfaceTexture, int width, int height) throws CameraAccessException;

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
    void flashToggle();

    /**
     * Check if the device supports flash or not
     * @return
     */
    boolean supportFlash();

    /**
     * Set listener to get taken picture file
     * @param listener
     */
    void setOnTakePictureListener(OnTakePictureListener listener);

    /**
     * Enable or disable Orientation event listener <br>
     *     if it is <b>true</b> then, it is applied to taken picture. so, Make sure to set ORIENTATION
     * @param isEnabled
     */
    void setOrientationEventListener(boolean isEnabled);

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
