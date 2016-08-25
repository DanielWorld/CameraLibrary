package com.danielpark.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;

import com.danielpark.camera.util.AutoFitTextureView;

import java.io.IOException;

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
    public AutoFitTextureView build(Context context) throws IOException {

        if (!checkCameraHardware(context))
            throw new UnsupportedOperationException("No camera on this device!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No CAMERA permission!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No READ_EXTERNAL_STORAGE permission!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No WRITE_EXTERNAL_STORAGE permission!");

        return new CameraPreview(context);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            int numCameras = Camera.getNumberOfCameras();

            return numCameras > 0;
        } else {
            // no camera on this device
            return false;
        }
    }
}
