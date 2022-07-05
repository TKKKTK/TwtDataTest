package com.wg.twtdatatest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ListAdpter extends ArrayAdapter<BleDevice> {
    private int resourceId;
    public ListAdpter(@NonNull Context context, int resource, @NonNull List<BleDevice> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        BleDevice bleDevice = getItem(position);
        View view;
        if (convertView == null){
            view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
        }else{
            view = convertView;
        }

        TextView device_name = (TextView) view.findViewById(R.id.device_name);
        TextView device_address = (TextView) view.findViewById(R.id.device_address);
        TextView device_rssi = (TextView) view.findViewById(R.id.device_rssi);

        device_name.setText(bleDevice.getName());
        device_address.setText(bleDevice.getAddress());

        return view;
    }
}
