package com.example.svoboda;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView.Renderer;

import com.example.svoboda.arobject.BackgroundRenderHelper;
import com.example.svoboda.arobject.CustomRenderer;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackedImage;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class InstantTrackerRenderer implements Renderer {

    public static final String TAG = InstantTrackerRenderer.class.getSimpleName();

    private int surfaceWidth;
    private int surfaceHeight;

    private CustomRenderer customRenderer;
    private float posX;
    private float posY;
    private Activity activity;

    private BackgroundRenderHelper backgroundRenderHelper;

    InstantTrackerRenderer(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight);

        TrackingState state = TrackerManager.getInstance().updateTrackingState();
        TrackingResult trackingResult = state.getTrackingResult();

        TrackedImage image = state.getImage();
        float[] backgroundPlaneProjectionMatrix = CameraDevice.getInstance().getBackgroundPlaneProjectionMatrix();
        backgroundRenderHelper.drawBackground(image, backgroundPlaneProjectionMatrix);

        if (trackingResult.getCount() == 0) {
            return;
        }

        float [] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();

        Trackable trackable = trackingResult.getTrackable(0);

        GLES30.glEnable(GLES20.GL_DEPTH_TEST);

        customRenderer.setTransform(trackable.getPoseMatrix());
        customRenderer.setTranslate(posX+0.5f, posY-0.2f, 0.4f);
        customRenderer.setProjectionMatrix(projectionMatrix);
        customRenderer.draw();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        surfaceWidth = width;
        surfaceHeight = height;

        customRenderer.setScale(0.1f, 0.1f, 0.1f);
        customRenderer.setRotation(90.0f, -1.0f, 0.0f, 0.0f);

        MaxstAR.onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        customRenderer = new CustomRenderer();
        Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("svoboda_test.png", activity.getAssets());
        customRenderer.setTextureBitmap(bitmap);

        backgroundRenderHelper = new BackgroundRenderHelper();
    }

    void setTranslate(float x, float y) {
        posX += x;
        posY += y;
    }

    void resetPosition() {
        posX = 0;
        posY = 0;
    }
}
