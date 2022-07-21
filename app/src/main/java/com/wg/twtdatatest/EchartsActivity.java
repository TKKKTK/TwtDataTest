package com.wg.twtdatatest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.wg.twtdatatest.Data.EchartsData;
import com.wg.twtdatatest.Data.UiEchartsData;
import com.wg.twtdatatest.Service.BackgroundService;
import com.wg.twtdatatest.util.FileDownload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private WebView webView;
    private Button echarts_button,echarts_save,echarts_stop;
    private List dataList = new ArrayList();
    private EchartsDataReceiver echartsDataReceiver;
    private Queue<UiEchartsData> echartsDataQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echarts);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.hide();
            actionBar.setDisplayShowCustomEnabled(true);
        }

        //创建数据接收广播,并注册
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("com.wg.twtdatatest.ECHARTS_DATA");
//        echartsDataReceiver = new EchartsDataReceiver();
//        registerReceiver(echartsDataReceiver,intentFilter);

        webView = (WebView) findViewById(R.id.lineChart);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

//        webView.setOnTouchListener((v,event)->(event.getAction() == MotionEvent.ACTION_MOVE));
        webView.loadUrl("file:///android_asset/index.html");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
//                webView.loadUrl("javascript:test()");
            }
        });

        //设置数据回调监听
        //twtManager.setIreseviceDataListenner(EchartsActivity.this);
        echarts_button = (Button) findViewById(R.id.echarts_resivce);
        echarts_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EchartsDataListenner echartsDataListenner = new EchartsDataListenner();
                twtBinder.setIEchaertsUpdate(echartsDataListenner);
                twtBinder.startReadData(BackgroundService.ECHARTS_DATA);
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
//                        JSONObject jsonObject = new JSONObject();
//                        JSONArray dataArray = new JSONArray();
//                        JSONArray timeArray = new JSONArray();
//                            if (!echartsDataQueue.isEmpty()){
//
//                                UiEchartsData uiEchartsData = echartsDataQueue.poll();
//                                List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
//                                for (EchartsData echartsData : echartsDataList){
//                                     dataArray.put(echartsData.getDataPoint());
//                                     timeArray.put(echartsData.getTime());
//                                }
//
//                            }else{
//                                for (int i = 0; i < 10; i ++){
//                                    dataArray.put(0);
//                                    timeArray.put(new SimpleDateFormat("HH:mm:ss:SS").format(new Date()));
//                                }
//                            }
//                        try {
//                            jsonObject.put("data",dataArray);
//                            jsonObject.put("time",timeArray);
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        //Log.d("EchartsDataListThread", "run: "+jsonArray);
//                        Message message = new Message();
//                        message.what = 1;
//                        message.obj = jsonObject;
//                        handler.sendMessage(message);
                        }
                };
               // timer.schedule(task,0,1);

                /**
                 * 保存数据
                 */
                echarts_save = (Button) findViewById(R.id.echarts_save);
                echarts_save.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onClick(View view) {
                        FileDownload fileDownload = new FileDownload(new SimpleDateFormat("HH:mm:ss:SS").format(new Date())+".txt",EchartsActivity.this,dataList);
                        fileDownload.saveEchartsData();
                    }
                });

                /**
                 * 停止接收
                 */
                echarts_stop = (Button) findViewById(R.id.echarts_stop);
                echarts_stop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        twtBinder.stopReadData();
                    }
                });

            }
        });

    }


    class EchartsDataListenner implements IEchartsUpdate{

        @Override
        public void DrawEcharts(UiEchartsData data) {
            UiEchartsData uiEchartsData = data;
            //            echartsDataQueue.add(uiEchartsData);
            JSONObject jsonObject = new JSONObject();
            JSONArray dataArray = new JSONArray();
            JSONArray timeArray = new JSONArray();
//                 if (!echartsDataQueue.isEmpty()){
//
//                     UiEchartsData uiEchartsData1 = echartsDataQueue.poll();
            List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
            for (EchartsData echartsData : echartsDataList){
                dataArray.put(echartsData.getDataPoint());
                timeArray.put(echartsData.getTime());
            }

            //}
            try {
                jsonObject.put("data",dataArray);
                jsonObject.put("time",timeArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            webView.evaluateJavascript("update("+ jsonObject.toString() +")",null);
            // List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
            for (EchartsData echartsData : echartsDataList){
                //Log.d("EchartsDataReceiver", "onReceive: "+echartsData.getDataPoint());
                dataList.add(echartsData.getDataPoint());
            }
        }
    }



    //数据接收广播
    class EchartsDataReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            UiEchartsData uiEchartsData = intent.getParcelableExtra("ECHARTS_DATA");
//            echartsDataQueue.add(uiEchartsData);
            JSONObject jsonObject = new JSONObject();
             JSONArray dataArray = new JSONArray();
             JSONArray timeArray = new JSONArray();
//                 if (!echartsDataQueue.isEmpty()){
//
//                     UiEchartsData uiEchartsData1 = echartsDataQueue.poll();
              List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
              for (EchartsData echartsData : echartsDataList){
                   dataArray.put(echartsData.getDataPoint());
                   timeArray.put(echartsData.getTime());
              }

                 //}
             try {
                 jsonObject.put("data",dataArray);
                 jsonObject.put("time",timeArray);
             } catch (JSONException e) {
                 e.printStackTrace();
             }
            webView.evaluateJavascript("update("+ jsonObject.toString() +")",null);
           // List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
            for (EchartsData echartsData : echartsDataList){
                //Log.d("EchartsDataReceiver", "onReceive: "+echartsData.getDataPoint());
                dataList.add(echartsData.getDataPoint());
            }
        }
    }

    //图表数据刷新
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                JSONObject jsonObject =  (JSONObject) msg.obj;
                //Log.d("EchartsDataListThread", "run: "+jsonObject);
                webView.evaluateJavascript("update("+ jsonObject.toString() +")",null);
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver(echartsDataReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(echartsDataReceiver);
    }
}