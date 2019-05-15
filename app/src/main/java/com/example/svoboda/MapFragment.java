package com.example.svoboda;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class MapFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        Callback {
    private static final String TAG = "MapFragment";
    private GoogleMap mGoogleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private SvobodaAPIClient svobodaAPIClient;
    private ArrayList<CustomMarker> mapMarkers;
    private ContextData contextData;
    private LatLng currentLocation;
    private float currentLocationBearing;
    private IOHandler ioHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        svobodaAPIClient = SvobodaAPIClient.getInstance();
        contextData = ContextData.getInstance();
        mapMarkers = new ArrayList<>();
        ioHandler = new IOHandler(getActivity());

        SupportMapFragment mapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.googlemap);
        mapFragment.getMapAsync(this);
    }

    /*
        When the view is destroyed stop asking for location updates
     */
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if (googleApiClient != null)
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        // Removes the button for focusing the screen on current location
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        // Disable moving the map
        mGoogleMap.getUiSettings().setScrollGesturesEnabled(false);
        /*
            Get the last location, if there is one, and move the
            camera to that location with a zoom
         */
        if (contextData.currentlLocation != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(contextData.currentlLocation, 16));
        }

        /*
            Customise the styling of the base map using a JSON object defined
            in a raw resource file.
         */
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getActivity(), R.raw.map_retro_style));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }


        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }
        else {
            ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 50);
        }
    }


    /*
        The main entry point for Google Play services integration.
        Creates A google api client and connects to access the
        Google APIs provided in the Google Play services library
     */
    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    // Called when the google api client connects
    @Override
    public void onConnected(Bundle bundle)
    {

        /*
            LocationRequest objects are used to request
            location updates from the FusedLocationProviderApi.
            We set the request interval to 5000 milliseconds (5 seconds)
        */
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            // Start requesting location updates
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    // Called when the client is temporarily in a disconnected state.
    @Override
    public void onConnectionSuspended(int i)
    {
        Toast.makeText(getActivity(), "Connection to Google Play services suspended", Toast.LENGTH_SHORT).show();
        if (googleApiClient != null)
        {
            // Stop asking for location updates
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    // Called when the client fails to connect
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Toast.makeText(getActivity(), "Connection to Google Play services failed", Toast.LENGTH_SHORT).show();
    }

    /*
        Creates the display bitmap for the custom marker for how its rendered on the map
     */
    private Bitmap createCustomMarkerDisplay(Context context, String pictureName)
    {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
        CircleImageView markerImage = marker.findViewById(R.id.location_icon);
        /*
            If we get a picture name the we try to open the file with that name and set
            it as the picture inside the marker view. If this fails at some point it puts
            an image of a question mark inside the markers view.
         */
        if (pictureName != null)
        {
            File image = ioHandler.getFile("gallery", pictureName);
            try
            {
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(image));
                markerImage.setImageBitmap(b);
            }
            catch (FileNotFoundException e)
            {
                markerImage.setImageResource(R.drawable.ic_question_x);
            }
        }
        else
        {
            markerImage.setImageResource(R.drawable.ic_question_x);

        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(marker.getWidth(), marker.getHeight()));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap displayBitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(displayBitmap);
        marker.draw(canvas);

        return displayBitmap;
    }

    private void createOrUpdateCustomMapMarker(LatLng location, String locationName, String pictureName, Boolean found) throws ExecutionException, InterruptedException
    {
        for (CustomMarker marker : mapMarkers)
        {
            /*
                If a marker exists for the current location and the picture name sotred
                in the internal storage is different from the picture name received from the server
                (this means the picture was updated for the location) then we delete the old
                picture from the internal storage and call the updateMarkerDisplay function
                on the custom marker remove the old picture and set the new one as the
                markers icon.
             */
            if (marker.getPosition().equals(location) && !marker.getPictureName().equals(pictureName))
            {
                if (!marker.getPictureName().equals(pictureName))
                {
                    // Delete old picture of the location
                    File image = ioHandler.getFile("gallery", marker.getPictureName());
                    image.delete();
                    Bitmap markerDisplay = createCustomMarkerDisplay(getActivity(), pictureName);
                    marker.updateMarkerDisplay(markerDisplay, pictureName);
                }
                return;
            }
        }
        /*
            If a marker does not exist we create a new one and add it to the set of custom
            markers.
         */
        Bitmap markerDisplay = createCustomMarkerDisplay(getActivity(), pictureName);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
        if (found)
        {
            markerOptions.title(locationName);
        }
        else
        {
            markerOptions.title("Unknown");
        }
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerDisplay));
        Marker m = mGoogleMap.addMarker(markerOptions);
        CustomMarker customMarker = new CustomMarker(m, markerDisplay, pictureName, found);
        mapMarkers.add(customMarker);
    }

    /*
        Called when we receive a location update
     */
    @Override
    public void onLocationChanged(Location location)
    {
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        currentLocationBearing = location.getBearing();
        /*
            If this is the first current location found for the
            user then we move the camera to it and also zoom in on
            it, otherwise we don't zoom.
         */
        if (contextData.currentlLocation == null)
        {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));
        }
        else
        {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
        // Save the current location in the ContextData for easy access
        contextData.currentlLocation = currentLocation;

        // Rotates the camera of the map in the direction the user is moving
        CameraPosition camPos = CameraPosition
                .builder(mGoogleMap.getCameraPosition())
                .bearing(location.getBearing())
                .build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));

        // We add the current location latitude and longitude as query parameters
        String url = null;
        try
        {
            url = Uri.parse(contextData.locationsUrl)
                    .buildUpon()
                    .appendQueryParameter("user_id", contextData.userProfile.getString("id"))
                    .appendQueryParameter("lat", String.valueOf(contextData.currentlLocation.latitude))
                    .appendQueryParameter("lng", String.valueOf(contextData.currentlLocation.longitude))
                    .build()
                    .toString();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        /*
            Make request to the server for the locations around the user using the SvobodaAPIClient.
            When a response is received or the request fails the corresponding functions
            onResponse() and onFailure() are called.
         */
        svobodaAPIClient.makeRequest(url, null,this);
    }

    @Override
    public void onFailure(@NonNull  Call call,@NonNull IOException e)
    {
        e.printStackTrace();
    }

    @Override
    public void onResponse(@NonNull  Call call,@NonNull Response response) throws IOException
    {
        if (!response.isSuccessful())
        {
            throw new IOException("Unexpected code " + response);
        }
        else
        {
            String responseBody = response.body().string();

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run()
                {
                    try
                    {
                        /*
                            We take the current location of the user and the direction he is heading
                            and use the to compute a point that is +30 degrees of the direction and
                            100 metres away from the current location (rightmostPolygonPoint) and a
                            point that is -30 degrees of the direction and 100 metres away from the
                            current location (leftmostPolygonPoint). These points are used by the
                            camera fragment in its validatePicture() function.
                         */
                        contextData.leftmostPolygonPoint = SphericalUtil.computeOffset(currentLocation, 100, currentLocationBearing - 30);
                        contextData.rightmostPolygonPoint = SphericalUtil.computeOffset(currentLocation, 100, currentLocationBearing + 30);

                        /*
                            We add custom markers for each location received from the server
                         */
                        JSONArray locations = new JSONArray(responseBody);
                        for (int index = 0; index < locations.length(); index++) {
                            JSONObject location = locations.getJSONObject(index);
                            LatLng locationLatLng = new LatLng(
                                    location.getDouble("lat"),
                                    location.getDouble("lng")
                            );
                            createOrUpdateCustomMapMarker(
                                    locationLatLng,
                                    location.getString("name"),
                                    location.getString("picture_name"),
                                    location.getBoolean("found")
                            );
                        }
                    }
                    catch (JSONException e)
                    {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    catch (ExecutionException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            Log.i(TAG, responseBody);
            response.body().close();
        }
    }
}
