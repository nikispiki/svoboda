package com.example.svoboda;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.svoboda.util.SampleUtil;
import com.google.android.gms.maps.model.LatLng;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.SensorDevice;
import com.maxst.ar.TrackerManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class InstantTrackerFragment extends ARFragment implements View.OnClickListener, Callback {

    private InstantTrackerRenderer instantTargetRenderer;
    private int preferCameraResolution = 0;
    private Button showDescriptionButton;
    private GLSurfaceView glSurfaceView;
    private ContextData contextData;
    private SvobodaAPIClient svobodaAPIClient;
    private InstantTrackerFragment instance = this;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_instant_tracker, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        contextData = ContextData.getInstance();
        svobodaAPIClient = SvobodaAPIClient.getInstance();
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.CAMERA}, 50);
        }

        showDescriptionButton = getActivity().findViewById(R.id.start_tracking);
        showDescriptionButton.setOnClickListener(this);

        instantTargetRenderer = new InstantTrackerRenderer(getActivity());
        glSurfaceView = getActivity().findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setRenderer(instantTargetRenderer);

        preferCameraResolution = getActivity().getSharedPreferences(SampleUtil.PREF_NAME, Activity.MODE_PRIVATE).getInt(SampleUtil.PREF_KEY_CAM_RESOLUTION, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        glSurfaceView.onResume();
        SensorDevice.getInstance().start();
        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_INSTANT);

        ResultCode resultCode = ResultCode.Success;

        switch (preferCameraResolution) {
            case 0:
                resultCode = CameraDevice.getInstance().start(0, 640, 480);
                break;

            case 1:
                resultCode = CameraDevice.getInstance().start(0, 1280, 720);
                break;

            case 2:
                resultCode = CameraDevice.getInstance().start(0, 1920, 1080);
                break;
        }

        if (resultCode != ResultCode.Success) {
            Toast.makeText(getActivity(), "Failed to open camera", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        MaxstAR.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        glSurfaceView.onPause();

        TrackerManager.getInstance().quitFindingSurface();
        TrackerManager.getInstance().stopTracker();
        CameraDevice.getInstance().stop();
        SensorDevice.getInstance().stop();

        MaxstAR.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
        This makes a request to the server to get the description
        of the location the user is facing. For now, however, the only
        thing the server does is check if there is a location inside
        the polygon points we send it and the server returns
        success or failure based on that.
     */
    private void getNearestLocationDescription()
    {
        LatLng currentLocation = contextData.currentlLocation;
        LatLng leftmostPolygonPoint = contextData.leftmostPolygonPoint;
        LatLng rightmostPolygonPoint = contextData.rightmostPolygonPoint;

        JSONObject requestData = new JSONObject();
        try
        {
            requestData
                    .put("currentLocationLat", currentLocation.latitude)
                    .put("currentLocationLng", currentLocation.longitude)
                    .put("leftBoundryPointLat", leftmostPolygonPoint.latitude)
                    .put("leftBoundryPointLng", leftmostPolygonPoint.longitude)
                    .put("rightBoundryPointLat", rightmostPolygonPoint.latitude)
                    .put("rightBoundryPointLng", rightmostPolygonPoint.longitude);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        /*
            Make request to the server to get the description
            for the nearest location using the SvobodaAPIClient.
            When a response is received or the request fails the corresponding
            functions onResponse() and onFailure() are called.
         */
        svobodaAPIClient.makeRequest(contextData.descriptionUrl, requestData, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_tracking:
                String text = showDescriptionButton.getText().toString();
                if (text.equals(getString(R.string.show_description))) {
                    // Disable description button
                    showDescriptionButton.setOnClickListener(null);
                    // Request description
                    getNearestLocationDescription();
                } else {
                    TrackerManager.getInstance().quitFindingSurface();
                    showDescriptionButton.setText(getString(R.string.show_description));
                }
                break;
        }
    }

    /*
    Called on request failure
 */
    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e)
    {
        e.printStackTrace();
    }

    /*
        Called when a response is received from the request
     */
    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response)
    {
        if (!response.isSuccessful())
        {
            /*
                If the validation was unsuccessful then we display the fail dialog
             */
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Unknown location", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else
        {
            /*
                On success from the server we show the default
                AR description.
             */
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    TrackerManager.getInstance().findSurface();
                    instantTargetRenderer.resetPosition();
                    showDescriptionButton.setText(getString(R.string.hide_description));
                }
            });
        }
        /*
            Finally we allow the user to click the description button
         */
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                showDescriptionButton.setOnClickListener(instance);
            }
        });
    }
}
