package com.example.svoboda;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;


public class GalleryImageAdapter extends BaseAdapter {
    private Context context;
    private ContextData contextData;
    private ArrayList<Uri> imagesGlide;
    private IOHandler ioHandler;
    private Integer[] defaultImages = {
            R.drawable.pic1, R.drawable.pic2,
            R.drawable.pic3, R.drawable.pic4,
            R.drawable.pic5, R.drawable.pic6,
            R.drawable.pic7, R.drawable.pic8,
            R.drawable.pic9, R.drawable.pic10,
            R.drawable.pic11
    };


    /*
        We get the files for the pictures using the picture names passed from GalleryFragment
        and the create URIs for the files that we store in the imagesGlide list. We also
        add some default images from the drawables so that we can show how a full gallery of
        images looks like.
     */
    GalleryImageAdapter(Context c, JSONArray pictureNames)
    {
        context = c;
        ioHandler = new IOHandler(c);
        imagesGlide = new ArrayList<Uri>();
        contextData = ContextData.getInstance();
        try
        {
            for(int i=0; i<pictureNames.length(); i++)
            {
                String name = pictureNames.getString(i);
                File image = ioHandler.getFile("gallery", pictureNames.getString(i));
                if (image.exists())
                {
                    imagesGlide.add(Uri.fromFile(image));
                }
            }
            for (Integer defImg : defaultImages)
            {
                imagesGlide.add(Uri.parse(contextData.drawablesLocationString + context.getResources().getResourceEntryName(defImg)));
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount()
    {
        return imagesGlide.size();
    }

    @Override
    public Object getItem(int position)
    {
        return imagesGlide.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    /*
        Get a View that displays the data at the specified position in the data set.
        In this case the data set is the imagesGlide list. We create an ImageView populated
        with the picture we load with Glide using the URI of the picture from the list.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_imageview, null);
        }
        ImageView imageView = convertView.findViewById(R.id.gridImageView);
        Glide.with(context)
                .load(imagesGlide.get(position))
                .into(imageView);
        return imageView;
    }
}
