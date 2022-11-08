package com.wg.twtdatatest;

import static android.content.ContentValues.TAG;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.navigation.NavigationView;
import com.wg.twtdatatest.Data.EchartsData;
import com.wg.twtdatatest.Data.UiEchartsData;
import com.wg.twtdatatest.EDFlib.EDFException;
import com.wg.twtdatatest.EDFlib.EDFwriter;
import com.wg.twtdatatest.Service.BackgroundService;
import com.wg.twtdatatest.util.FileDownload;
import com.wg.twtdatatest.util.LineChartUtil;
import com.wg.twtdatatest.util.MyObjectInputStream;
import com.wg.twtdatatest.util.ObjectAppendOutputStream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.lang.System;
import java.util.Queue;

public class EchartsActivity extends TwtBaseActivity implements View.OnClickListener{

    public LineChart lineChart;
    private LineChartUtil lineChartUtil;
    private NavigationView navigationView;
    private static final int IMPORT_CODE = 100; //文件导入请求码
    private static final int SAVE_CODE = 200;
    private DrawerLayout drawerLayout; //侧滑菜单栏
    private Button open_Menu;
    private Queue<Integer> dataQueue = new LinkedList<>(); //存储导入txt的数据
    private Boolean isRead = false; //记录是否正在读取 是用于导入txt用的
    private Boolean isSave = false;//记录是否正在保存
    private Queue<UiEchartsData> catchData = new LinkedList<>(); //暂存需要存入EDF中的数据
    private String cache_filename; //存放缓存时的文件名
    private Queue<int[]> edfDataQueue = new LinkedList<>(); //存放解析后将要存入edf中的数据

    private List<int[]> edfDataList = null; //存放解析后将要存入edf中的数据
    //存放反序列化后的数据
    private Queue<EchartsData> deserialization = new LinkedList<>();
    //存放用于记录标签的数据
    private Queue<EchartsData> tagDataQueue = new LinkedList<>();
    //用于存放测试反序列化后的数据
    private List dataList = new LinkedList();

    private int count = 0;
    private boolean isRecordClik = false;

    /**
     * 按钮组
     */
    private Button start_data; // 开启数据渲染
    private Button stop_data; //关闭数据渲染
    private Button start_save; // 开启存储
    private Button import_edf; //导出为edf
    private Button record; //记录
    private Button back; // 返回上一个界面

    /**
     * 数据渲染的类型
     */
    private enum ReciveType{
        Import, //所要导入的txt文件
        DeviceData, //实时设备数据渲染
    }

    private ReciveType state = ReciveType.DeviceData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echarts);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.hide();
            actionBar.setDisplayShowCustomEnabled(true);
        }
