package com.wg.twtdatatest.Data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class EchartsData implements Serializable {
    private int dataPoint;
    private String time;
    private boolean isRecord;
    public EchartsData(){
    }

    public int getDataPoint() {
        return dataPoint;
    }

    public void setDataPoint(int dataPoint) {
        this.dataPoint = dataPoint;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public void setRecord(boolean record) {
        isRecord = record;
    }

    @Override
    public String toString() {
        return "EchartsData{" +
                "dataPoint=" + dataPoint +
                ", time='" + time + '\'' +
                ", isRecord=" + isRecord +
                '}';
    }
}
