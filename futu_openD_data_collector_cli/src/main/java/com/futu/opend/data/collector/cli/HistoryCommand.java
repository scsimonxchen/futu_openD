package com.futu.opend.data.collector.cli;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotRequestHistoryKL;
import com.futu.opend.data.collector.client.FutuQuoteClient;
import com.futu.opend.data.collector.util.KlTypeParser;
import com.futu.opend.data.collector.util.SymbolParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

@Command(name = "history", description = "Fetch historical K-lines")
public class HistoryCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--symbol"}, required = true, description = "MARKET:CODE symbol")
    String symbol;

    @Option(names = {"--interval"}, defaultValue = "day", description = "K-line interval: 1m, 5m, day, week, month")
    String interval;

    @Option(names = {"--from"}, required = true, description = "Begin time, e.g. 2025-01-01 or 2025-01-01 00:00:00")
    String from;

    @Option(names = {"--to"}, required = true, description = "End time, e.g. 2025-12-31 or 2025-12-31 16:00:00")
    String to;

    @Option(names = {"--rehab"}, defaultValue = "forward", description = "Rehab type: forward, none, backward")
    String rehab;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            QotCommon.Security sec = SymbolParser.parse(symbol);
            QotCommon.KLType klType = KlTypeParser.parse(interval);
            QotCommon.RehabType rehabType = parseRehab(rehab);

            List<QotCommon.KLine> allKlines = new ArrayList<>();
            byte[] nextKey = new byte[0];
            do {
                QotRequestHistoryKL.Response rsp = client.requestHistoryKL(
                        sec, klType, rehabType, from, to, 1000, nextKey, false);
                FutuQuoteClient.checkSuccess(rsp);
                allKlines.addAll(rsp.getS2C().getKlListList());
                nextKey = rsp.getS2C().getNextReqKey().toByteArray();
            } while (nextKey.length > 0);

            store.saveKlines(sec, interval, allKlines);
            System.out.printf("Saved %d K-line bar(s) for %s%n", allKlines.size(), symbol);
        });
        if (code != 0) {
            System.exit(code);
        }
    }

    private static QotCommon.RehabType parseRehab(String rehab) {
        switch (rehab.toLowerCase()) {
            case "none":
                return QotCommon.RehabType.RehabType_None;
            case "backward":
                return QotCommon.RehabType.RehabType_Backward;
            default:
                return QotCommon.RehabType.RehabType_Forward;
        }
    }
}
