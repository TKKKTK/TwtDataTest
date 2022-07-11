package com.wg.twtdatatest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.data.Data;

public class DeviceActivity extends AppCompatActivity implements IreseviceDataListenner, TwtDataAdapter.OnItemClikListener {

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
    private List<DataPacket> cacheData = new ArrayList<>();
    private DataListThread dataListThread;
    private boolean isResivice;
    private int count;

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

        twtDataAdapter = new TwtDataAdapter(dataList);
        twtDataAdapter.setOnItemClikListener(this);
        recyclerView.setAdapter(twtDataAdapter);

        file_layout = (LinearLayout)findViewById(R.id.file_layout);
        file_name = (EditText) findViewById(R.id.file_name);
        save = (Button) findViewById(R.id.file_download);
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
//            cacheData.add(dataPacket);
              dataList.add(dataPacket);
              count++;
//            twtDataAdapter.notifyItemInserted(dataList.size()-1);
//            //将RecyclerView定位到最后一行
//            recyclerView.scrollToPosition(dataList.size()-1);
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

    @Override
    public void onItemClik(DataPacket data) {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
        file_name.setText(ft.format(dNow)+".txt");
        twtManager.ReadData(2);
        file_layout.setVisibility(View.VISIBLE);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 FileDownload fileDownload = new FileDownload(file_name.getText().toString(),DeviceActivity.this);
                 fileDownload.save(data.getData());
            }
        });
    }

    public String getTimeRecord(){
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date());
    }
}