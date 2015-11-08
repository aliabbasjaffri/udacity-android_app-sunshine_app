package com.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

import com.sunshine.data.WeatherContract;

/**
 * Created by aliabbasjaffri on 24/10/15.
 */

/*public class ForecastAdapter extends ArrayAdapter
{
    Context context;
    int XMLID = 0;
    ArrayList<String> weatherData = new ArrayList<>();

    public ForecastAdapter(Context context, int resource , ArrayList<String> data)
    {
        super(context, resource , data);

        this.context = context;
        XMLID = resource;
        weatherData = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(context).inflate( XMLID, parent, false );
        }

        TextView forecast = (TextView) convertView.findViewById(R.id.list_item_forecast_textview);
        forecast.setText(weatherData.get(position));

        return convertView;
    }
}
*/

public class ForecastAdapter extends CursorAdapter
{
    public ForecastAdapter(Context context, Cursor c, int flags)
    {
        super(context, c, flags);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low)
    {
        boolean isMetric = Utility.isMetric(mContext);
        return Utility.formatTemperature(high, isMetric) + "/" + Utility.formatTemperature(low, isMetric);
        //return highLowStr;
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
     */
    private String convertCursorRowToUXFormat(Cursor cursor)
    {
        String highAndLow = formatHighLows(
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(ForecastFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.

        TextView tv = (TextView)view.findViewById(R.id.list_item_forecast_textview);
        tv.setText(convertCursorRowToUXFormat(cursor));
    }
}