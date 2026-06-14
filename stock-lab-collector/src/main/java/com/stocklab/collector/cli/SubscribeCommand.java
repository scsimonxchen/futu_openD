package com.stocklab.collector.cli;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotSub;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.client.PushListener;
import com.stocklab.collector.storage.DataStore;
import com.stocklab.collector.util.SubTypeParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Command(name = "subscribe", description = "Subscribe to real-time quote pushes")
public class SubscribeCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--symbols"}, description = "Comma-separated MARKET:CODE symbols")
    String symbols;

    @Option(names = {"--symbols-file"}, description = "File with one MARKET:CODE per line")
    String symbolsFile;

    @Option(names = {"--types"}, defaultValue = "basic,rt,kl_1m",
            description = "Subscription types: basic, rt, orderbook, kl_1m, kl_day, ticker, broker")
    String types;

    @Option(names = {"--duration"}, defaultValue = "0",
            description = "Run duration in seconds (0 = until Ctrl+C)")
    int durationSeconds;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            List<QotCommon.Security> secList = CommandSupport.resolveSymbols(symbols, symbolsFile);
            List<QotCommon.SubType> subTypes = SubTypeParser.parse(types);

            QotSub.Response subRsp = client.subscribe(secList, subTypes, true, true);
            FutuQuoteClient.checkSuccess(subRsp);

            AtomicBoolean running = new AtomicBoolean(true);
            CountDownLatch latch = new CountDownLatch(1);
            client.setPushListener(new PushListener() {
                @Override
                public void onUpdateBasicQuote(QotUpdateBasicQot.Response rsp) {
                    if (running.get()) {
                        store.saveBasicQuote(rsp);
                    }
                }

                @Override
                public void onUpdateKL(QotUpdateKL.Response rsp) {
                    if (running.get()) {
                        store.saveKlinePush(rsp);
                    }
                }

                @Override
                public void onUpdateRT(QotUpdateRT.Response rsp) {
                    if (running.get()) {
                        store.saveRealtimeTick(rsp);
                    }
                }

                @Override
                public void onUpdateOrderBook(QotUpdateOrderBook.Response rsp) {
                    if (running.get() && rsp.hasS2C()) {
                        store.saveOrderBook(rsp.getS2C().getSecurity(), rsp);
                    }
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
            if (durationSeconds > 0) {
                System.out.printf("Subscribed to %d symbol(s). Running for %d seconds.%n",
                        secList.size(), durationSeconds);
                latch.await(durationSeconds, TimeUnit.SECONDS);
            } else {
                System.out.printf("Subscribed to %d symbol(s). Press Ctrl+C to stop.%n", secList.size());
                latch.await();
            }
            running.set(false);
            System.out.println("Subscription ended.");
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
