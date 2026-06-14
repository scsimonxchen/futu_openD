package com.futu.opend.data.collector.service;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotGetTicker;
import com.futu.openapi.pb.QotSub;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateBroker;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdatePriceReminder;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.QotUpdateTicker;
import com.futu.opend.data.collector.client.FutuQuoteClient;
import com.futu.opend.data.collector.client.PushListener;
import com.futu.opend.data.collector.util.SubTypeParser;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuoteRealtimeService {
    private final FutuQuoteClient client;
    private final QuoteStorageService storage;

    public QuoteRealtimeService(FutuQuoteClient client, QuoteStorageService storage) {
        this.client = client;
        this.storage = storage;
    }

    public void stream(List<QotCommon.Security> symbols,
                       String typesCsv,
                       int durationSeconds,
                       int pullIntervalSeconds) throws InterruptedException {
        List<QotCommon.SubType> subTypes = SubTypeParser.parse(typesCsv);

        QotSub.Response subRsp = client.subscribe(symbols, subTypes, true, true);
        FutuQuoteClient.checkSuccess(subRsp);

        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        client.setPushListener(new PushListener() {
            @Override
            public void onUpdateBasicQuote(QotUpdateBasicQot.Response rsp) {
                if (running.get()) storage.savePushBasic(rsp);
            }

            @Override
            public void onUpdateKL(QotUpdateKL.Response rsp) {
                if (running.get()) storage.savePushKl(rsp);
            }

            @Override
            public void onUpdateRT(QotUpdateRT.Response rsp) {
                if (running.get()) storage.savePushRt(rsp);
            }

            @Override
            public void onUpdateOrderBook(QotUpdateOrderBook.Response rsp) {
                if (running.get()) storage.savePushOrderBook(rsp);
            }

            @Override
            public void onUpdateTicker(QotUpdateTicker.Response rsp) {
                if (running.get()) storage.savePushTicker(rsp);
            }

            @Override
            public void onUpdateBroker(QotUpdateBroker.Response rsp) {
                if (running.get()) storage.savePushBroker(rsp);
            }

            @Override
            public void onUpdatePriceReminder(QotUpdatePriceReminder.Response rsp) {
                if (running.get()) {
                    storage.archive("update_price_reminder", "global", rsp);
                }
            }
        });

        Thread pullThread = null;
        if (pullIntervalSeconds > 0) {
            pullThread = new Thread(() -> pullLoop(symbols, pullIntervalSeconds, running));
            pullThread.setDaemon(true);
            pullThread.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        if (durationSeconds > 0) {
            System.out.printf("Streaming %d symbol(s) for %d seconds.%n", symbols.size(), durationSeconds);
            latch.await(durationSeconds, TimeUnit.SECONDS);
        } else {
            System.out.printf("Streaming %d symbol(s). Press Ctrl+C to stop.%n", symbols.size());
            latch.await();
        }
        running.set(false);
        if (pullThread != null) {
            pullThread.interrupt();
        }
        System.out.println("Stream ended.");
    }

    private void pullLoop(List<QotCommon.Security> symbols, int intervalSeconds, AtomicBoolean running) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            for (QotCommon.Security sec : symbols) {
                if (!running.get()) {
                    break;
                }
                String key = QuoteStorageService.entityKey(sec);
                try {
                    QotGetBasicQot.Response basic = client.getBasicQot(
                            QotGetBasicQot.C2S.newBuilder().addSecurityList(sec).build());
                    storage.archiveChecked("get_basic_qot", key, basic);

                    QotGetOrderBook.Response book = client.getOrderBook(sec, 10);
                    storage.archiveChecked("get_order_book", key, book);

                    QotGetTicker.Response ticker = client.getTicker(
                            QotGetTicker.C2S.newBuilder().setSecurity(sec).setMaxRetNum(50).build());
                    storage.archiveChecked("get_ticker", key, ticker);
                } catch (Exception e) {
                    System.err.printf("Pull failed for %s: %s%n", key, e.getMessage());
                }
            }
            try {
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
