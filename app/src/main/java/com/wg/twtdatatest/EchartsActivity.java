package com.wg.twtdatatest;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.wg.twtdatatest.Data.DataPacket;
import com.wg.twtdatatest.Data.EchartsData;
import com.wg.twtdatatest.Data.UiEchartsData;
import com.wg.twtdatatest.EDFlib.EDFException;
import com.wg.twtdatatest.EDFlib.EDFwriter;
import com.wg.twtdatatest.Service.BackgroundService;
import com.wg.twtdatatest.util.FileDownload;
import com.wg.twtdatatest.util.LineChartUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    public LineChart lineChart;
    private LineChartUtil lineChartUtil;
    private NavigationView navigationView;
    private static final int IMPORT_CODE = 100; //文件导入请求码
    private static final int SAVE_CODE = 200;
    private DrawerLayout drawerLayout; //侧滑菜单栏
    private Button open_Menu;
    private Queue<Integer> dataQueue = new LinkedList<>(); //存储导入txt的数据
    private Boolean isRead = false; //记录是否正在读取
    private Boolean isSave = false;//记录是否正在保存
    private Queue<UiEchartsData> catchData = new LinkedList<>(); //暂存需要存入EDF中的数据
    private String cache_filename; //存放缓存时的文件名
    private int count = 0; // 记录存储的包数
    private long startTime; //记录开始保存的时间
    private long stopTime; //记录结束保存的时间
    private Queue<int[]> edfDataQueue = new LinkedList<>(); //存放解析后将要存入edf中的数据
    private int[] buf;

    /**
     * 数据渲染的类型
     */
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
//       测试用
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
            @RequiresApi(api = Build.VERSION_CODES.O)
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
                        //开启存储
                    case R.id.start_save:
                        StartSave();
                        //关闭侧滑菜单
                        drawerLayout.closeDrawer(GravityCompat.END);
                        break;
                        //关闭存储
                    case R.id.stop_save:
                        StopSave();
                        //关闭侧滑菜单
                        drawerLayout.closeDrawer(GravityCompat.END);
                        break;
                }
                return false;
            }
        });

    }

    /**
     * 开启保存
     */
     private void StartSave(){
           isSave = true;
           startTime = System.currentTimeMillis();
           cache_filename = getTimeRecord()+".txt";
     }

    /**
     * 列表数据转字符串
     * @param dataList
     * @return
     */
    private String listToString(List<EchartsData> dataList){
        StringBuilder stringBuilder = new StringBuilder();
        if (dataList.size()>0){
            for (EchartsData echartsData : dataList){
                stringBuilder.append(echartsData.getDataPoint());
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 关闭保存
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void StopSave(){
        isSave = false;
        stopTime = System.currentTimeMillis();
        dataCache(); // 将剩余的数据进行缓存
        readCacheData(); //读取缓冲区数据
        WriteEdf(); //导出为EDF文件
    }

    /**
     * 数据接收回调
     */
    class EchartsDataListenner implements IEchartsUpdate{

        @Override
        public void DrawEcharts(UiEchartsData data) {
            UiEchartsData uiEchartsData = data;
            lineChartUtil.UpdateData(uiEchartsData);
            if (isSave){
                catchData.add(uiEchartsData);
                count++;
                if (count == 200){
                    dataCache();
                    count = 0;
                }
            }

        }
    }

    /**
     * 数据缓存
     */
    private void dataCache(){
        StringBuilder stringBuilder = new StringBuilder();
        while (!catchData.isEmpty()){
            List<EchartsData> list = catchData.poll().getListPacket();
            stringBuilder.append(listToString(list));
        }
        String content = stringBuilder.toString();
        try {
            FileOutputStream outputStream = openFileOutput(cache_filename,Context.MODE_APPEND);
            outputStream.write(content.getBytes());
            outputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 写入EDF文件
     */
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void WriteEdf()  {
        int i, err,
                sf1=500, // 通道1的采样频率
                edfsignals = 1; //通道数

        EDFwriter hdl;
        try
        {
            hdl = new EDFwriter("xyz.edf", EDFwriter.EDFLIB_FILETYPE_BDFPLUS, edfsignals,EchartsActivity.this);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }
        catch(EDFException e)
        {
            e.printStackTrace();
            return;
        }

        //设置信号的最大物理值
        hdl.setPhysicalMaximum(0, 3000);
        //设置信号的最小物理值
        hdl.setPhysicalMinimum(0, -3000);
        //设置信号的最大数字值
        hdl.setDigitalMaximum(0, 32767);
        //设置信号的最小数字值
        hdl.setDigitalMinimum(0, -32768);
        //设置信号的物理单位
        hdl.setPhysicalDimension(0, String.format("uV"));

        //设置采样频率
        hdl.setSampleFrequency(0, sf1);

        //设置信号标签
        hdl.setSignalLabel(0, String.format("sine 500Hz", 0 + 1));

        try
        {
            for(i=0; i<edfDataQueue.size(); i++)
            {

                err = hdl.writeDigitalSamples(edfDataQueue.poll());
                if(err != 0)
                {
                    System.out.printf("writePhysicalSamples() returned error: %d\n", err);
                    return;
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }

        hdl.writeAnnotation(0, -1, "Recording starts");

        hdl.writeAnnotation(edfDataQueue.size() * 10000, -1, "Recording ends");

        try
        {
            hdl.close();
            Toast.makeText(EchartsActivity.this, "导出EDF文件成功", Toast.LENGTH_SHORT).show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }
        catch(EDFException e)
        {
            e.printStackTrace();
            return;
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
        /**
         * 文件保存结果回调
         */
        if (requestCode == SAVE_CODE && resultCode == RESULT_OK){
//            Log.d(TAG, "onActivityResult: "+data.getData());
//            FileDownload fileDownload = new FileDownload(EchartsActivity.this,dataList);
//            fileDownload.saveToUri(data.getData());
        }
        /**
         * 文件导入结果回调
         */
        else if (requestCode == IMPORT_CODE && resultCode == RESULT_OK){
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
     * EDF数据解析
     */
    private void EdfDataSolution(String dataStr){
        String[] dataArr = dataStr.split(" ");
        Log.d("EdfDataSolution", "解析后的数据: "+ Arrays.toString(dataArr));
        buf = new int[500];
        int index = 0;
        for (int i = 0; i < dataArr.length; i++){
            if (index == 500){
                edfDataQueue.add(buf);
                buf = new int[500];
                index = 0;
            }
            buf[index] = Integer.parseInt(dataArr[i]);
            index++;
        }
    }

    /**
     * 读取缓冲区数据
     */
    private void readCacheData(){
        try {
            StringBuilder stringBuilder = new StringBuilder();
            FileInputStream inputStream = openFileInput(cache_filename);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            while((line = reader.readLine()) != null){
                stringBuilder.append(line);
            }
            EdfDataSolution(stringBuilder.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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


    /**
     * 渲染导入的数据
     */
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