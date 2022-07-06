package com.wg.twtdatatest;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

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
    private List<Data> dataList = new ArrayList<>();
    private TwtDataAdapter twtDataAdapter;
    private EditText file_name;
    private Button save;
    private LinearLayout file_layout;

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
        resivice_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twtManager.startRead();
            }
        });

        RecyclerView recyclerView =(RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        twtDataAdapter = new TwtDataAdapter(dataList);
        twtDataAdapter.setOnItemClikListener(this);
        recyclerView.setAdapter(twtDataAdapter);

        file_layout = (LinearLayout)findViewById(R.id.file_layout);
        file_name = (EditText) findViewById(R.id.file_name);
        save = (Button) findViewById(R.id.file_download);

    }

    public void connect(){
        if (device != null){
            connectRequest = twtManager.connect(device.getDevice())
                    .retry(3,100)
                    .useAutoConnect(false)
                    .then(d -> connectRequest = null);
            connectRequest.enqueue();
        }
    }

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

    @Override
    public void DataResevice(Data data) {
            dataList.add(data);
            twtDataAdapter.notifyItemInserted(dataList.size()-1);
    }

    @Override
    public void onItemClik(Data data) {
        twtManager.stopRead();
        file_name.setText(new Date().toString());
        file_layout.setVisibility(View.VISIBLE);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 FileDownload fileDownload = new FileDownload(file_name.getText().toString(),DeviceActivity.this);
                 fileDownload.save("123456");
            }
        });
    }
}