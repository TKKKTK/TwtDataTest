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

import com.wg.twtdatatest.Callback.TwowaytoDataCallback;
import com.wg.twtdatatest.Service.BackgroundService;

import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;

public class TwtManager extends ObservableBleManager {

    private IreseviceDataListenner ireseviceDataListenner;

    /**
     * iFocus
     */
    //bt_patch(mtu).bin
    public static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-3c17-d293-8e48-14fe2e4da212");  //蓝牙通讯服务
    public static final UUID READ_UUID = UUID.fromString("0000ffe2-3c17-d293-8e48-14fe2e4da212");  //读特征
    public static final UUID WRITE_UUID = UUID.fromString("0000ffe3-3c17-d293-8e48-14fe2e4da212");  //写特征 //服务

    /**
     * 8通道
     */
//    public static final UUID SERVICE_UUID = UUID.fromString("0000CCF0-67EE-4600-BCCC-FF98C2A749AB");  //蓝牙通讯服务
//    public static final UUID READ_UUID = UUID.fromString("0000CCF1-67EE-4600-BCCC-FF98C2A749AB");  //读特征
//    public static final UUID WRITE_UUID = UUID.fromString("0000CCF2-67EE-4600-BCCC-FF98C2A749AB");  //写特征 //服务

    /**
     * 臂环
     */
//    public static final UUID SERVICE_UUID = UUID.fromString("8653000a-43e6-47b7-9cb0-5fc21d4ae340");  //蓝牙通讯服务
//    public static final UUID READ_UUID = UUID.fromString("8653000b-43e6-47b7-9cb0-5fc21d4ae340");  //读特征
//    public static final UUID WRITE_UUID = UUID.fromString("8653000c-43e6-47b7-9cb0-5fc21d4ae340");  //写特征 //服务

    private BluetoothGattCharacteristic readCharacteristic,writeCharacteristic;
    private boolean supported;

    public TwtManager(@NonNull Context context) {
        super(context);
    }

    public void setIreseviceDataListenner(IreseviceDataListenner listenner){
        this.ireseviceDataListenner = listenner;
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
            //Log.d(TAG, "onInvalidDataReceived: "+data);
        }
    };



    private class DeviceBleManagerGattCallback extends BleManagerGattCallback{
        @Override
        protected void initialize() {
            super.initialize();
            setNotificationCallback(readCharacteristic).with(twowaytoDataCallback);
            readCharacteristic(readCharacteristic).with(twowaytoDataCallback).enqueue();
            enableNotifications(readCharacteristic).enqueue();
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

    public void startNotifications(){
        enableNotifications(readCharacteristic).enqueue();
    }

    public void stopNotifications(){
        disableNotifications(readCharacteristic).enqueue();
    }

    public void  ReadData(int cmd){
        if (writeCharacteristic == null){
            return;
        }
        writeCharacteristic(writeCharacteristic
        ,new Data(new byte[]{(byte)cmd})
        ,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT).with(twowaytoDataCallback).enqueue();
    }


}
