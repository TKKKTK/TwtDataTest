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
import java.util.concurrent.TimeUnit;

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
         * θηθΏζ₯
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
         * θηζ­εΌ
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
         * εΌε―ζ°ζ?θ―»ε
         */
        public void startReadData(int state){
            STATE = state;
            twtManager.ReadData(1);
        }

        /**
         * ε³ι­ζ°ζ?θ―»ε
         */
        public void stopReadData(){
            twtManager.ReadData(2);
        }
    }

     private class reseviceDataListenner implements IreseviceDataListenner{

         @Override
         public void DataResevice(Data data) {
             Log.d("reseviceDataListenner", "DataResevice: "+data);
           switch (STATE){
               case LIST_DATA:
                   DataPacket dataPacket = new DataPacket(data,getTimeRecord());
                   //ειη»εε°δΈζ‘εΉΏζ­
                   Intent intent = new Intent("com.wg.twtdatatest.DATALIST_RESEVICE");
                   intent.putExtra("LIST_DATA",dataPacket);
                   sendBroadcast(intent);
                   break;
               case ECHARTS_DATA:
                   int[] dataInts = subData(data.toString());
                   for (int i = 0; i < dataInts.length; i++){
                           EchartsData echartsData = new EchartsData();
                           echartsData.setTime(getTimeRecord());
                           echartsData.setDataPoint(dataInts[i]);
                           catchList.add(echartsData);
                   }
//                   count++;
//                   if (count == 1){
                       //ειη»εε°ηΌε²εΊζ°ζ?ε
                       UiEchartsData uiEchartsData = new UiEchartsData();
                       uiEchartsData.setListPacket(catchList);
                       iEchartsUpdate.DrawEcharts(uiEchartsData);
//                       count = 0;
                       catchList.clear();
//                   }

                   break;
           }

         }
     }

    /**
     * ζ°ζ?ζͺε
     */
    private int[] subData(String hexString){

        String[] arr1 = hexString.split(" ");
        String[] arr = arr1[1].split("-");
        //Log.d("ε­η¬¦δΈ²θ½¬ζ°η»εηζ°ζ?οΌ", "subData: "+ Arrays.toString(arr));
        int[] dataArr = new int[arr.length];
        int[] subData = new int[15];
        int[][] echartsData = new int[5][3];
        /**
         * 16θΏεΆε­η¬¦δΈ²θ½¬ζ΄ε
         */
        for (int i = 0;i < arr.length;i++){

            dataArr[i] = Integer.valueOf(arr[i],16);
        }
        //Log.d("εε­θΏεΆθ½¬ζ΄εεηζ°ζ?", "subData: "+Arrays.toString(dataArr));
        System.arraycopy(dataArr,2,subData,0,15);
        for (int i = 0; i < echartsData.length;i++){
            System.arraycopy(subData,i*3,echartsData[i],0,3);
        }
        //ι«δ½δ½εθ½¬
        for (int i = 0; i<echartsData.length;i++){
            Stack<Integer> stack = new Stack<Integer>();
            for (int j = 0;j<echartsData[i].length;j++){
                 stack.push(echartsData[i][j]);
            }
            for (int z = 0;z<echartsData[i].length;z++){
                echartsData[i][z]=stack.pop();
            }
        }

        //ε­ζΎδΈε­θθ½¬εε­θηζ°ζ?
        int[] newdata = new int[5];

        for (int i=0;i<echartsData.length;i++){
            //Log.d("ι«δ½δ½εθ½¬εηζ°ζ?:", "subData: "+Arrays.toString(echartsData[i]));
            newdata[i] = byteToInt(echartsData[i]);

        }
        //Log.d("δΈε­θθ½¬εε­θεηζ°ζ?:", "subData: "+Arrays.toString(newdata));
        return  newdata;
    }

    //δΈε­θθ½¬εε­θζ΄ε
    private int byteToInt(int[] bytes){
        int DataInt = 0;
        for (int i = 0;i < bytes.length; i++){
            DataInt = (DataInt << 8)|bytes[i];
        }
        //Log.d("η§»δ½εηζ΄εζ°ζ?οΌ", "byteToInt: "+DataInt);
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
     * ζε‘εΌε―ζΆθͺε¨θ°η¨
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
        //εΌε―ζ°ζ?ζ₯ζΆηε¬
        reseviceDataListenner listenner = new reseviceDataListenner();
        twtManager.setIreseviceDataListenner(listenner);
        Log.d("BackgroundService:", "onBind: ζε‘εΌε―");
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        Log.d("BackgroundService:", "onBind: ζε‘εΌε―"+device.getName()+" "+device.getAddress());
        return twtBinder;
    }

    public String getTimeRecord(){
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date().getTime());
    }
}