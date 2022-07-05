package com.wg.twtdatatest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends AppCompatActivity {

    private List<BleDevice> deviceList = new ArrayList<>();
    private ListAdpter listAdpter;

    //请求打开蓝牙
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    //权限请求码
    private static final int REQUEST_PERMISSION_CODE = 9527;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        openBluetooth();
        initView();
    }


    private void initView() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listAdpter = new ListAdpter(ScanActivity.this, R.layout.list_item, deviceList);
        listView.setAdapter(listAdpter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
    }

    private void openBluetooth() {
        //获取蓝牙适配器
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {//是否支持蓝牙
            if (bluetoothAdapter.isEnabled()) {
                //蓝牙已打开
//                showMsg("蓝牙已打开");
            } else {
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH);
            }
        }else{
            //设备不支持蓝牙
            // showMsg("设备不支持蓝牙");
        }

    }

    //动态权限申请
    private void requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            String[] perms ={Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
            if (ContextCompat.checkSelfPermission(ScanActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ScanActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(ScanActivity.this,perms,REQUEST_PERMISSION_CODE);
            }
        }
    }






}