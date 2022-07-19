package com.wg.twtdatatest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.wg.twtdatatest.Data.BleDevice;
import com.wg.twtdatatest.Service.BackgroundService;
import com.wg.twtdatatest.TwtManager;

import no.nordicsemi.android.ble.ConnectRequest;

public class TwtBaseActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public BleDevice device;
    public BackgroundService.TwtBinder twtBinder;


    //通过服务连接类获取twtBinder对象
    public ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            twtBinder = (BackgroundService.TwtBinder)iBinder; //获取twtBinder对象
            twtBinder.connect();//蓝牙连接
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {//服务断开连接时调用

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);

        //绑定后台服务,连接蓝牙
        Intent startIntent  = new Intent(this, BackgroundService.class);
        startIntent.putExtra(EXTRA_DEVICE,device);
        bindService(startIntent,connection,BIND_AUTO_CREATE);
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
        twtBinder.disconnect();
        unbindService(connection);
    }

}