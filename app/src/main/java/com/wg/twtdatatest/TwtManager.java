package com.wg.twtdatatest;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;

public class TwtManager extends ObservableBleManager {

    private IreseviceDataListenner ireseviceDataListenner;

    //bt_patch(mtu).bin
    public static final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");  //蓝牙通讯服务
    public static final UUID READ_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");  //读特征
    public static final UUID WRITE_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");  //写特征 //服务

    private BluetoothGattCharacteristic readCharacteristic,writeCharacteristic;
    private boolean supported;

    public TwtManager(@NonNull Context context,IreseviceDataListenner Listenner) {
        super(context);
        this.ireseviceDataListenner = Listenner;
    }

    public TwtManager(@NonNull Context context, @NonNull Handler handler) {
        super(context, handler);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new DeviceBleManagerGattCallback();
    }

    private final TwowaytoDataCallback twowaytoDataCallback = new TwowaytoDataCallback() {
        @Override
        public void onDataSent(@NonNull BluetoothDevice device, @NonNull Data data) {
            super.onDataSent(device, data);
        }

        @Override
        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
            super.onDataReceived(device, data);
            Log.d(TAG, "onDataReceived: "+data);
            ireseviceDataListenner.DataResevice(data);
        }

        @Override
        public void onInvalidDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
            super.onInvalidDataReceived(device, data);
        }
    };



    private class DeviceBleManagerGattCallback extends BleManagerGattCallback{

        @Override
        protected void initialize() {
            super.initialize();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            final BluetoothGattService twtService = gatt.getService(SERVICE_UUID);
            if (twtService != null){
                readCharacteristic = twtService.getCharacteristic(READ_UUID);
                writeCharacteristic = twtService.getCharacteristic(WRITE_UUID);
            }
            boolean twtWriteRequest = false;
            if (writeCharacteristic != null){
                final int writeProperties = writeCharacteristic.getProperties();
                twtWriteRequest = (writeProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
            }

            supported = readCharacteristic !=null && writeCharacteristic != null & twtWriteRequest;

            return supported;
        }

        @Override
        protected void onServicesInvalidated() {
               readCharacteristic = null;
               writeCharacteristic = null;
        }
    }

    public void startRead(){
        setNotificationCallback(readCharacteristic).with(twowaytoDataCallback);
        readCharacteristic(readCharacteristic).with(twowaytoDataCallback).enqueue();
        enableNotifications(readCharacteristic).enqueue();
    }

    public void stopRead(){
        readCharacteristic = null;
        writeCharacteristic = null;
    }
}
