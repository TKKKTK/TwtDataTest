package com.wg.twtdatatest.Data;

import android.os.Parcel;
import android.os.Parcelable;

import no.nordicsemi.android.ble.data.Data;

/**
 * 数据包实体类
 */
public class DataPacket implements Parcelable {
    private Data data;
    private boolean isSelect;
    private long timeRecord;

    public DataPacket(Data data, long timeRecord) {
        this.data = data;
        this.timeRecord = timeRecord;
        this.isSelect = false;
    }

    protected DataPacket(Parcel in) {
        data = in.readParcelable(Data.class.getClassLoader());
        isSelect = in.readByte() != 0;
        timeRecord = in.readLong();
    }

    public static final Creator<DataPacket> CREATOR = new Creator<DataPacket>() {
        @Override
        public DataPacket createFromParcel(Parcel in) {
            return new DataPacket(in);
        }

        @Override
        public DataPacket[] newArray(int size) {
            return new DataPacket[size];
        }
    };

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public long getTimeRecord() {
        return timeRecord;
    }

    public void setTimeRecord(long timeRecord) {
        this.timeRecord = timeRecord;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeParcelable(data, i);
        parcel.writeByte((byte) (isSelect ? 1 : 0));
        parcel.writeLong(timeRecord);
    }
}
