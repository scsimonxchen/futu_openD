package com.stocklab.collector.client;

import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;

public interface PushListener {
    void onUpdateBasicQuote(QotUpdateBasicQot.Response rsp);

    void onUpdateKL(QotUpdateKL.Response rsp);

    void onUpdateRT(QotUpdateRT.Response rsp);

    void onUpdateOrderBook(QotUpdateOrderBook.Response rsp);
}
