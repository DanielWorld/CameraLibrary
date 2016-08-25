package com.danielpark.camera.util;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.danielpark.camera.listeners.ControlInterface;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 * <br><br>
 * Copyright (C) 2014-2016 daniel@bapul.net
 * Created by Daniel on 2016-08-23.
 */
public class AutoFitTextureView extends TextureView implements ControlInterface{

    protected Logger LOG = Logger.getInstance();

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            Log.d("CameraLogger", "onMeasure() : " + width + " / " + height);
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                Log.d("CameraLogger", "onMeasure() : " + width + " / " + width * mRatioHeight / mRatioWidth);
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                Log.d("CameraLogger", "onMeasure() : " + height * mRatioWidth / mRatioHeight + " / " + height);
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public void autoFocus() {

    }

    @Override
    public void takePicture() {

    }
}
