package com.wg.twtdatatest;

import com.wg.twtdatatest.Data.UiEchartsData;

import java.util.List;
import java.util.Queue;

public interface IDataCache {
    void AddDataCache(Queue<List<UiEchartsData>> cacheData);
}
