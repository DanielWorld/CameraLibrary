package com.danielpark.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;

import com.danielpark.camera.util.AutoFitTextureView;
import com.danielpark.camera.util.DeviceUtil;
import com.danielpark.camera.util.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

/**
 * Check if the device support Camera feature <br>
 *     And make sure this device support Camera2 API
 * <br><br>
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-21.
 */
public class CameraApiChecker {

    private Logger LOG = Logger.getInstance();

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
    public AutoFitTextureView build(Activity context) throws IOException {

        if (!checkCameraHardware(context))
            throw new UnsupportedOperationException("No camera on this device!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No CAMERA permission!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No READ_EXTERNAL_STORAGE permission!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No WRITE_EXTERNAL_STORAGE permission!");

        fixOrientation(context);

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

    /**
     * Fix orientation
     * @param context
     */
    private void fixOrientation(Activity context) {
        LOG.d("FixOrientation()");

        Camera camera = Camera.open();

        // Daniel (2016-08-26 12:17:06): Get the largest supported preview size
        Camera.Size largestPreviewSize = Collections.max(
                camera.getParameters().getSupportedPreviewSizes(),
                new CompareSizesByArea());

        // Daniel (2016-08-26 12:17:33): Get current device configuration
        int orientation = context.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int screenWidth = DeviceUtil.getResolutionWidth(context);
            int screenHeight = DeviceUtil.getResolutionHeight(context);

            if ((screenWidth > screenHeight && largestPreviewSize.width < largestPreviewSize.height)
                    || (screenWidth < screenHeight && largestPreviewSize.width > largestPreviewSize.height))
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            int screenWidth = DeviceUtil.getResolutionWidth(context);
            int screenHeight = DeviceUtil.getResolutionHeight(context);

            if ((screenWidth > screenHeight && largestPreviewSize.width < largestPreviewSize.height)
                    || (screenWidth < screenHeight && largestPreviewSize.width > largestPreviewSize.height))
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }
    }
}
