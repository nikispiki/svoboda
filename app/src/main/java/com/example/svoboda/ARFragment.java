package com.example.svoboda;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.maxst.ar.MaxstAR;
import com.maxst.ar.TrackerManager;

import java.util.ArrayList;

public abstract class ARFragment extends Fragment {
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        MaxstAR.init(getActivity(),"bDc1FSIla8l3BiVEeto3cWp53VWROVpz+QlZSV0Auf8=");
        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        TrackerManager.getInstance().destroyTracker();
        MaxstAR.deinit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        MaxstAR.setScreenOrientation(newConfig.orientation);
    }
}
