package com.danielpark.camera;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.danielpark.camera.listeners.OnTakePictureListener;
import com.danielpark.camera.util.AutoFitTextureView;
import com.danielpark.camera.util.DeviceUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Camera API preview
 * <br><br>
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-21.
 */
public class CameraPreview extends AutoFitTextureView{

    private Camera mCamera;
    private Camera.Size mPreviewSize;
    private Camera.Size mPictureSize;
    private int mSensorOrientation;
    /** the lastest view size */
    private PointF mLatestViewSize = new PointF();

    /** Orientation event flag */
    private boolean isOrientationEventAvailable = false;
    /** Last changed orientation */
    private int mLastOrientation;
    private OnTakePictureListener onTakePictureListener;
    private OrientationEventListener mOrientationEventListener;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    public CameraPreview(Activity context) {
        super(context);

        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            try {
                openCamera(surfaceTexture, width, height);
            } catch (RuntimeException e){
                // Daniel (2016-11-09 23:59:52): It might camera already release
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            try {
                configureTransform(surfaceTexture, width, height);
            } catch (RuntimeException e){
                // Daniel (2016-11-09 23:59:52): It might camera already release
                e.printStackTrace();
            }
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

    @Override
    public void openCamera(SurfaceTexture surfaceTexture, int width, int height) {
        LOG.d("openCamera() : " + width + " , " + height);

        /**
         * If OrientationEventListener is available then open it
         */
        if (mOrientationEventListener != null && mOrientationEventListener.canDetectOrientation())
            mOrientationEventListener.enable();

        if (mCamera == null)
            mCamera = Camera.open();

        try {
            setUpCameraOutput(width, height);
            configureTransform(surfaceTexture, width, height);
        } catch (RuntimeException e){
            e.printStackTrace();
        }
    }

    /**
     * Check if current device gets correct orientation compares to preview size
     * @return
     */
    private boolean isCorrectRatioOrientation() {
        // Daniel (2016-08-26 12:17:06): Get the largest supported preview size
        Camera.Size largestPreviewSize = Collections.max(
                mCamera.getParameters().getSupportedPreviewSizes(),
                new CompareSizesByArea());

        // Daniel (2016-08-26 12:17:33): Get current device configuration
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int screenWidth = DeviceUtil.getResolutionWidth(getContext());
            int screenHeight = DeviceUtil.getResolutionHeight(getContext());

            if ((screenWidth > screenHeight && largestPreviewSize.width < largestPreviewSize.height)
                    || (screenWidth < screenHeight && largestPreviewSize.width > largestPreviewSize.height))
                return false;
            else
                return true;


        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            int screenWidth = DeviceUtil.getResolutionWidth(getContext());
            int screenHeight = DeviceUtil.getResolutionHeight(getContext());

            if ((screenWidth > screenHeight && largestPreviewSize.width < largestPreviewSize.height)
                    || (screenWidth < screenHeight && largestPreviewSize.width > largestPreviewSize.height))
                return false;
            else
                return true;
        }
        return true;
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutput(int width, int height) throws RuntimeException {
        LOG.d("setupCameraOutput() : " + width + " , " + height);

        // 1. Get the largest supported preview size
        Camera.Size largestPreviewSize = Collections.max(
                mCamera.getParameters().getSupportedPreviewSizes(),
                new CompareSizesByArea());

        LOG.d("1. Largest preview size : " + largestPreviewSize.width + " , " + largestPreviewSize.height);

        // 2. Get the largest supported picture size
        Camera.Size largestPictureSize = Collections.max(
                mCamera.getParameters().getSupportedPictureSizes(),
                new CompareSizesByArea());

        LOG.d("2. Largest Picture size (Not preview size) : " + largestPictureSize.width + " , " + largestPictureSize.height);

        // 3. Get Camera orientation to fix rotation problem
        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        // https://developer.android.com/reference/android/hardware/Camera.CameraInfo.html
        /**
         * The orientation of the camera image. The value is the angle that the camera image needs to be rotated clockwise so it shows correctly on the display in its natural orientation.
         * It should be 0, 90, 180, or 270.
         */
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
        mSensorOrientation = cameraInfo.orientation;
        LOG.d("3. Camera Lens orientation : " + mSensorOrientation);
        mCamera.setDisplayOrientation(mSensorOrientation);

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

        // 6. Get device resolution max size
        Point resolutionSize = new Point();
        windowManager.getDefaultDisplay().getSize(resolutionSize);

        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = resolutionSize.x;
        int maxPreviewHeight = resolutionSize.y;

        if (swappedDimensions) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = resolutionSize.y;
            maxPreviewHeight = resolutionSize.x;
        }
        LOG.d("6. Resolution Size : " + resolutionSize.x + " , " + resolutionSize.y);

        // 7. choose Optimal preview size!
        // Daniel (2016-11-05 13:59:19): : use the largest preview size for better quality
        mPreviewSize = largestPreviewSize;
        LOG.d("7. Optimal Preview size : " + mPreviewSize.width + " , " + mPreviewSize.height);

        // 8. choose Optimal Picture size!
        mPictureSize = chooseOptimalSize(mCamera.getParameters().getSupportedPictureSizes(),
                largestPreviewSize.width, largestPreviewSize.height, largestPreviewSize.width, largestPreviewSize.height,
                largestPreviewSize);

        // Daniel (2016-11-14 15:13:09): OKAY, but if mPictureSize is too bigger than mPreviewSize, e.g) multiple by 2
        if ((float) (mPictureSize.width * mPictureSize.height) > (float) (mPreviewSize.width * mPreviewSize.height) * 2) {
            mPictureSize = chooseOptimalSize(mCamera.getParameters().getSupportedPictureSizes(),
                    largestPreviewSize.width, largestPreviewSize.height, largestPreviewSize.width, largestPreviewSize.height);
        }

        // Daniel (2016-11-04 12:18:33): Picture size should be equal or better than the largest preview size
        LOG.d("8. Optimal Picture size : " + mPictureSize.width + " , " + mPictureSize.height);

        // 9. According to orientation, change SurfaceView size
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            LOG.d("9. Orientation PORTRAIT");
//            setAspectRatio(
//                    mPreviewSize.height, mPreviewSize.width);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LOG.d("9. Orientation LANDSCAPE");
//            setAspectRatio(
//                    mPreviewSize.width, mPreviewSize.height);
        }

        // Daniel (2016-11-09 15:52:55): try to disable shutter sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (cameraInfo.canDisableShutterSound) {
                LOG.d("Disable shutter sound");
                mCamera.enableShutterSound(false);
                return;
            }
        }

        // Daniel (2016-11-09 15:55:29): Device can't disable shutter sound
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                mPreviewFrame = data;
            }
        });
    }

    private byte[] mPreviewFrame;

    /**
     * Save offset of preview size to take picture with correct aspect ratio
     */
    RectF mConfigureTransformMargin = new RectF();

    /**
     * Initialize transform margin
     */
    private void initializeTransformMargin() {
        if (mConfigureTransformMargin == null) return;

        mConfigureTransformMargin.left = 0;
        mConfigureTransformMargin.right = 0;
        mConfigureTransformMargin.top = 0;
        mConfigureTransformMargin.bottom = 0;
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(SurfaceTexture surfaceTexture, int viewWidth, int viewHeight) throws RuntimeException {
        if (null == mPreviewSize)
            return;

        LOG.d("configureTransform () : " + viewWidth + " , " + viewHeight);

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

        // Set preview size and make any resize, rotate or
        // reformatting changes here
        // and start preview with new settings
        try {
            if (null == mPreviewSize ) {
                return;
            }
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            int rotation = windowManager.getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, mPreviewSize.width, mPreviewSize.height);
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            LOG.d("Surface rotation : " + ORIENTATIONS.get(rotation));
            LOG.d("Correct Ratio Orientation : " + isCorrectRatioOrientation());
            LOG.d("Sensor orientation : " + mSensorOrientation);

            if (isCorrectRatioOrientation()) {
                switch (rotation) {
                    // Daniel (2016-11-08 11:45:11): TODO: TEST
                    case Surface.ROTATION_0: {
                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // TODO: add margin to take a correct picture
                        break;
                    }
                    // Daniel (2016-11-08 11:44:18): TEST COMPLETED!
                    case Surface.ROTATION_90: {
                        // Daniel (2016-08-26 17:34:05): Reverse dst size
                        bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);

                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // Daniel (2016-11-07 21:59:28): get Scale to fit view size (container) not screen!!!
                        float scale = Math.max(
                                (float) viewWidth / mPreviewSize.height,
                                (float) viewHeight / mPreviewSize.width
                        );
                        LOG.d("scale : " + scale);
                        matrix.postScale(scale, scale, centerX, centerY);

                        // Daniel (2016-11-07 22:36:43): Calculate margin to crop Image
                        // Daniel (2016-11-08 11:33:57): TODO: Reverse width & height
                        // Daniel (2016-11-08 11:35:33): Because it has to be rotate then x/y will be converted each other
                        float currentExpectedWidthSize = bufferRect.height() * scale;
                        float currentExpectedHeightSize = bufferRect.width() * scale;

                        // Daniel (2016-11-08 00:38:08): save the latest view size
                        mLatestViewSize.set(currentExpectedWidthSize, currentExpectedHeightSize);

                        LOG.d("currentVisibleWidth : " + currentExpectedWidthSize);
                        LOG.d("x_margin : " + (currentExpectedWidthSize - viewWidth));
                        mConfigureTransformMargin.left = (currentExpectedWidthSize - viewWidth) / 2;
                        mConfigureTransformMargin.right = (currentExpectedWidthSize - viewWidth) / 2;

                        LOG.d("currentVisibleHeight : " + currentExpectedHeightSize);
                        LOG.d("y_margin : " + (currentExpectedHeightSize - viewHeight));
                        mConfigureTransformMargin.top = (currentExpectedHeightSize - viewHeight) / 2;
                        mConfigureTransformMargin.bottom = (currentExpectedHeightSize - viewHeight) / 2;

                        matrix.postRotate(-90, centerX, centerY);
                        break;
                    }
                    // Daniel (2016-11-08 11:45:11): TODO: TEST
                    case Surface.ROTATION_180: {
                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // TODO: add margin to take a correct picture

                        matrix.postRotate(-180, centerX, centerY);
                        break;
                    }
                    // Daniel (2016-11-08 11:36:47): TEST COMPLETED!
                    case Surface.ROTATION_270: {
                        // Daniel (2016-08-26 17:34:05): Reverse dst size
                        bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);

                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // Daniel (2016-11-07 21:59:28): get Scale to fit view size (container) not screen!!!
                        float scale = Math.max(
                                (float) viewWidth / mPreviewSize.height,
                                (float) viewHeight / mPreviewSize.width
                        );
                        LOG.d("scale : " + scale);
                        matrix.postScale(scale, scale, centerX, centerY);

                        // Daniel (2016-11-07 22:36:43): Calculate margin to crop Image
                        // Daniel (2016-11-08 11:33:57): TODO: Reverse width & height
                        // Daniel (2016-11-08 11:35:33): Because it has to be rotate then x/y will be converted each other
                        float currentExpectedWidthSize = bufferRect.height() * scale;
                        float currentExpectedHeightSize = bufferRect.width() * scale;

                        // Daniel (2016-11-08 00:38:08): save the latest view size
                        mLatestViewSize.set(currentExpectedWidthSize, currentExpectedHeightSize);

                        LOG.d("currentVisibleWidth : " + currentExpectedWidthSize);
                        LOG.d("x_margin : " + (currentExpectedWidthSize - viewWidth));
                        mConfigureTransformMargin.left = (currentExpectedWidthSize - viewWidth) / 2;
                        mConfigureTransformMargin.right = (currentExpectedWidthSize - viewWidth) / 2;

                        LOG.d("currentVisibleHeight : " + currentExpectedHeightSize);
                        LOG.d("y_margin : " + (currentExpectedHeightSize - viewHeight));
                        mConfigureTransformMargin.top = (currentExpectedHeightSize - viewHeight) / 2;
                        mConfigureTransformMargin.bottom = (currentExpectedHeightSize - viewHeight) / 2;

                        matrix.postRotate(-270, centerX, centerY);
                        break;
                    }
                }
            } else {
                switch (rotation) {
                    // Daniel (2016-11-08 11:02:08): TEST COMPLETED!
                    case Surface.ROTATION_0: {
                        // Daniel (2016-08-26 17:34:05): Reverse dst size
                        bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);

                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(offset_x, offset_y);
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                        // No need to rotate view, because unlike Camear2 API,
                        // Camera 1 API has setDisplayOrientation();

                        // Daniel (2016-11-07 21:59:28): get Scale to fit view size (container) not screen!!!
                        float scale = Math.max(
                                (float) viewWidth / mPreviewSize.height,
                                (float) viewHeight / mPreviewSize.width
                        );
                        LOG.d("scale : " + scale);
                        matrix.postScale(scale, scale, centerX, centerY);

                        // Daniel (2016-11-07 22:36:43): Calculate margin to crop Image
                        float currentExpectedWidthSize = bufferRect.width() * scale;
                        float currentExpectedHeightSize = bufferRect.height() * scale;

                        // Daniel (2016-11-08 00:38:08): save the latest view size
                        mLatestViewSize.set(currentExpectedWidthSize, currentExpectedHeightSize);

                        LOG.d("currentVisibleWidth : " + currentExpectedWidthSize);
                        LOG.d("x_margin : " + (currentExpectedWidthSize - viewWidth));
                        mConfigureTransformMargin.left = (currentExpectedWidthSize - viewWidth) / 2;
                        mConfigureTransformMargin.right = (currentExpectedWidthSize - viewWidth) / 2;

                        LOG.d("currentVisibleHeight : " + currentExpectedHeightSize);
                        LOG.d("y_margin : " + (currentExpectedHeightSize - viewHeight));
                        mConfigureTransformMargin.top = (currentExpectedHeightSize - viewHeight) / 2;
                        mConfigureTransformMargin.bottom = (currentExpectedHeightSize - viewHeight) / 2;
                        break;
                    }
                    // Daniel (2016-11-08 11:45:11): TODO: TEST
                    case Surface.ROTATION_90: {
                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // TODO: add margin to take a correct picture

                        matrix.postRotate(-90, centerX, centerY);
                        break;
                    }
                    // Daniel (2016-11-08 11:45:11): TODO: TEST
                    case Surface.ROTATION_180: {
                        // Daniel (2016-08-26 17:34:05): Reverse dst size
                        bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);

                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // TODO: add margin to take a correct picture

                        matrix.postRotate(-180, centerX, centerY);
                        break;
                    }
                    // Daniel (2016-11-08 11:45:11): TODO: TEST
                    case Surface.ROTATION_270: {
                        if (mConfigureTransformMargin == null) mConfigureTransformMargin = new RectF();
                        final float offset_x = (centerX - bufferRect.centerX());
                        final float offset_y = (centerY - bufferRect.centerY());

                        LOG.d("offset x  : " + offset_x);
                        LOG.d("offset y : " + offset_y);

                        initializeTransformMargin();

                        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                        // TODO: add margin to take a correct picture

                        matrix.postRotate(-270, centerX, centerY);
                        break;
                    }
                }
            }
            setTransform(matrix);

            // 10. Set preview size
            Camera.Parameters mParameters = mCamera.getParameters();
            mParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            LOG.d("10. Set preview size : " + mPreviewSize.width + " , " + mPreviewSize.height);

            // 11. Set Picture size & format
            mParameters.setPictureSize(mPictureSize.width, mPictureSize.height);
