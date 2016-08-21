package com.danielpark.camerasample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.danielpark.camera.CameraApiChecker;
import com.danielpark.camera.CameraPreview;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout camera_preview = (FrameLayout) findViewById(R.id.camera_preview);
        camera_preview.addView(new CameraPreview(this));

        try {
            CameraApiChecker.getInstance().build(this);
        } catch (UnsupportedOperationException e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
