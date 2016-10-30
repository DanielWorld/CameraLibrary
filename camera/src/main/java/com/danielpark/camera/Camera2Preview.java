package com.danielpark.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.danielpark.camera.listeners.OnTakePictureListener;
import com.danielpark.camera.util.AutoFitTextureView;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Camera2 API preview
 * <br><br>
 * Copyright (C) 2014-2016 daniel@bapul.net
 * Created by Daniel on 2016-08-26.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Preview extends AutoFitTextureView {

    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private int mSensorOrientation;
    /** A {@link Handler} for running tasks in the background */
    private Handler mBackgroundHandler;
    /** An additional thread for running tasks that shouldn't block the UI */
    private HandlerThread mBackgroundThread;

    private OnTakePictureListener onTakePictureListener;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public Camera2Preview(Context context) {
        super(context);

        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(surfaceTexture, width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(surfaceTexture, width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            releaseCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {

        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    @Override
    public void openCamera(SurfaceTexture surfaceTexture, int width, int height) {
//        super.openCamera(surfaceTexture, width, height);
        LOG.d("openCamera() : " + width + " , " + height);

        // Daniel (2016-10-25 23:32:40): Start handler thread
        startBackgroundThread();

        if (mCameraManager == null)
            mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);

        setUpCameraOutput(width, height, mCameraManager);
        configureTransform(surfaceTexture, width, height);
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutput(int width, int height, CameraManager cameraManager) throws CameraAccessException, SecurityException, NullPointerException {
        LOG.d("setupCameraOutput() : " + width + " , " + height);

        // TODO: For now, only rear Camera is supported
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            // We don't use a front camera for now
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                continue;

            // We don't use an external camera in this class
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                continue;

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                continue;
            }

            // TODO: We do use a back camera for now
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {

                // 1. Get the largest supported preview size
                Size largestPreviewSize = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                LOG.d("1. Largest preview size : " + largestPreviewSize.getWidth() + " , " + largestPreviewSize.getHeight());

                // 2. Get the largest supported picture size

                // 3. Get Camera orientation to fix rotation problem
                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                // https://developer.android.com/reference/android/hardware/Camera.CameraInfo.html
                /**
                 * The orientation of the camera image. The value is the angle that the camera image needs to be rotated clockwise so it shows correctly on the display in its natural orientation.
                 * It should be 0, 90, 180, or 270.
                 */
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                LOG.d("3. Camera Lens orientation : " + mSensorOrientation);

                // 4. Get current display rotation
                WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                int displayRotation = windowManager.getDefaultDisplay().getRotation();
                LOG.d("4. Current device rotation : " + ORIENTATIONS.get(displayRotation));

                // 5. Check if dimensions should be swapped
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                }
                LOG.d("5. is Dimension swapped? : " + swappedDimensions);

            }
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(SurfaceTexture surfaceTexture, int viewWidth, int viewHeight) {

    }

    /**
     * You must call this method to release Camera
     */
    @Override
    public void releaseCamera() {
//        super.releaseCamera();
        if (mCameraDevice != null) {
            LOG.d("Release Camera");
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
