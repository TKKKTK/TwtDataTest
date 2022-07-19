package com.wg.twtdatatest.Data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class UiEchartsData implements Parcelable {
    private List<EchartsData> listPacket;

    public UiEchartsData(){}

    protected UiEchartsData(Parcel in) {
        listPacket = in.createTypedArrayList(EchartsData.CREATOR);
    }

    public static final Creator<UiEchartsData> CREATOR = new Creator<UiEchartsData>() {
        @Override
        public UiEchartsData createFromParcel(Parcel in) {
            return new UiEchartsData(in);
        }

        @Override
        public UiEchartsData[] newArray(int size) {
            return new UiEchartsData[size];
        }
    };

    public List<EchartsData> getListPacket() {
        return listPacket;
    }

    public void setListPacket(List<EchartsData> listPacket) {
        this.listPacket = listPacket;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(listPacket);
    }
}
