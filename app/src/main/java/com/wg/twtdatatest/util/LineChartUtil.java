package com.wg.twtdatatest.util;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
import java.util.concurrent.TimeUnit;

public class LineChartUtil {
    private LineChart lineChart;
    private List<Entry> list = new ArrayList<>();
    private int count = 0;

    public LineChartUtil(LineChart lineChart) {
        this.lineChart = lineChart;
        Setting();
        SetXAxis();
        SetYAxis();
        InitData();
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

        //折线图例 标签 设置 这里不显示图例
        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);
    }

    private void SetXAxis(){
        //绘制X轴
        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setAxisMaximum(1000);
//        xAxis.setAxisMinimum(0);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#d8d8d8"));
        //设置最后和第一个标签不超出x轴
        xAxis.setAvoidFirstLastClipping(true);
//        设置线的宽度
        xAxis.setAxisLineWidth(1.0f);
        xAxis.setAxisLineColor(Color.parseColor("#d5d5d5"));
        xAxis.setLabelCount(6,true);
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
        yAxis.setAxisMaximum(30000);
        yAxis.setAxisMinimum(-30000);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void InitData(){
        for (int i = 0;i < 1000;i++ ){
            Entry entry = new Entry(count++,0);
            list.add(entry);
        }

        LineDataSet dataSet = new LineDataSet(list,"linechart");
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setColor(Color.BLUE);
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
    }



    public void UpdateData(UiEchartsData uiEchartsData){
        UiEchartsData data = uiEchartsData;

        List<EchartsData> dataList = data.getListPacket();
        for (EchartsData echartsData : dataList){
            Log.d("LineChartUtil", "UpdateData: "+echartsData.getDataPoint());
            Entry entry = new Entry(count++,echartsData.getDataPoint());
            list.remove(0);
            list.add(entry);
        }
        LineDataSet dataSet = new LineDataSet(list,"linechart");
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setColor(Color.BLUE);
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

    }
}
