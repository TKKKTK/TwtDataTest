package com.wg.twtdatatest.Data;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class BleDevice implements Parcelable {
    private String name;
    private String address;
    private BluetoothDevice device;
    private int rssi;

    public BleDevice(ScanResult result){
        device = result.getDevice();
        name = device.getName();
        address = device.getAddress();
        rssi = result.getRssi();
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    //反序列化
    protected BleDevice(Parcel in) {
        name = in.readString();
        address = in.readString();
        device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        rssi = in.readInt();
    }

    public static final Creator<BleDevice> CREATOR = new Creator<BleDevice>() {
        @Override
        public BleDevice createFromParcel(Parcel in) {
            return new BleDevice(in);
        }

        @Override
        public BleDevice[] newArray(int size) {
            return new BleDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //序列化
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(address);
        parcel.writeParcelable(device, i);
        parcel.writeInt(rssi);
    }
}
