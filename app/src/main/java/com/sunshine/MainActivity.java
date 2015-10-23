package com.sunshine;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    static ArrayList<String> weatherData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weatherData.add("Today - Sunny - 88/63");
        weatherData.add("Today - Rainy - 50/63");
        weatherData.add("Today - Frosty - -09/63");

        if( savedInstanceState == null )
        {
            getSupportFragmentManager().beginTransaction().add(R.id.container , new PlaceholderFragment()).commit();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment
    {
        ArrayList<String> data = new ArrayList<>();

        public PlaceholderFragment( )
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.fragment_main, container, false);

            ListView listView = (ListView) view.findViewById(R.id.listView_forecast);
            ForecastAdapter forecastAdapter = new ForecastAdapter( getActivity() ,R.layout.list_item_forecast , weatherData);
            listView.setAdapter(forecastAdapter);

            return view;
        }
    }

}
