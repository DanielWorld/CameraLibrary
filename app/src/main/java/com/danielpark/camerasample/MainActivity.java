package com.danielpark.camerasample;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danielpark.camera.CameraApi1Activity;
import com.danielpark.camera.CameraApiChecker;
import com.danielpark.camera.CameraPreview;
import com.danielpark.camera.CameraSurfacePreview;
import com.danielpark.camera.util.CameraLogger;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_PERMISSION = 1001;

//    CameraSurfacePreview cameraPreview;
    CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView bottomView = (TextView) findViewById(R.id.bottomView);
        bottomView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraPreview.autoFocus();
            }
        });

        // Daniel (2016-08-23 10:45:00): Turn on CameraLogger Log switch
        CameraLogger.enable();

        try {
            CameraApiChecker.getInstance().build(this);

//            Intent intent = new Intent(this, CameraApi1Activity.class);
//            startActivity(intent);

            FrameLayout f = (FrameLayout) findViewById(R.id.container);
//            cameraPreview = new CameraSurfacePreview(this);
            cameraPreview = new CameraPreview(this);
            f.addView(cameraPreview);

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
}
