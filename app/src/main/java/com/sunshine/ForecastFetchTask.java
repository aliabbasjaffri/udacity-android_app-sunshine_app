package com.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ListView;

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
 * Created by aliabbasjaffri on 01/11/15.
 */
class ForecastFetchTask extends AsyncTask<String, Void, ArrayList<String>>
{
    private final String LOG_TAG = ForecastFetchTask.class.getSimpleName();

    private Context mContext = null;
    private Double latitude = null;
    private Double longitude = null;
    private BufferedReader reader = null;
    private String forecastJsonStr = null;
    private String temperatureMode = null;
    private HttpURLConnection urlConnection = null;
    private ForecastAdapter mForecastAdapter = null;
    private ArrayList<String> weatherData = new ArrayList<>();

    public ForecastFetchTask(Context context, ForecastAdapter forecastAdapter)
    {
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    private String getReadableDateString(long time)
    {
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    private String formatHighLows(double high, double low)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        temperatureMode = sharedPref.getString( mContext.getString(R.string.settings_mode_key) , mContext.getString(R.string.settings_mode_default) );

        if (temperatureMode.equals(mContext.getString(R.string.temperature_unit_imperial)))
        {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        }
        else if (!temperatureMode.equals(mContext.getString(R.string.temperature_unit_metric)))
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
            String apiKey = "&appid=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;

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
        mForecastAdapter.clear();
        for(String dayForecastStr : s) {
            mForecastAdapter.add(dayForecastStr);
        }
        mForecastAdapter.notifyDataSetChanged();
    }
}
