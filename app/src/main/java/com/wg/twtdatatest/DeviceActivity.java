package com.wg.twtdatatest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.data.Data;

public class DeviceActivity extends AppCompatActivity implements IreseviceDataListenner {

    private BleDevice device;
    private TwtManager twtManager;
    private ConnectRequest connectRequest;
    private Button resivice_button;
    private Button stop_resivice;
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

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(device.getName() == null ? "UnknownDevice":device.getName());
        actionBar.setSubtitle(device.getAddress());

        twtManager = new TwtManager(getApplication(),this);
        connect();

        resivice_button = (Button) findViewById(R.id.resivice_button);
        stop_resivice = (Button)findViewById(R.id.stop_resivice);
        resivice_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataListThread = new DataListThread();
                isResivice = true;
                dataListThread.start();
                twtManager.ReadData(1);
                resivice_button.setVisibility(View.GONE);
                stop_resivice.setVisibility(View.VISIBLE);
            }
        });

        stop_resivice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twtManager.ReadData(2);
                isResivice = false;
                resivice_button.setVisibility(View.VISIBLE);
                stop_resivice.setVisibility(View.GONE);
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
                twtManager.ReadData(2);
                isResivice = false;
                file_layout.setVisibility(View.VISIBLE);
                Date dNow = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
                file_name.setText(ft.format(dNow)+".txt");

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
                Intent intent1 = new Intent(DeviceActivity.this,EchartsActivity.class);
                startActivity(intent1);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 蓝牙建立连接
     */
    public void connect(){
        if (device != null){
            connectRequest = twtManager.connect(device.getDevice())
                    .retry(3,100)
                    .useAutoConnect(false)
                    .then(d -> connectRequest = null);
            connectRequest.enqueue();
        }
    }

    /**
     * 蓝牙断开连接
     */
    public void disConnect(){
        device = null;
        if (connectRequest != null){
            connectRequest.cancelPendingConnection();
        }else if (twtManager.isConnected()){
            twtManager.disconnect().enqueue();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disConnect();
    }

    /**
     * 数据包接收
     * @param data
     */
    @Override
    public void DataResevice(Data data) {
            DataPacket dataPacket = new DataPacket(data,getTimeRecord());
              dataList.add(dataPacket);
              count++;
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

    public String getTimeRecord(){
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date());
    }
}