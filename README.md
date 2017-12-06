# CameraLibrary

Camera preview which uses TextureView and it supports all of devices.

## Gradle

<pre>
dependencies {
    compile 'com.danielworld:camera-library:1.1.6'
}
</pre>

## Features

- Support all devices.
- Lens, device, sensor orientation were applied.

## Code

<pre>
AutoFitTextureView cameraPreview;
FrameLayout container;
...
@Override
protected void onCreate(Bundle savedInstanceState) {
    ...
    
    try {
        cameraPreview = CameraApiChecker.getInstance()
                        .setOrientation(CameraApiChecker.CameraOrientation.Landscape)
                        .setCameraType(CameraApiChecker.CameraType.CAMERA_FACING_FRONT)
                        .build(this);

        // Simply add camera preview to container view.               
        container.addView(cameraPreview);

        // Daniel (2016-11-05 18:42:58): It is required to listen taking a picture event, and auto-focus event.
        cameraPreview.setOnCameraPreviewListener(new OnCameraPreviewListener() {
                @Override
                public void onTakePicture(File file) {
                      // .. get file when 'cameraPreview.takePicture()' was invoked!
                }

                @Override
                public void onLensFocused(boolean isFocused) {
                      // .. get lens focused result when 'cameraPreview.autoFocus()' was invoked!
                }
            });
            
       // flash toggle (cameraPreview.flashToggle())
       // get thumbnail (cameraPreview.getThumbnail(ratio) : from 0.01f ~ 1.0f)
        
    } catch (UnsupportedOperationException | IOException e){
        
        //  Required permissions!
        //  android.permission.CAMERA, android.permission.WRITE_EXTERNAL_STORAGE
        //  android.permission.READ_EXTERNAL_STORAGE
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        finish();
    }
}

@Override
protected void onResume() {
    super.onResume();

    try {
        if (cameraPreview != null)
            cameraPreview.openCamera(cameraPreview.getSurfaceTexture(), cameraPreview.getWidth(), cameraPreview.getHeight());
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@Override
protected void onPause() {
    super.onPause();

    if (cameraPreview != null)
        cameraPreview.releaseCamera();
}

@Override
protected void onDestroy() {

    if (cameraPreview != null)
        cameraPreview.finishCamera();

    super.onDestroy();
}
</pre>
