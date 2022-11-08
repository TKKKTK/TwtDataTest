package com.wg.twtdatatest;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wg.twtdatatest.Data.DataPacket;
import com.wg.twtdatatest.Service.BackgroundService;
import com.wg.twtdatatest.util.FileDownload;
import com.wg.twtdatatest.adapter.TwtDataAdapter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import no.nordicsemi.android.ble.data.Data;

public class DeviceActivity extends TwtBaseActivity{

    private static final int SAVE_CODE = 200;
    private Button resivice_button;
    private Button stop_resivice;
    private Button zhiding_button;
    private Button open_Inform;
    private Button close_Inform;
    private List<DataPacket> dataList = new ArrayList<>();
    private TwtDataAdapter twtDataAdapter;
    private EditText file_name;
    private Button save;
    private LinearLayout file_layout;
    private RecyclerView recyclerView;
    private DataListThread dataListThread;
    private boolean isResivice;
    private int count;
    private FloatingActionButton download;
    private FloatingActionButton echarts;
    private DataListReceiver dataListReceiver; //接收后台服务的广播

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        //标题栏设置设备名和地址
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(device.getName() == null ? "UnknownDevice":device.getName());
        actionBar.setSubtitle(device.getAddress());


        //开启数据接收的广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.wg.twtdatatest.DATALIST_RESEVICE");
        dataListReceiver = new DataListReceiver();
        registerReceiver(dataListReceiver,intentFilter);

        resivice_button = (Button) findViewById(R.id.resivice_button);
        stop_resivice = (Button)findViewById(R.id.stop_resivice);
        resivice_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataListThread = new DataListThread();
                isResivice = true;
                dataListThread.start();
                //开启后台接收数据
                twtBinder.startReadData(BackgroundService.LIST_DATA);
                resivice_button.setVisibility(View.GONE);
                stop_resivice.setVisibility(View.VISIBLE);
            }
        });

        stop_resivice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twtBinder.stopReadData();
                isResivice = false;
                resivice_button.setVisibility(View.VISIBLE);
                stop_resivice.setVisibility(View.GONE);
            }
        });

       open_Inform = (Button)findViewById(R.id.open_inform);
       close_Inform = (Button)findViewById(R.id.close_inform);
       open_Inform.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               dataListThread = new DataListThread();
               isResivice = true;
               dataListThread.start();
               //开启数据接收通知
               twtBinder.openNotifications(BackgroundService.LIST_DATA);
               open_Inform.setVisibility(View.GONE);
               close_Inform.setVisibility(View.VISIBLE);
           }
       });
       close_Inform.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               //关闭数据接收通知
               twtBinder.closeNotifications();
               isResivice = false;
               open_Inform.setVisibility(View.VISIBLE);
               close_Inform.setVisibility(View.GONE);
           }
       });


        zhiding_button = (Button)findViewById(R.id.zhiding);
        zhiding_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //将RecyclerView定位到第一行
                recyclerView.scrollToPosition(0);
            }
        });

        recyclerView =(RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        twtDataAdapter = new TwtDataAdapter(dataList);
        recyclerView.setAdapter(twtDataAdapter);

        file_layout = (LinearLayout)findViewById(R.id.file_layout);
        file_name = (EditText) findViewById(R.id.file_name);
        save = (Button) findViewById(R.id.file_download);
        download = (FloatingActionButton) findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twtBinder.stopReadData();
                isResivice = false;
                file_layout.setVisibility(View.VISIBLE);
                Date dNow = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
                file_name.setText(ft.format(dNow)+".txt");

                //文件保存按钮
                save.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onClick(View view) {
                        FileDownload fileDownload = new FileDownload(dataList,file_name.getText().toString(),DeviceActivity.this);
                        String path = fileDownload.save();

                        //调用系统文件管理器打开指定路径目录
                        if (path != null){
                            file_layout.setVisibility(View.GONE);
                            AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this);
                            builder.setTitle("文件保存成功");
                            builder.setMessage("路径为："+path+"/"+file_name.getText());
                            builder.setCancelable(true);
                            builder.setPositiveButton("OK",null);
                            builder.show();
                        }else {
                            file_layout.setVisibility(View.GONE);
                            AlertDialog.Builder builder = new AlertDialog.Builder(DeviceActivity.this);
                            builder.setTitle("文件保存失败");
                            builder.setCancelable(true);
                            builder.setPositiveButton("OK",null);
                            builder.show();
                        }

                    }
                });
            }
        });

        echarts = (FloatingActionButton) findViewById(R.id.echarts);
        echarts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(DeviceActivity.this, EchartsActivity.class);
                intent1.putExtra(EXTRA_DEVICE,device);
                startActivity(intent1);
            }
        });

    }



    //接收数据的广播
    class DataListReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //Toast.makeText(DeviceActivity.this,"数据接收",Toast.LENGTH_LONG).show();
            DataPacket dataPacket = intent.getParcelableExtra("LIST_DATA");
            //脱落检测
            //DataSolation(dataPacket.getData().toString());
            dataList.add(dataPacket);
            count++;
            //Log.d("DeviceActivity", "onReceive: "+dataPacket.getData());
        }
    }

    /**
     *数据解析及脱落判断
     */
    private void DataSolation(String hexString){
        String[] arr1 = hexString.split(" ");
        String[] arr = arr1[1].split("-");
        //Log.d("字符串转数组后的数据：", "subData: "+ Arrays.toString(arr));
        int[] dataArr = new int[arr.length];//记录原始数据
        int fallOff = 0; //脱落位
        int verify = 0; //校验位

        /**
         * 16进制字符串转整型
         */
        for (int i = 0;i < arr.length;i++){

            dataArr[i] = Integer.valueOf(arr[i],16);
        }
        fallOff = dataArr[17];
        verify = dataArr[18];
        Log.d(TAG, "脱落位: "+dataArr[17]);
        Log.d(TAG, "校验位: "+dataArr[18]);

        if (fallOff == 1){
            Toast.makeText(DeviceActivity.this,"设备脱落",Toast.LENGTH_LONG).show();
        }else {
            //Toast.makeText(DeviceActivity.this,"设备未脱落",Toast.LENGTH_LONG).show();
        }

    }


    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver(dataListReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        twtBinder.disconnect(); //断开蓝牙连接
        unbindService(connection); //解绑服务
    }

    class DataListThread extends Thread{
        @Override
        public void run() {
            super.run();
            while (isResivice){
                try {
                    Thread.sleep(1000);
                    Message message = new Message();
                    message.what = 1;
                    message.arg1 = count;
                    handler.sendMessage(message);
                    count = 0;
                }catch (InterruptedException e){
                    e.printStackTrace();
                    isResivice = false;
                }

            }
        }
    }


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
               if (msg.what == 1){
                   int count = msg.arg1;
                   twtDataAdapter.notifyItemRangeChanged(dataList.size()-1,count);
                   //将RecyclerView定位到最后一行
                   recyclerView.scrollToPosition(dataList.size()-1);
               }
        }
    };


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
            FileDownload fileDownload = new FileDownload(DeviceActivity.this,dataList);
            fileDownload.save(data.getData());
        }
    }


    public String getTimeRecord(){
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date());
    }
}