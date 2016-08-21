package com.danielpark.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;

/**
 * Check if the device support Camera feature <br>
 *     And make sure this device support Camera2 API
 * <br><br>
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-21.
 */
public class CameraApiChecker {

    private CameraApiChecker(){}

    private static CameraApiChecker sThis;

    public static CameraApiChecker getInstance(){
        if (sThis == null)
            sThis = new CameraApiChecker();
        return sThis;
    }

    /**
     * Start proceed Camera feature
     */
    public void build(Context context) {

        if (!checkCameraHardware(context))
            throw new UnsupportedOperationException("No camera on this device!");
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            int numCameras = Camera.getNumberOfCameras();

            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
}