//        drawerLayout = findViewById(R.id.drawer_layout);
        lineChart = findViewById(R.id.Chart);
        lineChartUtil = new LineChartUtil(lineChart);
        //按钮组
        start_data = (Button) findViewById(R.id.start_data);
        stop_data = (Button) findViewById(R.id.stop_data);
        start_save = (Button) findViewById(R.id.start_save);
        import_edf = (Button) findViewById(R.id.import_edf);
        record = (Button) findViewById(R.id.recorde);
        back = (Button) findViewById(R.id.back);

        start_data.setOnClickListener(this);
        stop_data.setOnClickListener(this);
        start_save.setOnClickListener(this);
        import_edf.setOnClickListener(this);
        record.setOnClickListener(this);
        back.setOnClickListener(this);

        //初始化按钮状态
        stop_data.setEnabled(false);
        import_edf.setEnabled(false);

    }

    /**
     * 按钮组点击事件
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.start_data: //开启数据渲染事件
                switch (state){
                    case DeviceData:
                        EchartsDataListenner echartsDataListenner = new EchartsDataListenner();
                        twtBinder.setIEchaertsUpdate(echartsDataListenner);
                        twtBinder.startReadData(BackgroundService.ECHARTS_DATA);
                        break;
                    case Import:
                        ChartUpdateThread chartUpdateThread = new ChartUpdateThread();
                        isRead = true;
                        new Thread(chartUpdateThread).start();break;}
                    start_data.setEnabled(false);
                    stop_data.setEnabled(true);
                break;
            case R.id.stop_data: //停止数据渲染事件
                 switch (state){
                    case DeviceData:
                        twtBinder.stopReadData();
                        break;
                    case Import:
                        isRead = false;
                        break;
                     }
                start_data.setEnabled(true);
                stop_data.setEnabled(false);
                break;
            case R.id.start_save: //开始存储数据
                StartSave();
                start_save.setEnabled(false);
                import_edf.setEnabled(true);
                break;
            case R.id.import_edf: //导出为edf
                StopSave();
                start_save.setEnabled(true);
                import_edf.setEnabled(false);
                break;
            case R.id.recorde: //记录
                 isRecordClik = true;
                break;
            case R.id.back: //返回上一个界面
                finish();
                break;
        }
    }

    /**
     * 开启保存
     */
     private void StartSave(){
           isSave = true;
           count = 0;
           cache_filename = getTimeRecord()+".txt";
     }

    /**
     * 列表数据转字符串
     * @param dataList
     * @return
     */
    private String listToString(List<EchartsData> dataList){
        StringBuilder stringBuilder = new StringBuilder();
        if (dataList.size()>0){
            for (EchartsData echartsData : dataList){
                stringBuilder.append(echartsData.getDataPoint());
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 关闭保存
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void StopSave(){
        isSave = false;
        Log.d(TAG, "总共存储的数据量:"+count);
        dataCache(); // 将剩余的数据进行缓存--序列化
        //readCacheData(); //读取缓冲区数据
        readCacheDataThread thread = new readCacheDataThread();
        new Thread(thread).start();
    }

    /**
     * 数据接收回调
     */
    class EchartsDataListenner implements IEchartsUpdate{

        /**
         * 数据接收回调
         * @param data
         */
        @Override
        public void DrawEcharts(UiEchartsData data) {
            UiEchartsData uiEchartsData = data;
            List<EchartsData> echartsDataList = uiEchartsData.getListPacket();
            for (int i = 0;i < echartsDataList.size();i++){
                EchartsData echartsData = echartsDataList.get(i);
            if (isRecordClik){
                echartsData.setRecord(true);
                isRecordClik = false;
            }
                echartsDataList.set(i,echartsData);
            }
            uiEchartsData.setListPacket(echartsDataList);
            if (isSave){
                catchData.add(uiEchartsData);
                dataCache();
                count += 5;
            }
            lineChartUtil.UpdateData(uiEchartsData);
        }

        /**
         * 界面消息提示
         * @param msg
         */
        @Override
        public void ShowMessage(String msg) {
            Toast.makeText(EchartsActivity.this,msg,Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 数据缓存 -- 序列化
     */
    private void dataCache(){
        try {
            while (!catchData.isEmpty()){
                ObjectAppendOutputStream objectOutputStream = new ObjectAppendOutputStream(openFileOutput(cache_filename,Context.MODE_APPEND));
                List<EchartsData> list = catchData.poll().getListPacket();
//                for (EchartsData echartsData : list){
                    objectOutputStream.writeObject(list);
//                }
                if (!isSave){
                    objectOutputStream.writeObject(null);
                }
                objectOutputStream.flush();
                objectOutputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
//        StringBuilder stringBuilder = new StringBuilder();
//        while (!catchData.isEmpty()){
//            List<EchartsData> list = catchData.poll().getListPacket();
//            stringBuilder.append(listToString(list));
//        }
//        String content = stringBuilder.toString();
//        try {
//            FileOutputStream outputStream = openFileOutput(cache_filename,Context.MODE_APPEND);
//            outputStream.write(content.getBytes());
//            outputStream.close();
//        }catch (IOException e){
//            e.printStackTrace();
//        }
    }

    /**
     * 写入EDF文件
     */
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void WriteEdf()  {
        int i, err,
                sf1=250, // 通道1的采样频率
                edfsignals = 1; //通道数

        EDFwriter hdl;
        try
        {
            hdl = new EDFwriter(getTimeRecord()+".bdf", EDFwriter.EDFLIB_FILETYPE_BDFPLUS, edfsignals,EchartsActivity.this);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }
        catch(EDFException e)
        {
            e.printStackTrace();
            return;
        }

        //设置信号的最大物理值
        hdl.setPhysicalMaximum(0, 3000);
        //设置信号的最小物理值
        hdl.setPhysicalMinimum(0, -3000);
        //设置信号的最大数字值
        hdl.setDigitalMaximum(0, 32767);
        //设置信号的最小数字值
        hdl.setDigitalMinimum(0, -32768);
        //设置信号的物理单位
        hdl.setPhysicalDimension(0, String.format("uV"));

        //设置采样频率
        hdl.setSampleFrequency(0, sf1);

        //设置信号标签
        hdl.setSignalLabel(0, String.format("sine 250Hz", 0 + 1));

        int total = 0;
        int totalCount = tagDataQueue.size();
        int T = edfDataList.size();
        try
        {
            for(i=0; i<T; i++)
            {
                err = hdl.writeDigitalSamples(edfDataList.get(i));
                if(err != 0)
                {
                    System.out.printf("writePhysicalSamples() returned error: %d\n", err);
                    return;
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }
        /**
         * 开头写入标签
         */
        hdl.writeAnnotation(0, -1, "Recording starts");
        /**
         * 写入中间标签
         */
        while (!tagDataQueue.isEmpty()){
            total++;
            if (tagDataQueue.poll().isRecord()){
                Log.d(TAG, "有记录");
                hdl.writeAnnotation(calculateTime(total), -1, "Recording");
            }
        }
        /**
         * 结尾写入标签
         */
        hdl.writeAnnotation(calculateTime(total), -1, "Recording ends");

        try
        {
            hdl.close();
            //Toast.makeText(EchartsActivity.this, "导出EDF文件成功", Toast.LENGTH_SHORT).show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }
        catch(EDFException e)
        {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 计算当前点的时间 -- 用于记录标签
     * @param total
     * @return
     */
    private long calculateTime(int total){
        float totalTime = (float) total/250 * 10000;
        return (long)totalTime;
    }

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
//            Log.d(TAG, "onActivityResult: "+data.getData());
//            FileDownload fileDownload = new FileDownload(EchartsActivity.this,dataList);
//            fileDownload.saveToUri(data.getData());
        }
        /**
         * 文件导入结果回调
         */
        else if (requestCode == IMPORT_CODE && resultCode == RESULT_OK){
            //进度条对话框
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("正在导入");
            progressDialog.setMessage("Loading...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.setIcon(R.drawable.ic_launcher_background);
            progressDialog.show();

//            Log.d("是否选取到文件:", "onActivityResult: ");
            Uri uri = data.getData();
//            Log.d(TAG, "文件的uri: "+uri);
            InputStream inputStream = null;
            StringBuilder stringBuilder = new StringBuilder();
            try {
                inputStream = getContentResolver().openInputStream(uri);

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = reader.readLine()) != null){
                    stringBuilder.append(line);
                }
                progressDialog.cancel();
//                Log.d(TAG, "读取到的数据: "+stringBuilder);
                DataSolution(stringBuilder.toString());
                Toast.makeText(EchartsActivity.this, "读取成功", Toast.LENGTH_SHORT).show();

                //read_text.setText(stringBuilder);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据解析
     */
    private void DataSolution(String dataStr){
        String[] dataArr = dataStr.split(" ");
//        Log.d(TAG, "DataSolution: "+ Arrays.toString(dataArr));
        for (int i = 0; i < dataArr.length; i++){
            dataQueue.add(Integer.parseInt(dataArr[i]));
        }
    }

    /**
     * 需要写入时的EDF数据解析
     */
    private void EdfDataSolution(){
//        String[] dataArr = dataStr.split(" ");
//        Log.d("EdfDataSolution", "解析后的数据: "+ Arrays.toString(dataArr));
        double T = Math.ceil ((double) deserialization.size()/250); //向上取整
        edfDataList = new LinkedList<>();
        //初始化将要写入edf数据的容器
        for (int i = 0; i < T; i++){
             int[] buf = new int[250];
             edfDataList.add(buf);
        }
        int fuck = 0;
        //遍历写入数据
        for (int i = 0;i < edfDataList.size();i++){
            for (int j =0;j < edfDataList.get(i).length; j++ ){
                fuck ++;
                if (!deserialization.isEmpty()){
                    edfDataList.get(i)[j] = deserialization.poll().getDataPoint();
                }else{
                    break;
                }
            }
        }
        Log.d(TAG, "写入EDF时的数据量: "+fuck);
    }

    /**
     * 读取缓冲区线程 --数据反序列化
     */
    class readCacheDataThread implements Runnable{

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            int RealityCount = 0;
            try {
                FileInputStream fileInputStream = openFileInput(cache_filename);
                MyObjectInputStream objectInputStream = new MyObjectInputStream(fileInputStream);
                List<EchartsData> echartsDataList = null;
                while (fileInputStream.available()>0){
                    //Log.d(TAG, echartsData.toString());
                    echartsDataList = (List<EchartsData>)objectInputStream.readObject();
                    for (EchartsData echartsData : echartsDataList){
                        deserialization.add(echartsData);
                        tagDataQueue.add(echartsData);
//                        if (echartsData.isRecord()){
//                            Log.d(TAG, "是否有记录: "+echartsData.isRecord());
//                        }echartsDataList = {ArrayList@13425}  size = 5
//                        dataList.add(echartsData.getDataPoint());
                        RealityCount++;
                    }
                }
                Log.d(TAG, "实际存储的数据量: "+RealityCount);
                fileInputStream.close();
                objectInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "实际容器长度: "+tagDataQueue.size());
            //调试用，检测数据是否丢失
//
//            while (!deserialization.isEmpty()){
//                dataList.add(deserialization.poll().getDataPoint());
//            }
//
//            FileDownload fileDownload = new FileDownload(cache_filename,EchartsActivity.this,dataList);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                fileDownload.saveEchartsData();
//            }


//            while (!tagDataQueue.isEmpty()){
//                Log.d(TAG, "是否有记录: "+tagDataQueue.poll().getDataPoint());
//            }

            EdfDataSolution(); //edf数据解析
            WriteEdf(); //导出为EDF文件
            deleteFile(cache_filename);//删除缓冲区文件

        }
    }



    /**
     * 读取缓冲区数据 --即数据反序列化
     */
    private void readCacheData(){
        try {
            FileInputStream fileInputStream = openFileInput(cache_filename);
            MyObjectInputStream objectInputStream = new MyObjectInputStream(fileInputStream);
            List<EchartsData> echartsDataList = null;
            while (fileInputStream.available()>0){
                //Log.d(TAG, echartsData.toString());
                echartsDataList = (List<EchartsData>)objectInputStream.readObject();
                for (EchartsData echartsData : echartsDataList){
                    deserialization.add(echartsData);
                    tagDataQueue.add(echartsData);
                }
            }
            Log.d(TAG, "实际存储的数据量: "+tagDataQueue.size());
            fileInputStream.close();
            objectInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

//        try {
//            StringBuilder stringBuilder = new StringBuilder();
//            FileInputStream inputStream = openFileInput(cache_filename);
//            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//            BufferedReader reader = new BufferedReader(inputStreamReader);
//            String line;
//            while((line = reader.readLine()) != null){
//                stringBuilder.append(line);
//            }
//            EdfDataSolution(stringBuilder.toString());
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }


    /**
     * 文件导入
     */
    private void ImportFile(){
        Uri uri = MediaStore.Files.getContentUri("external");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        startActivityIfNeeded(intent,IMPORT_CODE);
    }


    /**
     * 渲染导入的数据
     */
    class ChartUpdateThread implements Runnable{
        @Override
        public void run() {
            while (isRead){
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //lineChartUtil.addEntry();
                if (!dataQueue.isEmpty()){
                    List<Integer> dataPoints = new ArrayList<>();
                    for (int i = 0; i<10; i++){
                        dataPoints.add(dataQueue.poll());
                    }
                    lineChartUtil.UpdateData(dataPoints);
                }

            }

        }
    }
    public String getTimeRecord(){
        return new SimpleDateFormat("HH_mm_ss_SS").format(new Date().getTime());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}