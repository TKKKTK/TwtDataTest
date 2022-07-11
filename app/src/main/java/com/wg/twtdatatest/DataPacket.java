package com.wg.twtdatatest;

import no.nordicsemi.android.ble.data.Data;

/**
 * 数据包实体类
 */
public class DataPacket {
    private Data data;
    private boolean isSelect;
    private String timeRecord;

    public DataPacket(Data data, String timeRecord) {
        this.data = data;
        this.timeRecord = timeRecord;
        this.isSelect = false;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public String getTimeRecord() {
        return timeRecord;
    }

    public void setTimeRecord(String timeRecord) {
        this.timeRecord = timeRecord;
    }
}
