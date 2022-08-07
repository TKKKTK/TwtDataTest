package com.wg.twtdatatest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.wg.twtdatatest.Data.BleDevice;
import com.wg.twtdatatest.R;

import java.util.List;

public class ScanAdpter extends RecyclerView.Adapter<ScanAdpter.ViewHolder> {

    private List<BleDevice> dataList;
    private OnItemClickListener onItemClickListener;

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(@NonNull final BleDevice device);
    }

    public void setOnItemClickListener(@Nullable final OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public ScanAdpter(List<BleDevice> dataList){
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item,parent,false);
        ScanAdpter.ViewHolder holder = new ScanAdpter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BleDevice bleDevice = dataList.get(position);
        holder.device_name.setText(bleDevice.getName());
        holder.device_address.setText(bleDevice.getAddress());
        holder.device_rssi.setText(bleDevice.getRssi()+"dbm");
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(bleDevice);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView device_name;
        TextView device_address;
        TextView device_rssi;
        View itemView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            device_name = (TextView) itemView.findViewById(R.id.device_name);
            device_address = (TextView) itemView.findViewById(R.id.device_address);
            device_rssi = (TextView) itemView.findViewById(R.id.device_rssi);
            this.itemView = itemView;

        }
    }
}
