package com.sunshine;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by aliabbasjaffri on 24/10/15.
 */
public class ForecastFragment extends Fragment
{
    ArrayList<String> weatherData = new ArrayList<>();
    String forecastJsonStr = null;

    public ForecastFragment( )
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.forecastfragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_refresh)
        {
            new ForecastFetchTask().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        //new ForecastFetchTask().execute();

        weatherData.add("Today - Sunny - 88/63");
        weatherData.add("Today - Rainy - 50/63");
        weatherData.add("Today - Frosty - 09/63");


        ListView listView = (ListView) view.findViewById(R.id.listView_forecast);
        ForecastAdapter forecastAdapter = new ForecastAdapter( getActivity() ,R.layout.list_item_forecast , weatherData);
        listView.setAdapter(forecastAdapter);

        return view;
    }

    class ForecastFetchTask extends AsyncTask<Void, Void, Void>
    {
        private final String LOG_TAG = ForecastFetchTask.class.getSimpleName();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=Lahore,PK&mode=json&units=metric&cnt=7";
                String apiKey = "&appid=bd82977b86bf27fb59a04b61b657fb6f";
                String finalUrl = baseUrl + apiKey;
                URL url = new URL(finalUrl);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();


                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null)
                    return null;

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null)
                {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0)
                    return null;

                forecastJsonStr = buffer.toString();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                Log.e("data", forecastJsonStr);

                if (urlConnection != null)
                {
                    urlConnection.disconnect();
                }
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (final IOException e)
                    {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return null;
        }
    }
}