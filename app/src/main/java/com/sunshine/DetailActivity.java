package com.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Bundle arguments = new Bundle();
        arguments.putParcelable(DetailFragment.DETAIL_URI, getIntent().getData());
        DetailFragment detailFragment = new DetailFragment();
        detailFragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction().replace(R.id.weather_detail_container, detailFragment ).commit();
    }

}
