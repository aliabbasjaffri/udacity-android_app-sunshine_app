package com.sunshine;

import java.net.URL;
import android.util.Log;
import android.os.Bundle;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import android.os.AsyncTask;
import android.content.Intent;
import org.json.JSONException;
import android.view.ViewGroup;
import java.io.BufferedReader;
import android.widget.ListView;
import android.text.format.Time;
import java.io.InputStreamReader;
import android.widget.AdapterView;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.support.v4.app.Fragment;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by aliabbasjaffri on 24/10/15.
 */
public class ForecastFragment extends Fragment
{
    ListView listView = null;
    String cityLocation = null;
    SharedPreferences sharedPref = null;
    ForecastAdapter forecastAdapter = null;


    public ForecastFragment( )
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onStart()
    {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) view.findViewById(R.id.listView_forecast);
        forecastAdapter = new ForecastAdapter( getActivity() ,R.layout.list_item_forecast , new ArrayList<String>());
        listView.setAdapter(forecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getActivity(), DetailActivity.class);
                i.putExtra("Data", forecastAdapter.getItem(position).toString());
                startActivity(i);
            }
        });

        return view;
    }

    void updateWeather()
    {
        cityLocation = sharedPref.getString( getString(R.string.settings_location_key) , getString(R.string.settings_location_default));

        ForecastFetchTask fetchWeather = new ForecastFetchTask(getActivity() , forecastAdapter);
        fetchWeather.execute(cityLocation);
    }
}