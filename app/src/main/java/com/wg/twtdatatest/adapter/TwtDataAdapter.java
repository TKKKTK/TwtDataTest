package com.wg.twtdatatest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wg.twtdatatest.Data.DataPacket;
import com.wg.twtdatatest.R;

import java.util.List;

public class TwtDataAdapter extends RecyclerView.Adapter<TwtDataAdapter.ViewHolder> {

    private List<DataPacket> dataList;

    public TwtDataAdapter(List<DataPacket> dataList) {
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
            DataPacket dataPacket = dataList.get(position);
            holder.dataText.setText(dataPacket.getData().toString());
            //holder.timeRecord.setText(dataPacket.getTimeRecord());

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView dataText;
        TextView timeRecord;
        LinearLayout dataContaner;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dataText = (TextView) itemView.findViewById(R.id.data_text);
            timeRecord =(TextView) itemView.findViewById(R.id.time_record);
            dataContaner = (LinearLayout) itemView.findViewById(R.id.data_Contaner);
        }
    }
}
