package com.wg.twtdatatest.Service;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import com.wg.twtdatatest.Data.BleDevice;
import com.wg.twtdatatest.Data.DataPacket;
import com.wg.twtdatatest.Data.EchartsData;
import com.wg.twtdatatest.Data.UiEchartsData;
import com.wg.twtdatatest.IreseviceDataListenner;
import com.wg.twtdatatest.TwtManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.data.Data;

public class BackgroundService extends Service {

    private TwtBinder twtBinder = new TwtBinder();
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final int LIST_DATA = 0;
    public static final int ECHARTS_DATA = 1;
    public  int STATE = -1;
    public BleDevice device;
    public TwtManager twtManager;
    public ConnectRequest connectRequest;
    private Queue<List<EchartsData>> dataQueue = new LinkedList<>();
    private List<EchartsData> catchList = new ArrayList<>();
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
        public void startReadData(int state){
            STATE = state;
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
           switch (STATE){
               case LIST_DATA:
                   DataPacket dataPacket = new DataPacket(data,getTimeRecord());
                   //发送给前台一条广播
                   Intent intent = new Intent("com.wg.twtdatatest.DATALIST_RESEVICE");
                   intent.putExtra("LIST_DATA",dataPacket);
                   sendBroadcast(intent);
                   break;
               case ECHARTS_DATA:
                   Log.d("reseviceDataListenner", "DataResevice: "+data);
                   int[] dataInts = subData(data.getValue());
                   for (int i = 0; i < dataInts.length; i++){
                       count++;
                       if (count<=10){
                           EchartsData echartsData = new EchartsData();
                           echartsData.setTime(getTimeRecord());
                           echartsData.setDataPoint(dataInts[i]);
                           catchList.add(echartsData);
                       }else {
                           //Log.d("reseviceDataListenner", "DataResevice: "+dataInts[i]);
                           //发送给前台缓冲区数据包
                           UiEchartsData uiEchartsData = new UiEchartsData();
                           uiEchartsData.setListPacket(catchList);
                           //发送给前台一条广播
                           Intent Echartsintent = new Intent("com.wg.twtdatatest.ECHARTS_DATA");
                           Echartsintent.putExtra("ECHARTS_DATA",uiEchartsData);
                           sendBroadcast(Echartsintent);

                           count = 0;
                           catchList.clear();
                           count++;
                           EchartsData echartsData = new EchartsData();
                           echartsData.setTime(getTimeRecord());
                           echartsData.setDataPoint(dataInts[i]);
                           catchList.add(echartsData);
                       }
                   }

                   break;
           }

         }
     }

    /**
     * 数据截取
     */
    private int[] subData(byte[] bytes){
        byte[] newData = new byte[15];
        byte[][] subData = new byte[5][3];
        int[] intData = new int[5];
        Queue<Byte> byteQueue = new LinkedList<>();
        //把包头、脱落检测、校验位、序号包给去除
        System.arraycopy(bytes,2,newData,0,newData.length);
//        Log.d("EchartsActivity", "subData: "+new Data(newData));
        for (int i = 0; i < subData.length; i++){
            System.arraycopy(newData,i*3,subData[i],0,subData[i].length);
        }

        //打印原始数据
        for (int i = 0; i < bytes.length; i++){
            Log.d("BackgroundService", "subData: " + (int)bytes[i]);
        }

        /**
         * 高位变低位，数据位反转
         */
        for (int i = 0;i < subData.length; i++){
            Stack<Byte> byteStack = new Stack<>();
            for (int j = 0; j < subData[i].length; j++){
                //Log.d("BackgroundService", "subData: "+subData[i][j]);
                byteStack.push(subData[i][j]);
            }
            for (int z = 0;z <subData[i].length;z++){
                subData[i][z] = byteStack.pop();
                //Log.d("BackgroundService", "subData: "+subData[i][z]);
            }
        }

        /**
         * 三字节转四字节
         */
        for (int i = 0; i < subData.length; i++){
            intData[i] = byteToInt(subData[i]);
        }

//        List<Byte> subDataList = new ArrayList();

        for (int i = 0; i < subData.length;i++){
            for (int j = 0; j < subData[i].length;j++){
//                subDataList.add(subData[i][j]);
              //  Log.d("BackgroundService", "subData: "+subData[i][j]);
            }
        }

       // Log.d("BackgroundService", "subData: "+subDataList);
        return  intData;
    }

    //三字节转四字节整型
    private int byteToInt(byte[] bytes){
        int DataInt = 0x00000000;
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
        //开启数据接收监听
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