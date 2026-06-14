package com.futu.opend.data.collector.cli;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotSub;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.opend.data.collector.client.FutuQuoteClient;
import com.futu.opend.data.collector.client.PushListener;
import com.futu.opend.data.collector.util.SubTypeParser;
import com.futu.opend.data.collector.util.SymbolParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Command(name = "orderbook", description = "Fetch order book snapshot; optionally watch live updates")
public class OrderBookCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--symbol"}, required = true, description = "MARKET:CODE symbol")
    String symbol;

    @Option(names = {"--depth"}, defaultValue = "10", description = "Number of order book levels")
    int depth;

    @Option(names = {"--watch"}, description = "Subscribe and watch live order book updates until duration elapses")
    boolean watch;

    @Option(names = {"--duration"}, defaultValue = "60", description = "Watch duration in seconds (with --watch)")
    int durationSeconds;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            QotCommon.Security sec = SymbolParser.parse(symbol);

            if (watch) {
                List<QotCommon.SubType> subTypes = SubTypeParser.orderBookTypes();
                QotSub.Response subRsp = client.subscribe(Collections.singletonList(sec), subTypes, true, true);
                FutuQuoteClient.checkSuccess(subRsp);

                AtomicBoolean running = new AtomicBoolean(true);
                CountDownLatch latch = new CountDownLatch(1);
                client.setPushListener(new PushListener() {
                    @Override
                    public void onUpdateBasicQuote(com.futu.openapi.pb.QotUpdateBasicQot.Response rsp) {
                    }

                    @Override
                    public void onUpdateKL(com.futu.openapi.pb.QotUpdateKL.Response rsp) {
                    }

                    @Override
                    public void onUpdateRT(com.futu.openapi.pb.QotUpdateRT.Response rsp) {
                    }

                    @Override
                    public void onUpdateOrderBook(QotUpdateOrderBook.Response rsp) {
                        if (running.get()) {
                            store.saveOrderBook(sec, rsp);
                            System.out.printf("orderbook update %s%n", symbol);
                        }
                    }
                });

                Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
                System.out.printf("Watching order book for %s (%d seconds). Press Ctrl+C to stop.%n",
                        symbol, durationSeconds);
                latch.await(durationSeconds, TimeUnit.SECONDS);
                running.set(false);
            } else {
                QotSub.Response subRsp = client.subscribe(Collections.singletonList(sec),
                        SubTypeParser.orderBookTypes(), true, false);
                FutuQuoteClient.checkSuccess(subRsp);
                QotGetOrderBook.Response rsp = client.getOrderBook(sec, depth);
                FutuQuoteClient.checkSuccess(rsp);
                QotUpdateOrderBook.Response push = QotUpdateOrderBook.Response.newBuilder()
                        .setS2C(QotUpdateOrderBook.S2C.newBuilder()
                                .setSecurity(sec)
                                .addAllOrderBookAskList(rsp.getS2C().getOrderBookAskListList())
                                .addAllOrderBookBidList(rsp.getS2C().getOrderBookBidListList())
                                .build())
                        .build();
                store.saveOrderBook(sec, push);
                System.out.printf("Saved order book for %s (%d ask / %d bid levels)%n", symbol,
                        rsp.getS2C().getOrderBookAskListCount(), rsp.getS2C().getOrderBookBidListCount());
            }
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
