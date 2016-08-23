package com.danielpark.camerasample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.danielpark.camera.CameraApiChecker;
import com.danielpark.camera.CameraPreview;
import com.danielpark.camera.util.CameraLogger;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Daniel (2016-08-23 10:45:00): Turn on CameraLogger Log switch
        CameraLogger.enable();

        FrameLayout camera_preview = (FrameLayout) findViewById(R.id.camera_preview);
        camera_preview.addView(new CameraPreview(this));

        try {
            CameraApiChecker.getInstance().build(this);
        } catch (UnsupportedOperationException e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
