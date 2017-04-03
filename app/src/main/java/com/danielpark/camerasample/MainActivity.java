package com.danielpark.camerasample;

import android.Manifest;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danielpark.camera.CameraApiChecker;
import com.danielpark.camera.listeners.OnTakePictureListener;
import com.danielpark.camera.util.AutoFitTextureView;
import com.danielpark.camera.util.CameraLogger;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnTakePictureListener {

    final int REQUEST_PERMISSION = 1001;

    AutoFitTextureView cameraPreview;
    ImageView thumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout containerView = (FrameLayout) findViewById(R.id.container);
        TextView topView = (TextView) findViewById(R.id.topView);
        RelativeLayout bottomView = (RelativeLayout) findViewById(R.id.bottomView);
        ImageButton button = (ImageButton) findViewById(R.id.autoFocusBtn);
        ImageButton button2 = (ImageButton) findViewById(R.id.takePictureBtn);
        ImageButton button3 = (ImageButton) findViewById(R.id.flashBtn);
        ImageButton button4 = (ImageButton) findViewById(R.id.settingBtn);

        thumbnail = (ImageView) findViewById(R.id.imageView);

        button.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);

        // Daniel (2016-08-23 10:45:00): Turn on CameraLogger Log switch
        CameraLogger.enable();

        try {
            cameraPreview =  CameraApiChecker.getInstance().build(this);
            containerView.addView(cameraPreview);

            /**
             * Daniel (2016-11-05 18:42:58): It is required to listen taking a picture event, and auto-focus event
             */
            cameraPreview.setOnTakePictureListener(this);
            cameraPreview.setOrientationEventListener(true);

            cameraPreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            cameraPreview.autoFocus();
                            return true;
                    }
                    return false;
                }
            });

        } catch (UnsupportedOperationException e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_PERMISSION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
//			// Daniel (2016-05-17 14:37:25): TextureView 여부를 체크한 뒤 , textureView open!
            if (cameraPreview.isAvailable()) {
                Log.d("CameraLogger", "TextureView is available!");
                cameraPreview.openCamera(cameraPreview.getSurfaceTexture(), cameraPreview.getWidth(), cameraPreview.getHeight());
            } else {
                Log.d("CameraLogger", "TextureView is not available!");
            }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // TODO: You can build logic later
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();

        switch (id){
            case R.id.autoFocusBtn:
                cameraPreview.autoFocus();
                break;
            case R.id.takePictureBtn:
                cameraPreview.takePicture();
                break;
            case R.id.flashBtn:
                cameraPreview.flashToggle();
                break;
            case R.id.settingBtn:
                break;
        }
    }

    @Override
    protected void onDestroy() {

        if (cameraPreview != null)
            cameraPreview.finishCamera();

        super.onDestroy();
    }

    @Override
    public void onTakePicture(@NonNull File file) {
        Toast.makeText(this, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        try {
            if (thumbnail != null)
                thumbnail.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLensFocused(boolean isFocused) {
        Toast.makeText(this, "Lens focused : " + isFocused, Toast.LENGTH_SHORT).show();
    }
}
