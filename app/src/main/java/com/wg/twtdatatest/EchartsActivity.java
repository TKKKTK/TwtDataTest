package com.wg.twtdatatest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.lang.System;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import no.nordicsemi.android.ble.data.Data;

public class EchartsActivity extends TwtBaseActivity implements IreseviceDataListenner {

    private WebView webView;
    private Button echarts_button;
    private List dataList = new ArrayList();
    private boolean isResivice;
    private int count;
    private Queue dataqueue = new LinkedList();
    private Queue catchData = new LinkedList();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echarts);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.hide();
            actionBar.setDisplayShowCustomEnabled(true);
        }

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
        twtManager.setIreseviceDataListenner(EchartsActivity.this);
        echarts_button = (Button) findViewById(R.id.echarts_resivce);
        echarts_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isResivice = true;
//                EchartsDataListThread echartsDataListThread = new EchartsDataListThread();
//                echartsDataListThread.start();
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (this){
                            if (count>0){
                                int size = count;
                                count = 0;
                                Log.d("EchartsDataListThread", "run: "+size);
                                int[] catchData = new int[size];
                                //数据截取，出队列
                                for (int i = 0; i < size;i++){
                                    catchData[i] = (int)dataqueue.poll();
                                }
                                JSONObject jsonObject = new JSONObject();
                                JSONArray jsonArray = new JSONArray();
                                for (int item : catchData){
                                    jsonArray.put(item);
                                }
//                                try {
////                                    jsonObject.put(jsonArray);
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
                                //Log.d("EchartsDataListThread", "run: "+jsonArray);
                                Message message = new Message();
                                message.what = 1;
                                message.obj = jsonArray;
                                handler.sendMessage(message);

                            }
                        }

                    }
                };
                timer.schedule(task,0,50);
                twtManager.ReadData(1);

            }
        });

    }

    //图表数据刷新
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                JSONArray jsonArray =  (JSONArray)msg.obj;
                Log.d("EchartsDataListThread", "run: "+jsonArray);
                webView.evaluateJavascript("update("+ jsonArray +")",null);
            }
        }
    };

    //数据接收
    @Override
    public synchronized void DataResevice(Data data) {
        Log.d("EchartsActivity", "DataResevice: "+data);
         int[] dataInts = subData(data.getValue());
         for (int i = 0; i < dataInts.length; i++){
             dataList.add(dataInts[i]);
             dataqueue.add(dataInts[i]);
             count++;
         }
    }

    /**
     * 数据截取
     */
    private int[] subData(byte[] bytes){
       byte[] newData = new byte[15];
       byte[][] subData = new byte[5][3];
       int[] intData = new int[5];
       //把包头、脱落检测、校验位、序号包给去除
       System.arraycopy(bytes,2,newData,0,newData.length);
//        Log.d("EchartsActivity", "subData: "+new Data(newData));
        for (int i = 0; i < subData.length; i++){
            System.arraycopy(newData,i*3,subData[i],0,subData[i].length);
        }

        for (int i = 0;i < subData.length; i++){
            intData[i] = byteToInt(subData[i]);
//            Log.d("EchartsActivity", "subData: "+intData[i]);
        }

        return  intData;
    }

    //三字节转四字节整型
    private int byteToInt(byte[] bytes){
        int DataInt = 0;
        for (int i = 0;i < bytes.length; i++){
            DataInt = (DataInt << 8)|bytes[i];
        }
        if ((DataInt & 0x00800000) == 0x00800000){
            DataInt |= 0xFF000000;
        }else {
            DataInt &= 0x00FFFFFF;
        }
        return DataInt;
    }

    class EchartsDataListThread extends Thread{
        @Override
        public void run() {
            super.run();
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (this){
                                if (count>0){
                                    int size = count;
                                    count = 0;
                                    Log.d("EchartsDataListThread", "run: "+size);
                                    int[] catchData = new int[size];
                                    //数据截取，出队列
                                    for (int i = 0; i < size;i++){
                                        catchData[i] = (int)dataqueue.poll();
                                    }
                                    JSONArray jsonArray = new JSONArray();
                                    for (int item : catchData){
                                        jsonArray.put(item);
                                    }
                                    Log.d("EchartsDataListThread", "run: "+jsonArray);
                                }
                            }

                        }
                    };
                timer.schedule(task,0,50);
        }
    }

}