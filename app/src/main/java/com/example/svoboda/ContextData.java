package com.example.svoboda;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;


/*
    Here we have a singleton which has the data that is distributed all over the app.
 */
class ContextData {

    private static ContextData instance;
    private String protocol = "http";
    // Change this to your local IP
    private String inetAddress = "10.102.4.109";
//    private String inetAddress = "192.168.100.79";
//    private String inetAddress = "192.168.100.136";
    private String port = "3000";
    private String baseUrl = protocol + "://" + inetAddress + ":" + port + "/";

    private ContextData() { }

    JSONObject userProfile;
    String loginUrl = baseUrl + "login";
    String galleryUrl = baseUrl + "gallery";
    String validationUrl = baseUrl + "validate-location";
    String locationsUrl = baseUrl + "locations";
    String descriptionUrl = baseUrl + "description";
    String drawablesLocationString = "android.resource://com.example.svoboda/drawable/";
    LatLng currentlLocation;
    LatLng leftmostPolygonPoint;
    LatLng rightmostPolygonPoint;

    static ContextData getInstance()
    {
        if (instance == null)
        {
            instance = new ContextData();
        }
        return instance;
    }
}
