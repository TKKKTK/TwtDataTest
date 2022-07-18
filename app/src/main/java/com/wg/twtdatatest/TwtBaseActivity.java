package com.wg.twtdatatest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.wg.twtdatatest.Data.BleDevice;
import com.wg.twtdatatest.TwtManager;

import no.nordicsemi.android.ble.ConnectRequest;

public class TwtBaseActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public BleDevice device;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}