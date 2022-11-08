package com.wg.twtdatatest.Service;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

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
         * 开启通知
         */
         public void openNotifications(int state){
             STATE = state;
             twtManager.startNotifications();
         }

        /**
         * 关闭通知
         */
        public void closeNotifications(){
            twtManager.stopNotifications();
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
             //Log.d("reseviceDataListenner", "DataResevice: "+data);
           switch (STATE){
               case LIST_DATA:
                   DataPacket dataPacket = new DataPacket(data,getTimeRecord());
                   //发送给前台一条广播
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
                        //此处对对象做记录
                        echartsData.setRecord(false);
                        catchList.add(echartsData);

                   }
                       //发送给前台缓冲区数据包
                       UiEchartsData uiEchartsData = new UiEchartsData();
                       uiEchartsData.setListPacket(catchList);
                       iEchartsUpdate.DrawEcharts(uiEchartsData);
                       catchList.clear();
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
        int[] dataArr = new int[arr.length];//记录原始数据
        int[] subData = new int[15]; //存放截取的数据
        int[][] echartsData = new int[5][3]; //存放反转后的数据
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
        if (fallOff == 1){
              iEchartsUpdate.ShowMessage("设备脱落");
        }else {
           // iEchartsUpdate.ShowMessage("设备未脱落");
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

    /**
     * 在服务绑定时调用
     * @param intent
     * @return
     */
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
        return new SimpleDateFormat("HH:mm:ss:SS").format(new Date().getTime());
    }
}