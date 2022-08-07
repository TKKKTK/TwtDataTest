package com.wg.twtdatatest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.wg.twtdatatest.Data.BleDevice;
import com.wg.twtdatatest.adapter.ListAdpter;
import com.wg.twtdatatest.adapter.ScanAdpter;
import com.wg.twtdatatest.adapter.TwtDataAdapter;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScanActivity extends AppCompatActivity {

    private List<BleDevice> deviceList = new ArrayList<>();
//    private ListAdpter listAdpter;
    private ScanAdpter scanAdpter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar state_scanning;

    //请求打开蓝牙
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    //权限请求码
    private static final int REQUEST_PERMISSION_CODE = 9527;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        checkNeedPermissions();
        openBluetooth();
        initView();
        startScan();
        state_scanning.setVisibility(View.VISIBLE);
    }


    private void initView() {
        state_scanning = (ProgressBar)findViewById(R.id.state_scanning);
        recyclerView = (RecyclerView) findViewById(R.id.device_list);
        scanAdpter = new ScanAdpter(deviceList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL)); //每隔一项设置一条水平线
        scanAdpter.setOnItemClickListener(new ScanAdpter.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BleDevice device) {
                final Intent controlDeviceIntent = new Intent(ScanActivity.this, DeviceActivity.class);
                controlDeviceIntent.putExtra(DeviceActivity.EXTRA_DEVICE, device);
                startActivity(controlDeviceIntent);
            }
        });
        recyclerView.setAdapter(scanAdpter);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.refresh_layout);
        // 设置颜色属性的时候一定要注意是引用了资源文件还是直接设置16进制的颜色，因为都是int值容易搞混
        // 设置下拉进度的背景颜色，默认就是白色的
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white);
        // 设置下拉进度的主题颜色
//        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);
        final Handler handler = new Handler();
// 下拉时触发SwipeRefreshLayout的下拉动画，动画完毕之后就会回调这个方法
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 这里是主线程
                // 一些比较耗时的操作，比如联网获取数据，需要放到子线程去执行
                new Thread(){
                    @Override
                    public void run () {
                        super.run();
                        //同步加载网络数据
                        //加载数据 完毕后 关闭刷新状态 切回主线程
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                // 加载完数据设置为不刷新状态，将下拉进度收起来
                                swipeRefreshLayout.setRefreshing(false);
                                stopScan();
                                startScan();
                                deviceList.clear();
                                scanAdpter.notifyDataSetChanged();
                            }
                        }, 200);
                    }
                }.start();
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
//                startScan();
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivityIfNeeded(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH);
//                startScan();
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

    private boolean checkNeedPermissions(){
        boolean isPermit = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //多个权限一起申请
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
            isPermit = true;
        }
        return isPermit;
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
                .setReportDelay(100)
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
                if (indexOf(bleDevice) == -1 && bleDevice.getName() != null){
                    deviceList.add(bleDevice);
                    scanAdpter.notifyItemRangeChanged(deviceList.size()-1,1);
                    //将RecyclerView定位到最后一行
                    recyclerView.scrollToPosition(deviceList.size()-1);
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