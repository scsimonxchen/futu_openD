package com.stocklab.collector.cli;

import com.stocklab.collector.service.QuoteHistoricalSyncService;
import com.stocklab.collector.service.QuoteStorageService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Set;

@Command(name = "quote-sync", description = "Sync historical quote data to MySQL for analysis")
public class QuoteSyncCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--symbols"}, description = "Comma-separated MARKET:CODE symbols")
    String symbols;

    @Option(names = {"--symbols-file"}, description = "File with one MARKET:CODE per line")
    String symbolsFile;

    @Option(names = {"--from"}, description = "Begin date (default: 1 year ago)")
    String from;

    @Option(names = {"--to"}, description = "End date (default: today)")
    String to;

    @Option(names = {"--interval"}, defaultValue = "day", description = "K-line interval for kline sync")
    String interval;

    @Option(names = {"--rehab"}, defaultValue = "forward", description = "Rehab type: forward, none, backward")
    String rehab;

    @Option(names = {"--apis"}, defaultValue = "all",
            description = "API groups: all, kline, rehab, capital-flow, financials, research, valuation, " +
                    "corporate-actions, shareholders, insider, company, derivatives, market-wide, ...")
    String apis;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            List<com.futu.openapi.pb.QotCommon.Security> secList =
                    CommandSupport.resolveSymbols(symbols, symbolsFile);
            Set<String> apiGroups = QuoteHistoricalSyncService.parseApiGroups(apis);
            QuoteStorageService storage = new QuoteStorageService(store);
            QuoteHistoricalSyncService sync = new QuoteHistoricalSyncService(client, storage, config);
            sync.sync(secList, apiGroups, from, to, interval, rehab);
            System.out.println("Quote sync completed.");
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
