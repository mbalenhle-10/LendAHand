package com.example.lendahand;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class TopDonorsAdapter extends ArrayAdapter<String> {

    public TopDonorsAdapter(Context context,
                            ArrayList<String> donors) {

        super(context,
                android.R.layout.simple_list_item_1,
                donors);
    }

    @Override
    public View getView(int position,
                        View convertView,
                        ViewGroup parent) {

        TextView view = (TextView) super.getView(
                position,
                convertView,
                parent
        );

        view.setTextSize(18);

        return view;
    }
}