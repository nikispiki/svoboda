package com.example.svoboda;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;


public class CameraFragment extends Fragment implements View.OnClickListener, Callback{
    private static final String TAG = "CameraFragment";
    private CameraFragment instance = this;
    private static final int CAMERA_PIC_REQUEST = 1337;
    private ContextData contextData;
    private RelativeLayout statusDialog;
    private ProgressBar progressBar;
    private ImageView successImage;
    private ImageView failImage;
    private TextView verifyingText;
    private TextView successText;
    private TextView failText;
    private Boolean isValid = false;
    private ImageView mImageView;
    private String sessId;
    private Bitmap photoTaken;
    private SvobodaAPIClient svobodaAPIClient;
    private IOHandler ioHandler;


    @Override
    public View onCreateView(@NonNull  LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_camera, container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        contextData = ContextData.getInstance();
        /*
            Try to get the session id from ContextData class and if unsuccessful
            redirect user to LoginActivity
         */
        try
        {
            sessId = contextData.userProfile.getString("sess_id");
        }
        catch (JSONException e)
        {
            Intent openMapIntent = new Intent(getActivity(), LoginActivity.class);
            startActivity(openMapIntent);
        }

        ioHandler = new IOHandler(getActivity());
        svobodaAPIClient = SvobodaAPIClient.getInstance();
        statusDialog = view.findViewById(R.id.statusDialog);
        progressBar = view.findViewById(R.id.progressSpinner);
        successImage = view.findViewById(R.id.success);
        failImage = view.findViewById(R.id.fail);
        verifyingText = view.findViewById(R.id.verifyingText);
        successText = view.findViewById(R.id.successText);
        failText = view.findViewById(R.id.failText);
        mImageView = view.findViewById(R.id.cameraImageView);

        launchCamera();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            /*
                When the user clicks the dialog it redirects him to either the Map
                ot the Gallery based on the result of the validation
             */
            case R.id.statusDialog:
                if (isValid)
                {
                    selectNavigationItemFromMenu(R.id.nav_gallery);
                }
                else
                {
                    selectNavigationItemFromMenu(R.id.nav_map);
                }
                break;
        }
    }

    /*
        We use this to manually select an item from the menu
        so we can redirect to it
     */
    private void selectNavigationItemFromMenu(Integer item)
    {
        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
        if (navigationView != null)
        {
            navigationView.setCheckedItem(item);
            navigationView.getMenu().performIdentifierAction(item, 0);
        }
    }

    private void launchCamera()
    {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    private void setProgressDialogVisibility(int visibility)
    {
        progressBar.setVisibility(visibility);
        verifyingText.setVisibility(visibility);
    }

    private void setFailDialogVisibility(int visibility)
    {
        failImage.setVisibility(visibility);
        failText.setVisibility(visibility);
    }

    private void setSuccessDialogVisibility(int visibility, String locationName)
    {
        successImage.setVisibility(visibility);
        successText.append(" " + locationName);
        successText.setVisibility(visibility);
    }

    /*
        To validate the picture we get the current position of the user,
        the leftmostPolygonPoint and the rightmostPolygonPoint and we send them to
        the server along with the base64 encoded picture. The server creates a triangle
        from these 3 points and checks if there is a location within this triangle.
        If there is it gets the closest location to the user out of all those points
        and either adds it to the users gallery as a new found location or updates the
        picture if the user has already found this location before.
     */
    private void validatePicture()
    {
        LatLng currentLocation = contextData.currentlLocation;
        LatLng leftmostPolygonPoint = contextData.leftmostPolygonPoint;
        LatLng rightmostPolygonPoint = contextData.rightmostPolygonPoint;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        photoTaken.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedPhoto = Base64.encodeToString(byteArray, Base64.DEFAULT);

        JSONObject requestData = new JSONObject();
        try
        {
            requestData
                    .put("sessId", sessId)
                    .put("currentLocationLat", currentLocation.latitude)
                    .put("currentLocationLng", currentLocation.longitude)
                    .put("leftBoundryPointLat", leftmostPolygonPoint.latitude)
                    .put("leftBoundryPointLng", leftmostPolygonPoint.longitude)
                    .put("rightBoundryPointLat", rightmostPolygonPoint.latitude)
                    .put("rightBoundryPointLng", rightmostPolygonPoint.longitude)
                    .put("pic", encodedPhoto);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        /*
            Make request to the server to validate the picture using the SvobodaAPIClient.
            When a response is received or the request fails the corresponding
            functions onResponse() and onFailure() are called.
         */
        svobodaAPIClient.makeRequest(contextData.validationUrl, requestData, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        /*
            We step into this if we get an image from the camera. We get the image as a
            bitmap and display it in the mImageView. Then we display the status dialog and
            the processing dialog nested inside it and call the validation function.
         */
        if (requestCode == CAMERA_PIC_REQUEST && data != null && resultCode == RESULT_OK)
        {
            // Get a base64 encoded string of the photo
            photoTaken = (Bitmap) data.getExtras().get("data");
            if (photoTaken != null)
            {
                mImageView.setImageBitmap(photoTaken);
                statusDialog.setVisibility(View.VISIBLE);
                setProgressDialogVisibility(View.VISIBLE);
                validatePicture();
            }
        }
        /*
            Otherwise we redirect the user back to the Map
         */
        else
        {
            selectNavigationItemFromMenu(R.id.nav_map);
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
                    setProgressDialogVisibility(View.GONE);
                    setFailDialogVisibility(View.VISIBLE);
                    isValid = false;
                }
            });
        }
        else
        {
            /*
                If the validation was successful we display the success dialog
                and save the picture to the internal storage
             */
            if (response.body() != null)
            {
                try
                {
                    JSONObject jsonData = new JSONObject(response.body().string());
                    if (photoTaken != null)
                    {
                        ioHandler.writeFileToInternalData("gallery", jsonData.getString("picture_name"), photoTaken);
                    }
                    response.body().close();
                    String locationName = jsonData.getString("location_name");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            setProgressDialogVisibility(View.GONE);
                            setSuccessDialogVisibility(View.VISIBLE, locationName);
                            isValid = true;
                        }
                    });
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        /*
            Finally we allow the user to click the dialog
         */
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                statusDialog.setOnClickListener(instance);
            }
        });
    }
}
