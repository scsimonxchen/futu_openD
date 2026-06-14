package com.futu.opend.data.collector.cli;

import com.futu.opend.data.collector.service.QuoteApiType;
import com.futu.opend.data.collector.service.QuoteStorageService;
import com.futu.opend.data.collector.util.SymbolParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "quote-pull", description = "Pull a single quote API and save to MySQL")
public class QuotePullCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Parameters(index = "0", description = "API name, e.g. get-market-state, get-capital-flow")
    String apiName;

    @Option(names = {"--symbol"}, description = "Single MARKET:CODE symbol")
    String symbol;

    @Option(names = {"--symbols"}, description = "Comma-separated MARKET:CODE symbols")
    String symbols;

    @Option(names = {"--symbols-file"}, description = "File with one MARKET:CODE per line")
    String symbolsFile;

    @Option(names = {"--from"}, description = "Begin date for date-ranged APIs")
    String from;

    @Option(names = {"--to"}, description = "End date for date-ranged APIs")
    String to;

    @Option(names = {"--interval"}, defaultValue = "day", description = "K-line interval for get-kl / request-history-kl")
    String interval;

    @Option(names = {"--rehab"}, defaultValue = "forward", description = "Rehab type for get-kl / request-history-kl")
    String rehab;

    @Option(names = {"--num"}, defaultValue = "50", description = "Num bars/ticks or stock-filter page size")
    int num;

    @Option(names = {"--market"}, description = "Market code: HK, US, SH, SZ, SG, JP, MY")
    String market;

    @Option(names = {"--plate-code"}, description = "Plate code for get-plate-security, e.g. HK.BK1001")
    String plateCode;

    @Option(names = {"--plate-type"}, defaultValue = "all",
            description = "Plate set type: all, industry, region, concept")
    String plateType;

    @Option(names = {"--group-name"}, description = "Watchlist group name")
    String groupName;

    @Option(names = {"--op"}, description = "Operation: add/del for modify-user-security; add/del/enable/disable for set-price-reminder")
    String op;

    @Option(names = {"--reminder-type"}, defaultValue = "0", description = "Price reminder type (set-price-reminder)")
    int reminderType;

    @Option(names = {"--value"}, defaultValue = "0", description = "Price reminder value (set-price-reminder)")
    double reminderValue;

    @Option(names = {"--key"}, defaultValue = "0", description = "Price reminder key (set-price-reminder)")
    long reminderKey;

    @Option(names = {"--note"}, description = "Price reminder note (set-price-reminder)")
    String reminderNote;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            QuoteApiType apiType = QuoteApiType.fromCliName(apiName);
            java.util.List<com.futu.openapi.pb.QotCommon.Security> symbolList = new java.util.ArrayList<>();
            if (symbols != null && !symbols.trim().isEmpty()) {
                symbolList.addAll(SymbolParser.parseList(symbols));
            }
            if (symbolsFile != null && !symbolsFile.isEmpty()) {
                symbolList.addAll(SymbolParser.parseFile(java.nio.file.Paths.get(symbolsFile)));
            }
            com.futu.openapi.pb.QotCommon.Security single = null;
            if (symbol != null && !symbol.isEmpty()) {
                single = SymbolParser.parse(symbol);
            }

            QuoteApiType.PullContext ctx = new QuoteApiType.PullContext(
                    symbolList, single, from, to, interval, rehab, num,
                    market, plateCode, plateType, groupName, op,
                    reminderType, reminderValue, reminderKey, reminderNote);
            QuoteStorageService storage = new QuoteStorageService(store);
            apiType.pull(client, storage, config, ctx);
            System.out.printf("quote-pull %s done.%n", apiName);
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
