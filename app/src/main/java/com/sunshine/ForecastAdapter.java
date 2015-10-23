package com.sunshine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Created by aliabbasjaffri on 24/10/15.
 */
public class ForecastAdapter extends ArrayAdapter
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
