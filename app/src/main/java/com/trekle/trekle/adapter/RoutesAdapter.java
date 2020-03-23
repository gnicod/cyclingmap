package com.trekle.trekle.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sweetzpot.stravazpot.route.model.Route;
import com.trekle.trekle.TrekleApplication;

import androidx.annotation.NonNull;

public class RoutesAdapter extends ArrayAdapter<Route> {


    public RoutesAdapter(@NonNull Context context, int resource, @NonNull Route[] objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(TrekleApplication.Companion.getAppContext());
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
        }
        Route route = getItem(position);

        ((TextView) convertView.findViewById(android.R.id.text1))
                .setText(route.getName());
        return convertView;
    }
}
