package com.futu.opend.data.collector.client;

import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateBroker;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdatePriceReminder;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.QotUpdateTicker;

public interface PushListener {
    void onUpdateBasicQuote(QotUpdateBasicQot.Response rsp);

    void onUpdateKL(QotUpdateKL.Response rsp);

    void onUpdateRT(QotUpdateRT.Response rsp);

    void onUpdateOrderBook(QotUpdateOrderBook.Response rsp);

    default void onUpdateTicker(QotUpdateTicker.Response rsp) {
    }

    default void onUpdateBroker(QotUpdateBroker.Response rsp) {
    }

    default void onUpdatePriceReminder(QotUpdatePriceReminder.Response rsp) {
    }
}
