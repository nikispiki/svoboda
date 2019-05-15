package com.example.svoboda;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/*
    This class handler I/O operation to and from the internal storage
 */
class IOHandler {
    private static final String TAG = "IOHandler";
    private Context context;

    IOHandler(Context c)
    {
        context = c;
    }

    File getFile(String dir, String file)
    {
        File directory = context.getDir(dir, Context.MODE_PRIVATE);
        return new File(directory, file);
    }

    /*
        Writes the provided JSONObject to internal storage
     */
    void writeJSONToInternalData(JSONObject json)
    {
        try
        {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("svoboda.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(json.toString());
            outputStreamWriter.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    /*
        Saves the image in the specified directory in the internal storage
     */
    void writeFileToInternalData(String dir, String name, Bitmap iamgeBitmap)
    {
        File image = getFile(dir, name);
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(image);
            // Use the compress method on the BitMap object to write image to the OutputStream
            iamgeBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    /*
        Reads the internal storage data and return it as a JSONObject or null if
        an error occurs
     */
    JSONObject readInternalDataAsJSON(String readFrom)
    {

        JSONObject json = null;

        try
        {
            InputStream inputStream = context.openFileInput(readFrom);

            if ( inputStream != null )
            {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                String ret = stringBuilder.toString();
                json = new JSONObject(ret);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "File not found: " + e.toString());
        }
        catch (IOException e)
        {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return json;
    }
}
