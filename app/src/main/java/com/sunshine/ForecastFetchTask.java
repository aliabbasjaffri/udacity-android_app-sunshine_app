package com.sunshine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import com.sunshine.data.*;
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
import java.util.Date;
import java.util.Vector;

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
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
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

    long addLocation(String locationSetting, String cityName, double lat, double lon)
    {
        long locationId = 0;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = mContext.getContentResolver().query
                (
                    WeatherContract.LocationEntry.CONTENT_URI,
                    new String[]{WeatherContract.LocationEntry._ID},
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                    new String[]{locationSetting},
                    null
                );

        if (locationCursor.moveToFirst())
        {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        }
        else
        {
            ContentValues locationValues = new ContentValues();

            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );
            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        // Wait, that worked?  Yes!
        return locationId;
    }

    ArrayList<String> convertContentValuesToUXFormat(Vector<ContentValues> cvv)
    {
        // return strings to keep UI functional for now
        ArrayList<String> resultStrs = new ArrayList<String>();
        for ( int i = 0; i < cvv.size(); i++ )
        {
            ContentValues weatherValues = cvv.elementAt(i);

            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));

            resultStrs.add( getReadableDateString(
                    weatherValues.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow);
        }
        return resultStrs;
    }

    private ArrayList<String> getWeatherDataFromJson(String forecastJsonStr, String locationSetting) throws JSONException
    {
        ArrayList<String> data = new ArrayList<>();

        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            for (int i = 0; i < weatherArray.length(); i++)
            {
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            // add to database
            if ( cVVector.size() > 0 )
            {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);

            }

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");

            data = convertContentValuesToUXFormat(cVVector);
            return data;
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return data;

    }

    @Override
    protected ArrayList<String> doInBackground(String... params)
    {
        String cityName = "";

        try
        {
            //http://api.openweathermap.org/data/2.5/forecast/daily?q=Lahore,PK&mode=json&units=metric&cnt=7&appid=bd82977b86bf27fb59a04b61b657fb6f
            String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
            cityName = params[0];
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
            weatherData = getWeatherDataFromJson(forecastJsonStr , cityName);
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
