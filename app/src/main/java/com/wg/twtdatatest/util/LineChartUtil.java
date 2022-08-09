package com.wg.twtdatatest.util;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.wg.twtdatatest.Data.EchartsData;
import com.wg.twtdatatest.Data.UiEchartsData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LineChartUtil {
    private LineChart lineChart;
    private List<Entry> dataList = new ArrayList<>();
    private List<String> XLabel = new ArrayList<>();
    private List<EchartsData> echartsDataList = new ArrayList<>();
    private LineData lineData;
    private LineDataSet lineDataSet;
    private int count = 0;

    public LineChartUtil(LineChart lineChart) {
        this.lineChart = lineChart;
        Setting();
        SetXAxis();
        SetYAxis();
        initLineDataSet("方波图",Color.BLUE);

    }

    /**
     * 图表基础设置
     */
    private void Setting(){
        lineChart.setDoubleTapToZoomEnabled(false);
        // 不显示数据描述
        lineChart.getDescription().setEnabled(false);
        // 没有数据的时候，显示“暂无数据”
        lineChart.setNoDataText("暂无数据");
        //禁止x轴y轴同时进行缩放
        lineChart.setPinchZoom(false);
        //启用/禁用缩放图表上的两个轴。
        lineChart.setScaleEnabled(false);
        //设置为false以禁止通过在其上双击缩放图表。
        lineChart.setDrawGridBackground(false);
        //显示边界
        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.parseColor("#d5d5d5"));
        lineChart.getAxisRight().setEnabled(false);//关闭右侧Y轴
        lineChart.setTouchEnabled(false);


        //折线图例 标签 设置 这里不显示图例
        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);
    }

    private void SetXAxis(){
        //绘制X轴
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setAxisMaximum(1000);
        xAxis.setAxisMinimum(0);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#d8d8d8"));
        //设置最后和第一个标签不超出x轴
        //xAxis.setAvoidFirstLastClipping(true);
//        设置线的宽度
        xAxis.setAxisLineWidth(1.0f);
        xAxis.setAxisLineColor(Color.parseColor("#d5d5d5"));
        xAxis.setLabelCount(5,true);
        xAxis.setDrawLabels(true);
//        xAxis.setValueFormatter(new ValueFormatter() {
//            private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SS", Locale.ENGLISH);
//            @Override
//            public String getFormattedValue(float value) {
//                long millis = TimeUnit.HOURS.toMillis((long) value);
//                return dateFormat.format(new Date(millis));
//            }
//        });
    }

    private void SetYAxis(){
        //绘制Y轴
        YAxis yAxis = lineChart.getAxisLeft();
//        yAxis.setAxisMaximum(30000);
//        yAxis.setAxisMinimum(-30000);
    }

    /**
     * 初始化一条折线
     */
    private void initLineDataSet(String name, int color) {

        for (int i = 0;i<1000;i++){
            Entry entry = new Entry(i,0);
            EchartsData echartsData = new EchartsData();
            echartsDataList.add(echartsData);
            dataList.add(entry);
            XLabel.add(new SimpleDateFormat("HH:mm:ss:SS").format(new Date().getTime()));
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(XLabel));

        lineDataSet = new LineDataSet(dataList, name);
        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setColor(Color.BLUE);
        //添加一个空的 LineData
        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

//        LineChartMarkView mv = new LineChartMarkView(context, xAxis.getValueFormatter());
//        mv.setChartView(lineChart);
//        lineChart.setMarker(mv);
        lineChart.invalidate();

    }

    public void UpdateData(UiEchartsData uiEchartsData){
        List<EchartsData> datas = uiEchartsData.getListPacket();
        /**
         * 移除x轴、Y轴前面的数据
         */
        for (int i = 0; i<datas.size();i++){
            echartsDataList.remove(0);
            dataList.remove(0);
            XLabel.remove(0);
        }
        for (int i = 0;i<dataList.size();i++){
            Entry entry = dataList.get(i);

            EchartsData echartsData = echartsDataList.get(i);
            if (echartsData.isRecord()){
                dataList.set(i,new Entry(i,0));
            }else {
                dataList.set(i,new Entry(i,entry.getY()));
            }

        }
        for (int i = 0; i<datas.size();i++){
            Entry entry;
               if (datas.get(i).isRecord()){
                   entry = new Entry(dataList.size(),0);
               }else {
                   entry = new Entry(dataList.size(),datas.get(i).getDataPoint());
               }
            echartsDataList.add(datas.get(i));
            dataList.add(entry);
            //更新x轴标签的数据
            XLabel.add(datas.get(i).getTime());
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(XLabel));
        //Log.d(TAG, "UpdateData: "+lineData.getDataSetCount());
        lineDataSet.setValues(dataList);
        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    public void UpdateData(List<Integer> datas){
        /**
         * 移出前面的数据
         */
        for (int i = 0; i<datas.size();i++){
            dataList.remove(0);
            XLabel.remove(0);
        }
        /**
         * 添加Y轴数据
         */
        for (int i = 0;i<dataList.size();i++){
            Entry entry = dataList.get(i);
            dataList.set(i,new Entry(i,entry.getY()));
        }
        /**
         * 添加X轴数据
         */
        for (int i = 0; i<datas.size();i++){
            Entry entry = new Entry(dataList.size(),datas.get(i));
            dataList.add(entry);
            //更新x轴标签的数据
            XLabel.add(new SimpleDateFormat("HH:mm:ss:SS").format(new Date().getTime()));
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(XLabel));

        lineDataSet.setValues(dataList);
        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        //lineChart.moveViewTo(lineData.getEntryCount() - 10,50f, YAxis.AxisDependency.LEFT);
        lineChart.notifyDataSetChanged();

        lineChart.invalidate();
    }


}
