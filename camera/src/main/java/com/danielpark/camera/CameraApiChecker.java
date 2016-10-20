package com.danielpark.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
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

    private int orientationMode = 0;    // 0 means nothing happen!

    /**
     *
     * @param orientation Portrait(1), Landscape(2), auto set(3) (set perfect orientation according to device camera lens automatically)
     * @return
     */
    public CameraApiChecker setOrientation(int orientation) {
        this.orientationMode = orientation;
        return this;
    }


    /**
     * Start proceed Camera feature
     */
    public AutoFitTextureView build(Activity context) throws IOException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No CAMERA permission!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No READ_EXTERNAL_STORAGE permission!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new IOException("No WRITE_EXTERNAL_STORAGE permission!");

        if (!checkCameraHardware(context))
            throw new UnsupportedOperationException("No camera on this device!");

        switch (orientationMode) {
            case 1:
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 2:
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 3:
                fixOrientation(context);
                break;
        }

        if (checkCamera2BackLensSupport(context)) {
            return new Camera2Preview(context);
        } else {
            checkCamera1BackLensSupport();
            return new CameraPreview(context);
        }
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

        if (camera == null)
            throw new UnsupportedOperationException("No Camera1 Back facing Lens!");

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
     * Check if the device supports Camera 2 API
     * @return
     */
    private boolean checkCamera2BackLensSupport(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return false;

        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            if (manager == null || manager.getCameraIdList().length < 1)
                return false;

            // Check if Camera Id for FACING_BACK_LENS exists
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // get lens facing info
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                // front camera
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;

                // external camera
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                    continue;

                // rear camera
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    LOG.d("Camera2 API support level : " + characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));

                    if (CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL == characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                            || CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 == characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)){
                        return true;
                    }
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check if the device supports back facing lens in Camera
     */
    private void checkCamera1BackLensSupport() {
        Camera camera = Camera.open();

        if (camera == null) {
            throw new UnsupportedOperationException("No Camera1 Back facing Lens!");
        } else {
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
