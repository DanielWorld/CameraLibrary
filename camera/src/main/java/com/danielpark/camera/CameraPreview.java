package com.danielpark.camera;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.danielpark.camera.util.Logger;

import java.util.List;

/**
 * Copyright (c) 2014-2016 op7773hons@gmail.com
 * Created by Daniel Park on 2016-08-21.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private Logger LOG = Logger.getInstance();

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        LOG.d("surfaceCreated()");

        // The Surface has been created, now open the Camera and tell the camera where to draw the preview.
        try {
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        LOG.d("surfaceChanged() : " + width + " / " + height);

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (surfaceHolder.getSurface() == null) {
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

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        LOG.d("surfaceDestroyed()");
        
        if (mCamera != null)
            mCamera.release();
    }
}
