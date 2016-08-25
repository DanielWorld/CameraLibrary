package com.danielpark.camera.listeners;

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
}
