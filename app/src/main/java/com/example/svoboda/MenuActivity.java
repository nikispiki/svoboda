package com.example.svoboda;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONException;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MenuActivity";
    private DrawerLayout drawerLayout;
    private Fragment currentActiveFragment;
    private FragmentManager fragmentManager;
    private ImageView profilePicView;
    private TextView profileNameView;
    private ContextData contextData;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        contextData = ContextData.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View menuHeaderView = navigationView.getHeaderView(0);
        profilePicView = menuHeaderView.findViewById(R.id.profilePic);
        profileNameView = menuHeaderView.findViewById(R.id.profileName);

        try
        {
            String profilePic = contextData.userProfile.getString("profile_pic");
            String username = contextData.userProfile.getString("username");
            byte[] imageByteArray = Base64.decode(profilePic, Base64.DEFAULT);
            Glide.with(this)
                    .load(imageByteArray)
                    .placeholder(R.drawable.ic_default_profile_pic)
                    .into(profilePicView);
            profileNameView.append(username);
        }
        catch (JSONException e)
        {
            Glide.with(this)
                    .load(R.drawable.ic_default_profile_pic)
                    .into(profilePicView);
            profileNameView.append("Unknown");
            e.printStackTrace();
        }

        fragmentManager = this.getSupportFragmentManager();
        displayFragmnet(new MapFragment());
    }

    private void displayFragmnet(Fragment fragment)
    {
        if (currentActiveFragment == null ||
                !currentActiveFragment.getClass().toString().equals(fragment.getClass().toString()))
        {
            fragmentManager.beginTransaction().replace(R.id.mainLayout, fragment).commit();
            currentActiveFragment = fragment;
        }
    }

    // Handle navigation view item clicks here.
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();

        /*
            Calls on the fragment manager to display a fragment base on
            the selected menu item
         */
        if (id == R.id.nav_camera)
        {
            displayFragmnet(new CameraFragment());
        }
        else if (id == R.id.nav_gallery)
        {
            displayFragmnet(new GalleryFragment());
        }
        else if (id == R.id.nav_map)
        {
            displayFragmnet(new MapFragment());
        }
        else if (id == R.id.nav_description)
        {
            displayFragmnet(new InstantTrackerFragment());
        }

        // Close the menu
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
