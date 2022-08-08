package com.wg.twtdatatest.Data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class UiEchartsData {
    private List<EchartsData> listPacket;

    public UiEchartsData(){}

    public List<EchartsData> getListPacket() {
        return listPacket;
    }

    public void setListPacket(List<EchartsData> listPacket) {
        this.listPacket = listPacket;
    }
}
