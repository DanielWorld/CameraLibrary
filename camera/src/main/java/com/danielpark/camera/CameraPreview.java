package com.danielpark.camera;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.danielpark.camera.listeners.OnTakePictureListener;
import com.danielpark.camera.util.AutoFitTextureView;
import com.danielpark.camera.util.DeviceUtil;

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

    private OnTakePictureListener onTakePictureListener;

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

    @Override
    public void openCamera(SurfaceTexture surfaceTexture, int width, int height) {
        LOG.d("openCamera() : " + width + " , " + height);

        if (mCamera == null)
            mCamera = Camera.open();

        setUpCameraOutput(width, height);
        configureTransform(surfaceTexture, width, height);
    }

    /**
     * Check if current device gets correct orientation compares to preview size
     * @return
     */
    private boolean isCorrectOrientation() {
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
    private void setUpCameraOutput(int width, int height) {
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
        mPreviewSize = chooseOptimalSize(mCamera.getParameters().getSupportedPreviewSizes(),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight,
                largestPreviewSize);
        LOG.d("7. Optimal Preview size : " + mPreviewSize.width + " , " + mPreviewSize.height);

        // 8. choose Optimal Picture size!
        mPictureSize = chooseOptimalSize(mCamera.getParameters().getSupportedPictureSizes(),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight,
                largestPictureSize);
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

//        int screenWidth = DeviceUtil.getResolutionWidth(getContext());
//        int screenHeight = DeviceUtil.getResolutionHeight(getContext());
//
//        if ((screenWidth > screenHeight && mPreviewSize.width > mPreviewSize.height)
//                || (screenWidth < screenHeight && mPreviewSize.width < mPreviewSize.height)) {
//            setAspectRatio(mPictureSize.width, mPreviewSize.height);
//        } else {
//            setAspectRatio(mPictureSize.height, mPreviewSize.width);
//        }

//        int width1 = MeasureSpec.getSize(getMeasuredWidth());
//        int height1 = MeasureSpec.getSize(getMeasuredHeight());
//        LOG.d("mesured size : " + width1 + " , " + height1);
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
            LOG.d("rotation : " + ORIENTATIONS.get(rotation));
            LOG.d("Correct Orientation : " + isCorrectOrientation());
            LOG.d("Sensor orientation : " + mSensorOrientation);

            if (isCorrectOrientation()) {
                if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                    // Daniel (2016-08-26 17:34:05): Reverse dst size
                    bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize.height,
                            (float) viewWidth / mPreviewSize.width);
                    matrix.postScale(scale, scale, centerX, centerY);
                    matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                } else if (Surface.ROTATION_0 == rotation || Surface.ROTATION_180 == rotation) {
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize.height,
                            (float) viewWidth / mPreviewSize.width);
                    matrix.postScale(scale, scale, centerX, centerY);

                    if (Surface.ROTATION_180 == rotation)
                        matrix.postRotate(180, centerX, centerY);
                }

            } else {
                if (Surface.ROTATION_0 == rotation || Surface.ROTATION_180 == rotation) {
                    // Daniel (2016-08-26 17:34:05): Reverse dst size
                    bufferRect = new RectF(0, 0, mPreviewSize.height, mPreviewSize.width);
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize.height,
                            (float) viewWidth / mPreviewSize.width);
                    matrix.postScale(scale, scale, centerX, centerY);

                    if (Surface.ROTATION_0 == rotation)
                        matrix.postRotate(90 - mSensorOrientation, centerX, centerY);
                    else
                        matrix.postRotate(180 - mSensorOrientation, centerX, centerY);
                } else if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                    float scale = Math.max(
                            (float) viewHeight / mPreviewSize.height,
                            (float) viewWidth / mPreviewSize.width);
                    matrix.postScale(scale, scale, centerX, centerY);

                    if (Surface.ROTATION_90 == rotation)
                        matrix.postRotate(270, centerX, centerY);
                    if (Surface.ROTATION_270 == rotation)
                        matrix.postRotate(90, centerX, centerY);
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
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    LOG.d("onAutoFocus() : " + success);
                }
            });
        }
    }

    @Override
    public void takePicture() {
        super.takePicture();

        if (mCamera != null) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    LOG.d("onAutoFocus() : " + success);

                    // Daniel (2016-11-03 16:12:52): Start taking picture
                    captureStillPicture();
                }
            });
        }
    }

    /**
     * Try to capture a still image from preview
     */
    private void captureStillPicture() {
        try {
            if (mCamera != null)
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                        if (bytes != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                            // Daniel (2016-08-26 14:01:20): Current Device rotation
                            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                            int displayRotation = windowManager.getDefaultDisplay().getRotation();
                            LOG.d("Current device rotation : " + ORIENTATIONS.get(displayRotation));

                            int result = (mSensorOrientation - ORIENTATIONS.get(displayRotation) + 360) % 360;

                            Bitmap rotatedBitmap = null;

                            if (result % 360 != 0)
                                rotatedBitmap = rotateImage(bitmap, result);

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

                            } catch (IOException e) {
                            } finally {
                                LOG.d("File path : " + pictureFile.getAbsolutePath());

                                if (onTakePictureListener != null && pictureFile != null)
                                    onTakePictureListener.onTakePicture(pictureFile);

                                try {
                                    if (mCamera != null) {
                                        mCamera.stopPreview();
                                        mCamera.startPreview();
                                    }
                                } catch (Exception egnored){}
                            }
                        }
                    }
                });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void flashTorch() {
        super.flashTorch();

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
                return;
            }
        }
    }

    @Override
    public void setOnTakePictureListener(OnTakePictureListener listener) {
        onTakePictureListener = listener;
    }

    /**
     * You must call this method to release Camera
     */
    @Override
    public void releaseCamera() {
        if (mCamera != null) {
            LOG.d("Release Camera");
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

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        onTakePictureListener = null;
    }

    private Bitmap rotateImage(Bitmap bitmap, int degrees) {
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
                + "CameraLibrary.jpg");

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