//            mParameters.setPictureFormat(PixelFormat.JPEG);
            LOG.d("11. Set Picture size : " + mPictureSize.width + " , " + mPictureSize.height);

            mCamera.setParameters(mParameters);

            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;

        Matrix txform = new Matrix();
        getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        setTransform(txform);
    }

	private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int textureViewWidth,
												 int textureViewHeight, int maxWidth, int maxHeight) {
		// Collect the supported resolutions that are at least as big as the preview Surface
		List<Camera.Size> bigEnough = new ArrayList<>();
		// Collect the supported resolutions that are smaller than the preview Surface
		List<Camera.Size> notBigEnough = new ArrayList<>();
//		int w = aspectRatio.width;
//		int h = aspectRatio.height;
		for (Camera.Size option : choices) {
			if (option.width <= maxWidth && option.height <= maxHeight) {
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

    @Override
    public void autoFocus() {
        super.autoFocus();
        if (mCamera != null) {
            try {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        LOG.d("onAutoFocus() : " + success);

                        if (onTakePictureListener != null)
                            onTakePictureListener.onLensFocused(success);
                    }
                });
            } catch (RuntimeException e){
                e.printStackTrace();
                // Daniel (2016-11-10 00:53:01): Usually, it happens on some freak devices
                // return auto focus failure result
                if (onTakePictureListener != null)
                    onTakePictureListener.onLensFocused(false);
            }
        }
    }

    /**
     * Convert last changed orientation to correct degree
     * @param mLastOrientation
     * @return
     */
    private int getLastOrientation(int mLastOrientation) {
        if (!isOrientationEventAvailable) return 0;

        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (rotation == Surface.ROTATION_0) {
                if (mLastOrientation >= 45 && mLastOrientation <= 90 + 45)
                    return 90;
                else if (mLastOrientation >= 90 + 45 && mLastOrientation <= 90 * 2 + 45)
                    return 180;
                else if (mLastOrientation >= 90 * 2 + 45 && mLastOrientation <= 90 * 3 + 45)
                    return 270;
                return 0;
            } else {
                if (mLastOrientation >= 45 && mLastOrientation <= 90 + 45)
                    return 0;
                else if (mLastOrientation >= 90 + 45 && mLastOrientation <= 90 * 2 + 45)
                    return 90;
                else if (mLastOrientation >= 90 * 2 + 45 && mLastOrientation <= 90 * 3 + 45)
                    return 180;
                return 270;
            }
        } else {
            if (rotation == Surface.ROTATION_0) {
                if (mLastOrientation >= 45 && mLastOrientation <= 90 + 45)
                    return 90;
                else if (mLastOrientation >= 90 + 45 && mLastOrientation <= 90 * 2 + 45)
                    return 180;
                else if (mLastOrientation >= 90 * 2 + 45 && mLastOrientation <= 90 * 3 + 45)
                    return 270;
                return 0;
            } else {
                if (mLastOrientation >= 45 && mLastOrientation <= 90 + 45)
                    return 180;
                else if (mLastOrientation >= 90 + 45 && mLastOrientation <= 90 * 2 + 45)
                    return 270;
                else if (mLastOrientation >= 90 * 2 + 45 && mLastOrientation <= 90 * 3 + 45)
                    return 0;
                return 90;
            }
        }
    }

    @Override
    public void takePicture() {
        super.takePicture();

        // Daniel (2016-11-03 16:12:52): Start taking picture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
            if (cameraInfo.canDisableShutterSound) {
                captureStillPicture();
                return;
            }
        }

        // Daniel (2016-12-07 10:56:34): Which means preview frame is invalid (No need to setPreviewCallback, so use setOneshotCallback method
        if (mPreviewFrame == null || mPreviewFrame.length == 0) {
            if (mCamera != null) {
                mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        mPreviewFrame = data;
                        captureDeprecatePicture();
                    }
                });
            } else {
                // Daniel (2016-12-07 11:11:34): mCamera is null which means, camera was already released
                // (Activity or Fragment has been destroyed)
            }
        } else {
            // Daniel (2016-12-07 11:00:47): if mPreviewFrame is valid then, use captureDeprecatePicture()
            captureDeprecatePicture();
        }
    }

    /**
     * Try to capture a still image from preview
     */
    private void captureStillPicture() {
        LOG.d("captureStillPicture()");

        try {
            if (mCamera != null)
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                        if (bytes != null) {
                            LOG.d("view Width : " + getWidth());
                            LOG.d("view Height : " + getHeight());

                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                            // Daniel (2016-08-26 14:01:20): Current Device rotation
                            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                            int displayRotation = windowManager.getDefaultDisplay().getRotation();
                            LOG.d("Current device rotation : " + ORIENTATIONS.get(displayRotation));

                            int result = (mSensorOrientation - ORIENTATIONS.get(displayRotation) + 360) % 360;

                            Bitmap rotatedBitmap = null;

                            if (result % 360 != 0) {
                                rotatedBitmap = rotateImage(bitmap, result);
                                rotatedBitmap = cropImage(rotatedBitmap);
                            } else {
                                rotatedBitmap = cropImage(bitmap);
                            }

                            if (getLastOrientation(mLastOrientation) % 360 != 0)
                                rotatedBitmap = rotateImage(rotatedBitmap, getLastOrientation(mLastOrientation));

                            File pictureFile = getOutputMediaFile();
                            if (pictureFile == null) {
                                return;
                            }
                            try {
                                FileOutputStream fos = new FileOutputStream(pictureFile);

                                if (rotatedBitmap != null)
                                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                                else
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);

                                fos.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                LOG.d("File path : " + pictureFile.getAbsolutePath());

                                try {
                                    // TODO: recycle Bitmap!!!
                                    if (rotatedBitmap != null) {
                                        rotatedBitmap.recycle();
                                        rotatedBitmap = null;
                                    } else {
                                        bitmap.recycle();
                                        bitmap = null;
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                }

                                if (onTakePictureListener != null && pictureFile != null)
                                    onTakePictureListener.onTakePicture(pictureFile);

                                try {
                                    if (mCamera != null) {
                                        mCamera.stopPreview();
                                        mCamera.startPreview();
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Try to capture a still image from preview
     */
    @Deprecated
    private void captureDeprecatePicture() {
        LOG.d("captureDeprecatePicture()");

        try {
            if (mPreviewFrame != null && mPreviewFrame.length > 0) {
                int format = mCamera.getParameters().getPreviewFormat();
                YuvImage yuvImage = new YuvImage(mPreviewFrame, format, mPreviewSize.width, mPreviewSize.height, null);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Rect rect = new Rect(0, 0, mPreviewSize.width, mPreviewSize.height);
                yuvImage.compressToJpeg(rect, 95, byteArrayOutputStream);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;
                options.inInputShareable = true;

                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size(), options);

                byteArrayOutputStream.flush();
                byteArrayOutputStream.close();

//                Bitmap bitmap = BitmapFactory.decodeByteArray(mPreviewFrame, 0, mPreviewFrame.length);

                // Daniel (2016-08-26 14:01:20): Current Device rotation
                WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                int displayRotation = windowManager.getDefaultDisplay().getRotation();
                LOG.d("Current device rotation : " + ORIENTATIONS.get(displayRotation));

                int result = (mSensorOrientation - ORIENTATIONS.get(displayRotation) + 360) % 360;

                Bitmap rotatedBitmap = null;

                if (result % 360 != 0) {
                    rotatedBitmap = rotateImage(bitmap, result);
                    rotatedBitmap = cropImage(rotatedBitmap);
                } else {
                    rotatedBitmap = cropImage(bitmap);
                }

                if (getLastOrientation(mLastOrientation) % 360 != 0)
                    rotatedBitmap = rotateImage(rotatedBitmap, getLastOrientation(mLastOrientation));

                File pictureFile = getOutputMediaFile();
                if (pictureFile == null) {
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);

                    if (rotatedBitmap != null)
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    else
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);

                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    LOG.d("File path : " + pictureFile.getAbsolutePath());

                    try {
                        // TODO: recycle Bitmap!!!
                        if (rotatedBitmap != null) {
                            rotatedBitmap.recycle();
                            rotatedBitmap = null;
                        } else {
                            bitmap.recycle();
                            bitmap = null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (onTakePictureListener != null && pictureFile != null)
                        onTakePictureListener.onTakePicture(pictureFile);

                    // remove mPreviewFrame
                    mPreviewFrame = null;
                }
            } else {
                captureStillPicture();
            }
        } catch (Exception e){
            captureStillPicture();
        }
    }

    @Override
    public boolean supportFlash() {
        if (mCamera != null && mCamera.getParameters() != null && mCamera.getParameters().getSupportedFlashModes() != null) {
            return mCamera.getParameters().getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH);
        }
        return false;
    }

    @Override
    public void flashToggle() {
        super.flashToggle();

        LOG.d("flashTorch()");

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<String> supportedFlashModes = params.getSupportedFlashModes();

            if (supportedFlashModes == null) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); // Crashlytics #1648
                return;
            } else {
//                for (String i : supportedFlashModes) {
//                    LOG.d("Supported Flash mode : " + i);
//                }

                String cameraFlashMode = params.getFlashMode();
                LOG.d("Current Flash mode : " + cameraFlashMode);

                if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    if (Camera.Parameters.FLASH_MODE_TORCH.equals(cameraFlashMode))
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    else
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                    if (Camera.Parameters.FLASH_MODE_ON.equals(cameraFlashMode))
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    else
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                }
            }

            try {
                mCamera.setParameters(params);
                mCamera.startPreview();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setOnTakePictureListener(OnTakePictureListener listener) {
        onTakePictureListener = listener;
    }

    @Override
    public void setOrientationEventListener(boolean isEnabled) {
        super.setOrientationEventListener(isEnabled);

        isOrientationEventAvailable = isEnabled;

        if (isEnabled) {
            if (mOrientationEventListener == null) {
                mOrientationEventListener = new OrientationEventListener(getContext(),
                        SensorManager.SENSOR_DELAY_NORMAL) {
                    @Override
                    public void onOrientationChanged(int orientation) {
//                        LOG.d("Orientation : " + orientation);
                        if (orientation != -1)
                            mLastOrientation = orientation;
                    }
                };
            }

            if (mOrientationEventListener != null && mOrientationEventListener.canDetectOrientation())
                mOrientationEventListener.enable();
        } else {
            if (mOrientationEventListener != null) {
                mOrientationEventListener.disable();
                mOrientationEventListener = null;
            }

            mLastOrientation = 0;
        }
    }

    /**
     * You must call this method to release Camera
     */
    @Override
    public void releaseCamera() {
        LOG.d("Release Camera");

        if (mOrientationEventListener != null)
            mOrientationEventListener.disable();

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);

            // Call stopPreview() to stop updating the preview surface.
//            mCamera.stopPreview();
            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void finishCamera() {
        super.finishCamera();

        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
            mOrientationEventListener = null;
        }

		onTakePictureListener = null;

        if (mCamera != null) {
			mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Bitmap rotateImage(Bitmap bitmap, int degrees) {
        if (bitmap == null) return bitmap;
        if (degrees % 360 == 0)
            return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        // TODO: Check if it is okay to recycle!!
        if (bitmap != null && bitmap != rotatedBitmap && !bitmap.isRecycled())
            bitmap.recycle();

        return rotatedBitmap;
    }

    private Bitmap cropImage(Bitmap bitmap) {
        if (mConfigureTransformMargin == null || bitmap == null || mLatestViewSize.x == 0 || mLatestViewSize.y == 0) return bitmap;
        if (mConfigureTransformMargin.left == 0 && mConfigureTransformMargin.top == 0 && mConfigureTransformMargin.right == 0 && mConfigureTransformMargin.bottom == 0) return bitmap;

        Bitmap rotatedBitmap = null;

        if ((bitmap.getWidth() <= bitmap.getHeight() && mLatestViewSize.x <= mLatestViewSize.y)
                || (bitmap.getWidth() >= bitmap.getHeight() && mLatestViewSize.x >= mLatestViewSize.y)) {
            final float xRatio = bitmap.getWidth() / mLatestViewSize.x;
            final float yRatio = bitmap.getHeight() / mLatestViewSize.y;
            LOG.d("Correct ratio!");
            LOG.d("xRatio : " + xRatio);
            LOG.d("yRatio : " + yRatio);

            rotatedBitmap = Bitmap.createBitmap(bitmap, (int) Math.abs(mConfigureTransformMargin.left * xRatio), (int) Math.abs(mConfigureTransformMargin.top * yRatio),
                    (int) (bitmap.getWidth() - Math.abs(mConfigureTransformMargin.right * xRatio * 2)), (int) (bitmap.getHeight() - Math.abs(mConfigureTransformMargin.bottom * yRatio * 2)));
            LOG.d("bitmap size : " + bitmap.getWidth() + " , " + bitmap.getHeight());
            LOG.d("Cropped size : " + rotatedBitmap.getWidth() + " , " + rotatedBitmap.getHeight());
        } else {
            final float xRatio = bitmap.getWidth() / mLatestViewSize.y;
            final float yRatio = bitmap.getHeight() / mLatestViewSize.x;
            LOG.d("inCorrect ratio!");
            LOG.d("xRatio : " + xRatio);
            LOG.d("yRatio : " + yRatio);

            rotatedBitmap = Bitmap.createBitmap(bitmap, (int) Math.abs(mConfigureTransformMargin.bottom * xRatio), (int) Math.abs(mConfigureTransformMargin.left * yRatio),
                    (int) (bitmap.getWidth() - Math.abs(mConfigureTransformMargin.top * xRatio * 2)), (int) (bitmap.getHeight() - Math.abs(mConfigureTransformMargin.right * yRatio * 2)));
            LOG.d("bitmap size : " + bitmap.getWidth() + " , " + bitmap.getHeight());
            LOG.d("Cropped size : " + rotatedBitmap.getWidth() + " , " + rotatedBitmap.getHeight());
        }

        // TODO: Check if it is okay to recycle!!
        if (bitmap != null && bitmap != rotatedBitmap && !bitmap.isRecycled())
            bitmap.recycle();

        return rotatedBitmap;
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "CameraLibrary");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("CameraLogger", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "CameraLibrary_"+ timeStamp +"_.jpg");

        return mediaFile;
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }
    }
}
