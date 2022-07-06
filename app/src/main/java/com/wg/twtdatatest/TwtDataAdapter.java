package com.wg.twtdatatest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import no.nordicsemi.android.ble.data.Data;

public class TwtDataAdapter extends RecyclerView.Adapter<TwtDataAdapter.ViewHolder> {

    private List<Data> dataList;
    private OnItemClikListener onItemClikListener;

    public interface OnItemClikListener{
        void onItemClik(final Data data);
    }

    public void setOnItemClikListener(final OnItemClikListener listener){
        this.onItemClikListener = listener;
    }

    public TwtDataAdapter(List<Data> dataList) {
        this.dataList = dataList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.data_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Data data = dataList.get(position);
            holder.dataText.setText(data.toString());
            holder.dataContaner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onItemClikListener != null){
                        int position = holder.getLayoutPosition();
                        final Data data1 = dataList.get(position);
                        onItemClikListener.onItemClik(data1);
                    }
                }
            });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView dataText;
        LinearLayout dataContaner;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dataText = (TextView) itemView.findViewById(R.id.data_text);
            dataContaner = (LinearLayout) itemView.findViewById(R.id.data_Contaner);
        }
    }
}
