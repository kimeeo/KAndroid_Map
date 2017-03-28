package com.kimeeo.kAndroid.mapDemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.kimeeo.kAndroid.core.fragment.BaseFragment;
import com.kimeeo.kAndroid.mapDemo.R;

import java.util.HashMap;
import java.util.Map;

public class Main extends AppCompatActivity {

    Map<Integer, Class> views = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment view =BaseFragment.newInstance(MapView.class);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, view).commit();
    }

}
