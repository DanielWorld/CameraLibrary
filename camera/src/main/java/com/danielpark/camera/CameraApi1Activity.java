package com.danielpark.camera;

import android.Manifest;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.danielpark.camera.util.AutoFitTextureView;
import com.danielpark.camera.util.CameraLogger;
import com.danielpark.camera.util.DeviceUtil;
import com.danielpark.camera.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Activity which is using Camera API 1
 * <br><br>
 * Copyright (C) 2014-2016 daniel@bapul.net
 * Created by Daniel on 2016-08-23.
 */
public class CameraApi1Activity extends AppCompatActivity {

    private Logger LOG = Logger.getInstance();

    private AutoFitTextureView mTextureView;    // Daniel (2016-08-23 17:20:12): Camera preview
    private Camera mCamera;                     // Daniel (2016-08-23 17:20:21): Camera
    private Camera.Size mPreviewSize;           // Daniel (2016-08-23 17:20:25): Camera preview size
    private int mSensorOrientation;             // Daniel (2016-08-23 17:40:08): Orientation of the camera sensor

    private static final int MAX_PREVIEW_WIDTH = 1920;  // Daniel (2016-08-23 17:42:22): Max preview width that is guaranteed by Camera API
    private static final int MAX_PREVIEW_HEIGHT = 1080; // Daniel (2016-08-23 17:42:27): Max preview height that is guaranteed by Camera API

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
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
//            if (mCamera != null) {
//                mCamera.stopPreview();
//                mCamera.release();
//            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_api_1);
//
        mTextureView = (AutoFitTextureView) findViewById(R.id.cameraPreview);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    protected void onDestroy() {
        if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
        }
        super.onDestroy();
    }

    private void openCamera(SurfaceTexture surfaceTexture, int width, int height) {
        LOG.d("openCamera : " + width + " / " + height);

        try {
            if (mCamera == null)
                mCamera = Camera.open();

            setUpCameraOutput(width, height);
            configureTransform(surfaceTexture, width, height);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutput(int width, int height) {
        LOG.d("setUpCameraOutput : " + width + " / " + height);

        // TODO: 1. 해당 카메라 및 Back camera 지원여부 체크.. 나중에!

        // For still image captures, we use the largest available size.
        Camera.Size largest = Collections.max(
                mCamera.getParameters().getSupportedPreviewSizes(),
                new CompareSizesByArea());

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        //noinspection ConstantConditions
        mSensorOrientation = cameraInfo.orientation;
        LOG.d("mSensorOrientation : " + mSensorOrientation);

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
            default:
                LOG.e("Display rotation is invalid: " + displayRotation);
        }

        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        // Daniel (2016-05-04 13:45:09): Preview 의 최대 사이즈를 기기 resolution 크기와 비교!
        try {
            if (maxPreviewWidth > maxPreviewHeight) {
                if (maxPreviewWidth > Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this)))
                    maxPreviewWidth = Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this));

                if (maxPreviewHeight > Math.min(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this)))
                    maxPreviewHeight = Math.min(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this));

            } else if (maxPreviewHeight > maxPreviewWidth) {
                if (maxPreviewHeight > Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this)))
                    maxPreviewHeight = Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this));

                if (maxPreviewWidth > Math.min(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this)))
                    maxPreviewWidth = Math.min(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this));
            } else {
                if (maxPreviewWidth > Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this)))
                    maxPreviewWidth = Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this));

                if (maxPreviewHeight > Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this)))
                    maxPreviewHeight = Math.max(DeviceUtil.getResolutionWidth(this), DeviceUtil.getResolutionHeight(this));
            }

        } catch (Exception e) {
            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH;
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
        // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
        // garbage capture data.
        mPreviewSize = chooseOptimalSize(mCamera.getParameters().getSupportedPreviewSizes(),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largest);

        try {
            LOG.d("previewSize width : " + mPreviewSize.width);
            LOG.d("previewSize height : " + mPreviewSize.height);
        } catch (Exception ignored){}

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.setAspectRatio(
                    mPreviewSize.width, mPreviewSize.height);
        } else {
            mTextureView.setAspectRatio(
                    mPreviewSize.height, mPreviewSize.width);
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
        if (null == mPreviewSize)
            return;

        LOG.d("configureTransform : " + viewWidth + " / " + viewHeight);

//        int rotation = get

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (surfaceTexture == null) {
            // preview surface does not exist
            return;
        }

        // Stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception ignored){
            // ignore: tried to stop a non-existent preview
        }

        // 1. 해당 카메라 type 체크 (Back or Front)
        // 2. 현재는 Back 일 경우에만 지원할 예정
        // 2-1. 해당 Camera Info orientation + 기기의 orientation + surfaceHolder size 로 최적의 preview size 및 orientation 을 찾아서 적용하면 된다.
        // 3. onMeasure() 로 처리할 지 여부는 추후 처리할 예정.

        // Set preview size and make any resize, rotate or
        // reformatting changes here
        // and start preview with new settings
        try {

            if (null == mTextureView || null == mPreviewSize ) {
                return;
            }
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / mPreviewSize.height,
                        (float) viewWidth / mPreviewSize.width);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180, centerX, centerY);
            }
            mTextureView.setTransform(matrix);

            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int textureViewWidth,
                                                 int textureViewHeight, int maxWidth, int maxHeight, Camera.Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Camera.Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        for (Camera.Size option : choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                if (option.width >= textureViewWidth &&
                        option.height >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices.get(0);
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }
    }
}
