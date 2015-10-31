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
    Double latitude = null;
    Double longitude = null;
    ListView listView = null;
    String cityLocation = null;
    String temperatureMode = null;
    String forecastJsonStr = null;
    SharedPreferences sharedPref = null;
    ForecastAdapter forecastAdapter = null;
    ArrayList<String> weatherData = new ArrayList<>();


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
        temperatureMode = sharedPref.getString( getString( R.string.settings_mode_key ) , getString( R.string.settings_mode_default) );
        new ForecastFetchTask().execute(cityLocation);
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
            if (temperatureMode.equals(getString(R.string.temperature_unit_imperial)))
            {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            }
            else if (!temperatureMode.equals(getString(R.string.temperature_unit_metric)))
            {
                Log.d(LOG_TAG, "Unit type not found: " + temperatureMode);
            }

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
            final String OWM_LATITUDE = "lat";
            final String OWM_LONGITUDE = "lon";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONObject city = forecastJson.getJSONObject("city");
            JSONObject locationCordinates = city.getJSONObject("coord");
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            latitude = locationCordinates.getDouble(OWM_LATITUDE);
            longitude = locationCordinates.getDouble(OWM_LONGITUDE);

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
                //http://api.openweathermap.org/data/2.5/forecast/daily?q=Lahore,PK&mode=json&units=metric&cnt=7&appid=bd82977b86bf27fb59a04b61b657fb6f
                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
                String cityName = params[0];
                String postalCode = "";
                String countryName = ",PK";
                String mode = "&mode=json";
                String units = "&units=metric";
                String numberOfDays = "&cnt=7";
                String apiKey = "&appid=bd82977b86bf27fb59a04b61b657fb6f";  //+ BuildConfig.OPEN_WEATHER_MAP_API_KEY;

                //Toast.makeText(getActivity() , "City = " + params[0] + " and mode = " + params[1] ,Toast.LENGTH_SHORT).show();

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
                        //Toast.makeText(getActivity() , "City = " + params[0] + " and mode = " + params[1] ,Toast.LENGTH_SHORT).show();
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