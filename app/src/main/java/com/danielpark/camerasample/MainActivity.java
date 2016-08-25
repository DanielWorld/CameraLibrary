package com.danielpark.camerasample;

import android.Manifest;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danielpark.camera.CameraApiChecker;
import com.danielpark.camera.util.AutoFitTextureView;
import com.danielpark.camera.util.CameraLogger;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final int REQUEST_PERMISSION = 1001;

    AutoFitTextureView cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout containerView = (FrameLayout) findViewById(R.id.container);
        TextView topView = (TextView) findViewById(R.id.topView);
        RelativeLayout bottomView = (RelativeLayout) findViewById(R.id.bottomView);
        Button button = (Button) findViewById(R.id.autoFocusBtn);
        Button button2 = (Button) findViewById(R.id.takePictureBtn);

        button.setOnClickListener(this);
        button2.setOnClickListener(this);

        // Daniel (2016-08-23 10:45:00): Turn on CameraLogger Log switch
        CameraLogger.enable();

        try {
            cameraPreview =  CameraApiChecker.getInstance().build(this);
            containerView.addView(cameraPreview);


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
        }
    }
}
