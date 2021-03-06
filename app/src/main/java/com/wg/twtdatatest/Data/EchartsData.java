package com.wg.twtdatatest.Data;

import android.os.Parcel;
import android.os.Parcelable;

public class EchartsData implements Parcelable {
    private int dataPoint;
    private String time;

    public EchartsData(){

    }

    //反序列化
    protected EchartsData(Parcel in) {
        dataPoint = in.readInt();
        time = in.readString();
    }

    public static final Creator<EchartsData> CREATOR = new Creator<EchartsData>() {
        @Override
        public EchartsData createFromParcel(Parcel in) {
            return new EchartsData(in);
        }

        @Override
        public EchartsData[] newArray(int size) {
            return new EchartsData[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    //序列化
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(dataPoint);
        parcel.writeString(time);
    }
}
