package com.sunshine;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by aliabbasjaffri on 24/10/15.
 */
public class ForecastFragment extends Fragment
{
    ListView listView = null;
    String forecastJsonStr = null;
    ForecastAdapter forecastAdapter = null;
    ArrayList<String> weatherData = new ArrayList<>();

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
            new ForecastFetchTask().execute("Lahore");
            forecastAdapter.notifyDataSetChanged();
            return true;
        }
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        new ForecastFetchTask().execute("Lahore");

        listView = (ListView) view.findViewById(R.id.listView_forecast);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //Intent i = new Intent( getActivity() , DetailsActivity.class );
                Toast.makeText(getActivity(), forecastAdapter.getItem(position).toString(), Toast.LENGTH_SHORT).show();
                //i.putExtra("Data" , forecastAdapter.getItem(position).toString() );
                //startActivity(i);
            }
        });

        return view;
    }

    class ForecastFetchTask extends AsyncTask<String, Void, ArrayList<String>>
    {
        private final String LOG_TAG = ForecastFetchTask.class.getSimpleName();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        private String getReadableDateString(long time)
        {
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high, double low)
        {
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            return roundedHigh + "/" + roundedLow;
        }

        private ArrayList<String> getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException
        {
            ArrayList<String> data = new ArrayList<>();
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            String[] resultStrs = new String[numDays];

            for(int i = 0; i < weatherArray.length(); i++)
            {
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
                data.add(resultStrs[i]);
            }

            return data;

        }

        @Override
        protected ArrayList<String> doInBackground(String... params)
        {
            try
            {
                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
                String cityName = params[0];
                String postalCode = "";
                String countryName = ",PK";
                String mode = "&mode=json";
                String units = "&units=metric";
                String numberOfDays = "&cnt=7";
                String apiKey = "&appid=bd82977b86bf27fb59a04b61b657fb6f";  //+ BuildConfig.OPEN_WEATHER_MAP_API_KEY;

                String finalUrl = baseUrl + cityName + countryName + mode + units + numberOfDays + apiKey;
                URL url = new URL(finalUrl);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();

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

            try
            {
                weatherData = getWeatherDataFromJson(forecastJsonStr , 7);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            return weatherData;
        }

        @Override
        protected void onPostExecute(ArrayList<String> s)
        {
            super.onPostExecute(s);
            forecastAdapter = new ForecastAdapter( getActivity() ,R.layout.list_item_forecast , weatherData);
            listView.setAdapter(forecastAdapter);
        }
    }
}