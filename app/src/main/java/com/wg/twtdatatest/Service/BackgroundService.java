package com.wg.twtdatatest.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.wg.twtdatatest.Data.BleDevice;
import com.wg.twtdatatest.Data.DataPacket;
import com.wg.twtdatatest.IreseviceDataListenner;
import com.wg.twtdatatest.TwtManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.data.Data;

public class BackgroundService extends Service {

    private TwtBinder twtBinder = new TwtBinder();
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public BleDevice device;
    public TwtManager twtManager;
    public ConnectRequest connectRequest;
    private Queue<List<DataPacket>> dataQueue = new LinkedList<>();
    private List<DataPacket> catchList = new ArrayList<>();
    private int count = 0;

    public class TwtBinder extends Binder{

        /**
         * 蓝牙连接
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
         * 蓝牙断开
         */
        public void disconnect(){
            device = null;
            if (connectRequest != null){
                connectRequest.cancelPendingConnection();
            }else if (twtManager.isConnected()){
                twtManager.disconnect().enqueue();
            }
        }

        /**
         * 开启数据读取
         */
        public void startReadData(){
            twtManager.ReadData(1);
        }

        /**
         * 关闭数据读取
         */
        public void stopReadData(){
            twtManager.ReadData(2);
        }
    }

     private class reseviceDataListenner implements IreseviceDataListenner{

         @Override
         public void DataResevice(Data data) {
             Log.d("BackgroundService", "DataResevice: "+data);
             DataPacket dataPacket = new DataPacket(data,getTimeRecord());
             count++;
             if (count<=10){
                 catchList.add(dataPacket);
             }else{
                 dataQueue.add(catchList); //满10个包添加进队列
                 count=0;
                 catchList.clear();
             }
         }
     }

    /**
     * 数据列表线程
     */
    class DataListThread implements Runnable{
         @Override
         public void run() {
             Timer timer = new Timer();
             TimerTask task = new TimerTask() {
                 @Override
                 public void run() {

                 }
             };
             timer.schedule(task,0,300);
         }
     }

    public BackgroundService() {

    }

    /**
     * 服务开启时自动调用
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        twtManager = new TwtManager(getApplication());
        reseviceDataListenner listenner = new reseviceDataListenner();
        twtManager.setIreseviceDataListenner(listenner);
        Log.d("BackgroundService:", "onBind: 服务开启");
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        Log.d("BackgroundService:", "onBind: 服务开启"+device.getName()+" "+device.getAddress());
        return twtBinder;
    }

    public String getTimeRecord(){
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date());
    }
}