package com.wg.twtdatatest;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.DataSentCallback;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class TwowaytoDataCallback implements ProfileDataCallback, DataSentCallback {
    @Override
    public void onDataSent(@NonNull BluetoothDevice device, @NonNull Data data) {

    }

    @Override
    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {

    }
}
