package com.futu.opend.data.collector.service;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.futu.openapi.pb.QotGetBroker;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotGetRT;
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
import com.futu.opend.data.collector.config.AppConfig;
import com.futu.opend.data.collector.util.SubTypeParser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuoteRealtimeService {
    private static final long RECONNECT_BACKOFF_MS = 5_000;

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
        stream(symbols, typesCsv, durationSeconds, pullIntervalSeconds, false, null);
    }

    public void stream(List<QotCommon.Security> symbols,
                       String typesCsv,
                       int durationSeconds,
                       int pullIntervalSeconds,
                       boolean reconnect,
                       AppConfig config) throws InterruptedException {
        List<QotCommon.SubType> subTypes = SubTypeParser.parse(typesCsv);
        Set<String> subscribedTypes = parseTypeNames(typesCsv);

        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));

        client.setPushListener(createPushListener(running));
        subscribeChecked(symbols, subTypes);

        Thread pullThread = null;
        if (pullIntervalSeconds > 0) {
            pullThread = new Thread(() -> pullLoop(symbols, pullIntervalSeconds, running, subscribedTypes));
            pullThread.setDaemon(true);
            pullThread.start();
        }

        Thread monitorThread = null;
        if (reconnect && config != null) {
            monitorThread = new Thread(() -> reconnectLoop(symbols, subTypes, running, config));
            monitorThread.setDaemon(true);
            monitorThread.start();
        }

        try {
            if (durationSeconds > 0) {
                System.out.printf("Streaming %d symbol(s) for %d seconds.%n", symbols.size(), durationSeconds);
                latch.await(durationSeconds, TimeUnit.SECONDS);
            } else {
                System.out.printf("Streaming %d symbol(s). Press Ctrl+C to stop.%n", symbols.size());
                latch.await();
            }
        } finally {
            running.set(false);
            if (pullThread != null) {
                pullThread.interrupt();
            }
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
            try {
                client.unsubscribeAll();
            } catch (Exception e) {
                System.err.printf("Unsubscribe on shutdown failed: %s%n", e.getMessage());
            }
            System.out.println("Stream ended.");
        }
    }

    private PushListener createPushListener(AtomicBoolean running) {
        return new PushListener() {
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
        };
    }

    private void subscribeChecked(List<QotCommon.Security> symbols, List<QotCommon.SubType> subTypes)
            throws InterruptedException {
        QotSub.Response subRsp = client.subscribe(symbols, subTypes, true, true);
        FutuQuoteClient.checkSuccess(subRsp);
    }

    private void reconnectLoop(List<QotCommon.Security> symbols,
                               List<QotCommon.SubType> subTypes,
                               AtomicBoolean running,
                               AppConfig config) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            if (!client.isConnected()) {
                System.err.println("Quote connection lost. Reconnecting...");
                try {
                    client.close();
                    if (client.connect(config)) {
                        subscribeChecked(symbols, subTypes);
                        System.out.println("Reconnected and re-subscribed.");
                    } else {
                        System.err.println("Reconnect failed. Retrying...");
                    }
                } catch (Exception e) {
                    System.err.printf("Reconnect error: %s%n", e.getMessage());
                }
            }
            try {
                Thread.sleep(RECONNECT_BACKOFF_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void pullLoop(List<QotCommon.Security> symbols,
                          int intervalSeconds,
                          AtomicBoolean running,
                          Set<String> subscribedTypes) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            for (QotCommon.Security sec : symbols) {
                if (!running.get()) {
                    break;
                }
                String key = QuoteStorageService.entityKey(sec);
                try {
                    if (subscribedTypes.contains("basic")) {
                        QotGetBasicQot.Response basic = client.getBasicQot(
                                QotGetBasicQot.C2S.newBuilder().addSecurityList(sec).build());
                        storage.archiveChecked("get_basic_qot", key, basic);
                    }
                    if (subscribedTypes.contains("orderbook")) {
                        QotGetOrderBook.Response book = client.getOrderBook(sec, 10);
                        storage.archiveChecked("get_order_book", key, book);
                    }
                    if (subscribedTypes.contains("ticker")) {
                        QotGetTicker.Response ticker = client.getTicker(
                                QotGetTicker.C2S.newBuilder().setSecurity(sec).setMaxRetNum(50).build());
                        storage.archiveChecked("get_ticker", key, ticker);
                    }
                    if (subscribedTypes.contains("rt")) {
                        QotGetRT.Response rt = client.getRT(QotGetRT.C2S.newBuilder().setSecurity(sec).build());
                        storage.archiveChecked("get_rt", key, rt);
                    }
                    if (subscribedTypes.contains("broker")) {
                        QotGetBroker.Response broker = client.getBroker(
                                QotGetBroker.C2S.newBuilder().setSecurity(sec).build());
                        storage.archiveChecked("get_broker", key, broker);
                    }
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

    private static Set<String> parseTypeNames(String typesCsv) {
        Set<String> names = new HashSet<>();
        if (typesCsv == null) {
            return names;
        }
        for (String part : typesCsv.split(",")) {
            String name = part.trim().toLowerCase();
            if (name.startsWith("kl_") || name.startsWith("kl-")) {
                names.add("kl");
            } else if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}
