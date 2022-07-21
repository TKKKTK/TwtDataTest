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
import com.wg.twtdatatest.IEchartsUpdate;
import com.wg.twtdatatest.IreseviceDataListenner;
import com.wg.twtdatatest.TwtManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
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
    private IEchartsUpdate iEchartsUpdate;

    public class TwtBinder extends Binder{

        public void setIEchaertsUpdate(IEchartsUpdate listenner){
            iEchartsUpdate = listenner;
        }

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
                   //Log.d("reseviceDataListenner", "DataResevice: "+data);
                   long start = System.currentTimeMillis()*1000;
                   int[] dataInts = subData(data.toString());
                   //int[] dataInts = new int[5];
                   //Random random = new Random();
                   for (int i = 0; i < dataInts.length; i++){
                           //dataInts[i] = random.nextInt(10);
                           EchartsData echartsData = new EchartsData();
                           echartsData.setTime(getTimeRecord());
                           echartsData.setDataPoint(dataInts[i]);
                           catchList.add(echartsData);
                   }
                   count++;
                   if (count == 2){
                       //Log.d("reseviceDataListenner", "DataResevice: "+dataInts[i]);
                       //发送给前台缓冲区数据包
                       UiEchartsData uiEchartsData = new UiEchartsData();
                       uiEchartsData.setListPacket(catchList);
                       //发送给前台一条广播
//                       Intent Echartsintent = new Intent("com.wg.twtdatatest.ECHARTS_DATA");
//                       Echartsintent.putExtra("ECHARTS_DATA",uiEchartsData);
//                       sendBroadcast(Echartsintent);
                       iEchartsUpdate.DrawEcharts(uiEchartsData);
                       long end = System.currentTimeMillis()*1000;
                       long time = end-start;
                       Log.d("解码执行时间:", time +"ws");
                       count = 0;
                       catchList.clear();
                   }

                   break;
           }

         }
     }

    /**
     * 数据截取
     */
    private int[] subData(String hexString){

        String[] arr1 = hexString.split(" ");
        String[] arr = arr1[1].split("-");
        //Log.d("字符串转数组后的数据：", "subData: "+ Arrays.toString(arr));
        int[] dataArr = new int[arr.length];
        int[] subData = new int[15];
        int[][] echartsData = new int[5][3];
        /**
         * 16进制字符串转整型
         */
        for (int i = 0;i < arr.length;i++){

            dataArr[i] = Integer.valueOf(arr[i],16);
        }
        //Log.d("十六进制转整型后的数据", "subData: "+Arrays.toString(dataArr));
        System.arraycopy(dataArr,2,subData,0,15);
        for (int i = 0; i < echartsData.length;i++){
            System.arraycopy(subData,i*3,echartsData[i],0,3);
        }
        //高低位反转
        for (int i = 0; i<echartsData.length;i++){
            Stack<Integer> stack = new Stack<Integer>();
            for (int j = 0;j<echartsData[i].length;j++){
                 stack.push(echartsData[i][j]);
            }
            for (int z = 0;z<echartsData[i].length;z++){
                echartsData[i][z]=stack.pop();
            }
        }

        //存放三字节转四字节的数据
        int[] newdata = new int[5];

        for (int i=0;i<echartsData.length;i++){
            //Log.d("高低位反转后的数据:", "subData: "+Arrays.toString(echartsData[i]));
            newdata[i] = byteToInt(echartsData[i]);

        }
        //Log.d("三字节转四字节后的数据:", "subData: "+Arrays.toString(newdata));
        return  newdata;
    }

    //三字节转四字节整型
    private int byteToInt(int[] bytes){
        int DataInt = 0;
        for (int i = 0;i < bytes.length; i++){
            DataInt = (DataInt << 8)|bytes[i];
        }
        //Log.d("移位后的整型数据：", "byteToInt: "+DataInt);
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