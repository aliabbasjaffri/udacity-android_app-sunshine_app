package com.sunshine;

import java.net.URL;

import android.database.Cursor;
import android.net.Uri;
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

import com.sunshine.data.WeatherContract;

/**
 * Created by aliabbasjaffri on 24/10/15.
 */
public class ForecastFragment extends Fragment
{
    ListView listView = null;
    String cityLocation = null;
    SharedPreferences sharedPref = null;
    ForecastAdapter mForecastAdapter = null;


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
        String locationSetting = Utility.getPreferredLocation(getActivity());

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) view.findViewById(R.id.listView_forecast);

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.
                buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis()
                );

        Cursor cur = getActivity().getContentResolver().query
                (weatherForLocationUri, null, null, null, sortOrder);

        mForecastAdapter = new ForecastAdapter(getActivity(), cur, 0);

        Log.i("ForecastFragment" , "" + cur.getCount());
        mForecastAdapter.notifyDataSetChanged();
        mForecastAdapter.notifyDataSetChanged();
        listView.setAdapter(mForecastAdapter);

        //listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        //    @Override
        //    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //        Intent i = new Intent(getActivity(), DetailActivity.class);
        //        i.putExtra("Data", mForecastAdapter.getItem(position).toString());
        //        startActivity(i);
        //    }
        //});

        return view;
    }

    void updateWeather()
    {
        cityLocation = Utility.getPreferredLocation(getActivity());
        ForecastFetchTask fetchWeather = new ForecastFetchTask(getActivity());
        fetchWeather.execute(cityLocation);
        mForecastAdapter.notifyDataSetChanged();
    }
}