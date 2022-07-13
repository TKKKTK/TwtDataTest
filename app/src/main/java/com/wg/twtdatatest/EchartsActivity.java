package com.wg.twtdatatest;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.lang.System;

import no.nordicsemi.android.ble.data.Data;

public class EchartsActivity extends TwtBaseActivity implements IreseviceDataListenner {

    private WebView webView;
    private Button echarts_button;
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

        webView.setOnTouchListener((v,event)->(event.getAction() == MotionEvent.ACTION_MOVE));
        webView.loadUrl("file:///android_asset/index.html");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView.loadUrl("javascript:test()");
            }
        });
        //设置数据回调监听
        twtManager.setIreseviceDataListenner(this);
        echarts_button = (Button) findViewById(R.id.echarts_resivce);
        echarts_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twtManager.ReadData(1);
            }
        });
    }

    @Override
    public void DataResevice(Data data) {
        Log.d("EchartsActivity", "DataResevice: "+data);
       subData(data.getValue());
    }

    /**
     * 数据截取
     */
    private void subData(byte[] bytes){
       byte[] newData = new byte[15];
       byte[][] subData = new byte[5][3];
       int[] intData = new int[5];
       //把包头、脱落检测、校验位、序号包给去除
       System.arraycopy(bytes,2,newData,0,newData.length);
        Log.d("EchartsActivity", "subData: "+new Data(newData));
        for (int i = 0; i < subData.length; i++){
            System.arraycopy(newData,i*3,subData[i],0,subData[i].length);
        }

        for (int i = 0;i < subData.length; i++){
//            String hexString = "0x";
//            hexString += new String(subData[i]);
//           for (int j =0;j < subData[i].length;j++){
//               Log.d("EchartsActivity", "subData: "+subData[i][j]);
//           }

            Log.d("EchartsActivity", "subData: "+TypeConversion.bytes2HexString(subData[i]));

            intData[i] = subData[i][0] << 16 + subData[i][1] << 8 + subData[i][2];
            int r = (subData[i][2] & 0xFF) | ((subData[i][1] & 0xFF) << 8) | ((subData[i][0] & 0x0F) << 16);
            Log.d("EchartsActivity", "subData: "+r);
        }

    }
}