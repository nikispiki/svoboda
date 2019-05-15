package com.example.svoboda;

import org.json.JSONObject;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/*
    Singleton class that uses the OkHttpClient to make request to the "svoboda" server.
 */
public class SvobodaAPIClient {
    private static SvobodaAPIClient instance;
    private OkHttpClient httpClient;

    private SvobodaAPIClient()
    {
        httpClient = new OkHttpClient();
    }

    static SvobodaAPIClient getInstance()
    {
        if (instance == null)
        {
            instance = new SvobodaAPIClient();
        }
        return instance;
    }

    /*
        Makes a request to the specified url. If jsonData is provided its a POST request.
        The provided callback is called on failure or response from the request.
     */
    public void makeRequest(String url, JSONObject jsonData, Callback callback)
    {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (jsonData != null)
        {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody req = RequestBody.create(JSON, jsonData.toString());
            requestBuilder.post(req);
        }
        Request request = requestBuilder.build();
        httpClient.newCall(request).enqueue(callback);
    }
}
