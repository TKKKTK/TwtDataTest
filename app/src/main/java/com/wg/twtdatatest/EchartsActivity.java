package com.wg.twtdatatest;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.navigation.NavigationView;
import com.wg.twtdatatest.Data.EchartsData;
import com.wg.twtdatatest.Data.UiEchartsData;
import com.wg.twtdatatest.Service.BackgroundService;
import com.wg.twtdatatest.util.FileDownload;
import com.wg.twtdatatest.util.LineChartUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.lang.System;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import no.nordicsemi.android.ble.data.Data;

public class EchartsActivity extends TwtBaseActivity {

    private List dataList = new ArrayList();
    public LineChart lineChart;
    private LineChartUtil lineChartUtil;
    private NavigationView navigationView;
    private static final int IMPORT_CODE = 100;
    private static final int SAVE_CODE = 200;
    private DrawerLayout drawerLayout;
    private Button open_Menu;
    private Queue<Integer> dataQueue = new LinkedList<>();
    private Boolean isRead = false;
    private int count = 0;

    private enum ReciveType{
        Import,
        DeviceData,
    }

    private ReciveType state = ReciveType.DeviceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echarts);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.hide();
            actionBar.setDisplayShowCustomEnabled(true);
        }
        drawerLayout = findViewById(R.id.drawer_layout);
        lineChart = findViewById(R.id.Chart);
        lineChartUtil = new LineChartUtil(lineChart);
        open_Menu = (Button)findViewById(R.id.open_Menu);
        open_Menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //   Log.d(TAG, "点击打开菜单");
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

//        EchartsDataListenner echartsDataListenner = new EchartsDataListenner();
//        twtBinder.setIEchaertsUpdate(echartsDataListenner);

//        new Thread(){
//            @Override
//            public void run() {
//                super.run();
//                while (true){
//                    try {
//                        Thread.sleep(262);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    send();
//                }
//
//            }
//        }.start();
//        send();

        navigationView = (NavigationView)findViewById(R.id.nav_vew);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id){
                    case R.id.start:
                        switch (state){
                            case DeviceData:
                                EchartsDataListenner echartsDataListenner = new EchartsDataListenner();
                                twtBinder.setIEchaertsUpdate(echartsDataListenner);
                                twtBinder.startReadData(BackgroundService.ECHARTS_DATA);
                                break;
                            case Import:
                                ChartUpdateThread chartUpdateThread = new ChartUpdateThread();
                                isRead = true;
                                new Thread(chartUpdateThread).start();
                                break;
                        }
                        //关闭侧滑菜单
                        drawerLayout.closeDrawer(GravityCompat.END);
                        break;
                    case R.id.stop:
                        switch (state){
                            case DeviceData:
                                twtBinder.stopReadData();
                                break;
                            case Import:
                                isRead = false;
                                break;
                        }
                        //关闭侧滑菜单
                        drawerLayout.closeDrawer(GravityCompat.END);
                        break;
                    case R.id.Import:
                        state = ReciveType.Import;
                        ImportFile();
                        //关闭侧滑菜单
                        drawerLayout.closeDrawer(GravityCompat.END);
                        break;
                    case R.id.save:
                        openFileSave();
                        //关闭侧滑菜单
                        drawerLayout.closeDrawer(GravityCompat.END);
                        break;
                }
                return false;
            }
        });

    }

    private void send(){
        count ++;
        if (count % 2 ==0){
            twtBinder.startReadData(BackgroundService.ECHARTS_DATA);
        }else{
            twtBinder.stopReadData();
        }
    }



    /**
     * 数据接收回调
     */
    class EchartsDataListenner implements IEchartsUpdate{

        @Override
        public void DrawEcharts(UiEchartsData data) {
            UiEchartsData uiEchartsData = data;
            lineChartUtil.UpdateData(uiEchartsData);
            List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
//            for (EchartsData echartsData : echartsDataList){
//                dataList.add(echartsData.getDataPoint());
//            }
//            Log.d(TAG, "容器长度: "+dataList.size());
        }
    }

    /**
     * 选择对应的保存路径
     */
    private void openFileSave(){
        Uri uri = MediaStore.Files.getContentUri("external");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/txt");
        intent.putExtra(Intent.EXTRA_TITLE, getTimeRecord()+".txt");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        startActivityIfNeeded(intent,SAVE_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_CODE && resultCode == RESULT_OK){
//            Log.d(TAG, "onActivityResult: "+data.getData());
//            FileDownload fileDownload = new FileDownload(EchartsActivity.this,dataList);
//            fileDownload.saveToUri(data.getData());
        }else if (requestCode == IMPORT_CODE && resultCode == RESULT_OK){
            //进度条对话框
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("正在导入");
            progressDialog.setMessage("Loading...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.setIcon(R.drawable.ic_launcher_background);
            progressDialog.show();

//            Log.d("是否选取到文件:", "onActivityResult: ");
            Uri uri = data.getData();
//            Log.d(TAG, "文件的uri: "+uri);
            InputStream inputStream = null;
            StringBuilder stringBuilder = new StringBuilder();
            try {
                inputStream = getContentResolver().openInputStream(uri);

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = reader.readLine()) != null){
                    stringBuilder.append(line);
                }
                progressDialog.cancel();
//                Log.d(TAG, "读取到的数据: "+stringBuilder);
                DataSolution(stringBuilder.toString());
                Toast.makeText(EchartsActivity.this, "读取成功", Toast.LENGTH_SHORT).show();

                //read_text.setText(stringBuilder);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据解析
     */
    private void DataSolution(String dataStr){
        String[] dataArr = dataStr.split(" ");
//        Log.d(TAG, "DataSolution: "+ Arrays.toString(dataArr));
        for (int i = 0; i < dataArr.length; i++){
            dataQueue.add(Integer.parseInt(dataArr[i]));
        }
    }

    /**
     * 文件导入
     */
    private void ImportFile(){
        Uri uri = MediaStore.Files.getContentUri("external");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        startActivityIfNeeded(intent,IMPORT_CODE);
    }


    class ChartUpdateThread implements Runnable{
        @Override
        public void run() {
            while (isRead){
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //lineChartUtil.addEntry();
                if (!dataQueue.isEmpty()){
                    List<Integer> dataPoints = new ArrayList<>();
                    for (int i = 0; i<10; i++){
                        dataPoints.add(dataQueue.poll());
                    }
                    lineChartUtil.UpdateData(dataPoints);
                }

            }

        }
    }

    public String getTimeRecord(){
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date().getTime());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}