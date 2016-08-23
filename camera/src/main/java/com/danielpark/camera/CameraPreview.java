package com.danielpark.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.TextView;

import com.danielpark.camera.util.Logger;

import java.util.List;

/**
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-21.
 */
public class CameraPreview extends TextureView{

    private Logger LOG = Logger.getInstance();

    private Camera mCamera;
    private Camera.Size mPreviewSize;   // Daniel (2016-08-23 16:52:44): Camera preview size;

    public CameraPreview(Context context) {
        super(context);

        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private static final SparseArray ORIENTATIONS = new SparseArray();

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
            mCamera.stopPreview();
            mCamera.release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    /**
     * Open the camera
     * @param width
     * @param height
     */
    public void openCamera(SurfaceTexture surfaceTexture, int width, int height) {
        LOG.d("openCamera : " + width + " / " + height);

        try {
            mCamera = Camera.open();
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void configureTransform(SurfaceTexture surfaceTexture, int width, int height) {
        if (null == mPreviewSize)
            return;
        
        LOG.d("configureTransform : " + width + " / " + height);
        
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
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size mPreviewSize;

            List<Camera.Size> cameraSize = parameters.getSupportedPreviewSizes();
            mPreviewSize = cameraSize.get(0);

            // get best supported preview size
            for (Camera.Size s : cameraSize) {
                if ((s.width * s.height) > (mPreviewSize.width * mPreviewSize.height)) {
                    mPreviewSize = s;
                }
            }
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
//            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                mCamera.setDisplayOrientation((cameraInfo.orientation - rotation + 360) % 360);
//            } else {
//                mCamera.setDisplayOrientation((cameraInfo.orientation + rotation) % 360);
//            }

            int orientation = getContext().getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            }
            else {

            }

            LOG.d("cameraInfo orientation : " + cameraInfo.orientation);

            mCamera.setDisplayOrientation((cameraInfo.orientation) % 360);

//            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
