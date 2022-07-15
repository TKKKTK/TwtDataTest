package com.wg.twtdatatest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.wg.twtdatatest.Data.BleDevice;

import no.nordicsemi.android.ble.ConnectRequest;

public class TwtBaseActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public BleDevice device;
    public TwtManager twtManager;
    public ConnectRequest connectRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        twtManager = new TwtManager(getApplication());
        connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disConnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disConnect();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disConnect();
    }

    /**
     * 蓝牙建立连接
     */
    public void connect(){
        if (device != null){
            connectRequest = twtManager.connect(device.getDevice())
                    .retry(3,100)
                    .useAutoConnect(false)
                    .then(d -> connectRequest = null);
            connectRequest.enqueue();
        }
    }

    /**
     * 蓝牙断开连接
     */
    public void disConnect(){
        device = null;
        if (connectRequest != null){
            connectRequest.cancelPendingConnection();
        }else if (twtManager.isConnected()){
            twtManager.disconnect().enqueue();
        }
    }

}