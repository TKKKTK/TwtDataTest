package com.wg.twtdatatest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

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
        startScan();
    }


    private void initView() {
        ListView listView = (ListView) findViewById(R.id.list_view);
        listAdpter = new ListAdpter(ScanActivity.this, R.layout.list_item, deviceList);
        listView.setAdapter(listAdpter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BleDevice device = listAdpter.getItem(i);
                final Intent controlDeviceIntent = new Intent(ScanActivity.this, DeviceActivity.class);
                controlDeviceIntent.putExtra(DeviceActivity.EXTRA_DEVICE, device);
                startActivity(controlDeviceIntent);
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }

            }
        }else{
            //设备不支持蓝牙
            // showMsg("设备不支持蓝牙");
        }

    }

    //动态权限申请
    private void requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            String[] perms ={Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (ContextCompat.checkSelfPermission(ScanActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ScanActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(ScanActivity.this,perms,REQUEST_PERMISSION_CODE);
            }
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            String[] connectPerms = {Manifest.permission.BLUETOOTH_CONNECT};
            if (ContextCompat.checkSelfPermission(ScanActivity.this,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(ScanActivity.this,connectPerms,REQUEST_PERMISSION_CODE);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    /**
     * 开始扫描
     */
    public void startScan(){
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(500)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(null,settings,scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(@NonNull List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results){
                BleDevice bleDevice = new BleDevice(result);
                if (indexOf(bleDevice) == -1){
                    listAdpter.add(bleDevice);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.w("ScannerViewModel", "Scanning failed with code " + errorCode);
            if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                stopScan();
                startScan();
            }
        }
    };

    /**
     * 停止扫描
     */
    public void stopScan(){
         final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
         scanner.stopScan(scanCallback);
    }

    /**
     * 去重判断
     */
    public int indexOf(BleDevice bleDevice){
        int i = 0;
        for (BleDevice device : deviceList){
            if (device.getAddress().equals(bleDevice.getAddress()))
                return i;
            i++;
        }
        return -1;
    }

}