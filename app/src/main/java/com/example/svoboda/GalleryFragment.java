package com.example.svoboda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class GalleryFragment extends Fragment implements Callback {
    private static final String TAG = "GalleryFragment";
    private ContextData contextData;
    private GridView gridView;
    private ImageView galleryImage;
    private String sessId;
    private SvobodaAPIClient svobodaAPIClient;
    private IOHandler ioHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_gallery, container,false);
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
        galleryImage = view.findViewById(R.id.galleryImageView);
        gridView = view.findViewById(R.id.gridView);

        getGalleryImageNames();
    }

    /*
        This function sets the custom GalleryImageAdapter as the adapter for the grid view,
        sets the most recent image as the title image in the gallery and sets click events
        for the prictures in the grid view so that on click on a picture from the grid view
        it displays as the title picture of the gallery
     */
    private void setupGalleryImageAdapter(JSONArray pictureNames)
    {
        gridView.setAdapter(new GalleryImageAdapter(getActivity(), pictureNames));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                Glide.with(getActivity())
                        .load(gridView.getItemAtPosition(position))
                        .into(galleryImage);
            }
        });
        try
        {
            /*
                Set the first picture of those returned by the server
                as the title picture in the users gallery
             */
            File image = ioHandler.getFile("gallery", pictureNames.getString(0));
            if (image.exists())
            {
                Glide.with(this)
                        .load(Uri.fromFile(image))
                        .into(galleryImage);
            }
            else
            {
                /*
                    If the users gallery is empty display a default picture
                    to demonstrate how gallery looks
                 */
                Glide.with(this)
                        .load(Uri.parse(contextData.drawablesLocationString + getActivity().getResources().getResourceEntryName(R.drawable.pic1)))
                        .into(galleryImage);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void getGalleryImageNames() {
        String uri = Uri.parse(contextData.galleryUrl)
                .buildUpon()
                .appendQueryParameter("sessId", sessId)
                .build().toString();

        /*
            Make request to the server for the picture names in the gallery using
            the SvobodaAPIClient. When a response is received or the request fails
            the corresponding functions onResponse() and onFailure() are called.
         */
        svobodaAPIClient.makeRequest(uri, null, this);
    }

    @Override
    public void onFailure(@NonNull  Call call, @NonNull IOException e)
    {
        e.printStackTrace();
    }

    @Override
    public void onResponse(@NonNull  Call call,@NonNull Response response)
    {
        if (!response.isSuccessful())
        {
            new Handler(Looper.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(getActivity(), "Server error", Toast.LENGTH_SHORT).show();
                }
            });
        }
        /*
            When we receive the images from the server we pass them to
            the setupGalleryImageAdapter() function
         */
        else
        {
            if (response.body() != null)
            {
                try
                {
                    String responseBody = response.body().string();
                    JSONObject jsonData = new JSONObject(responseBody);
                    JSONArray pictureNames = jsonData.getJSONArray("images");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            setupGalleryImageAdapter(pictureNames);
                        }
                    });
                    response.body().close();

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
    }
}
