package com.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent i = getIntent();
        String weatherReport = i.getStringExtra("Data");

        Bundle bundle = new Bundle();
        bundle.putString("Data", weatherReport);

        Fragment fragment = new DetailActivityFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
    }

}
