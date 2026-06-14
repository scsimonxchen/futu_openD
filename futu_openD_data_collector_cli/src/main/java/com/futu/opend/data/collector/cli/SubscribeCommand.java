package com.futu.opend.data.collector.cli;

import com.futu.opend.data.collector.service.QuoteRealtimeService;
import com.futu.opend.data.collector.service.QuoteStorageService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.List;

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
            List<com.futu.openapi.pb.QotCommon.Security> secList =
                    CommandSupport.resolveSymbols(symbols, symbolsFile);
            QuoteStorageService storage = new QuoteStorageService(store);
            QuoteRealtimeService realtime = new QuoteRealtimeService(client, storage);
            realtime.stream(secList, types, durationSeconds, 0);
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
