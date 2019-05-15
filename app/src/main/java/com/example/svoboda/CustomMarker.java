package com.example.svoboda;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/*
    Class for the custom markers we display on the map. It holds the google map
    marker itself, the bitmap image of how the marker looks (this is so we can delete
    the bitmap later, since we cant get it from the marker itself) and the picture name.
    The picture name is the name of the picture in the internal storage that we display
    as a marker icon. We also use this to check if the image in the internal storage has been
    updated to a diffent one in which case we use the updateMarkerDisplay() function.
 */
class CustomMarker {
    private Marker marker;
    private Bitmap display;
    private String pictureName;

    CustomMarker(Marker m, Bitmap d, String p, Boolean f)
    {
        marker = m;
        display = d;
        pictureName = p;
    }

    String getPictureName()
    {
        return pictureName;
    }

    LatLng getPosition()
    {
        return marker.getPosition();
    }

    /*
        When called this deletes the created bitmap for the picture that we put as
        an icon in the marker and we replace it with a new one (newDisplay)
     */
    void updateMarkerDisplay(Bitmap newDisplay, String newPictureName)
    {
        display.recycle();
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(newDisplay));
        display = newDisplay;
        pictureName = newPictureName;
    }
}
