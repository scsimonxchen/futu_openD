package com.stocklab.collector.cli;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.stocklab.collector.client.FutuQuoteClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

@Command(name = "snapshot", description = "Fetch market snapshots for symbols")
public class SnapshotCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--symbols"}, description = "Comma-separated MARKET:CODE symbols")
    String symbols;

    @Option(names = {"--symbols-file"}, description = "File with one MARKET:CODE per line")
    String symbolsFile;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            List<QotCommon.Security> all = CommandSupport.resolveSymbols(symbols, symbolsFile);
            List<QotGetSecuritySnapshot.Snapshot> collected = new ArrayList<>();
            for (int i = 0; i < all.size(); i += 200) {
                int end = Math.min(i + 200, all.size());
                List<QotCommon.Security> batch = all.subList(i, end);
                QotGetSecuritySnapshot.Response rsp = client.getSecuritySnapshot(batch);
                FutuQuoteClient.checkSuccess(rsp);
                collected.addAll(rsp.getS2C().getSnapshotListList());
                if (end < all.size()) {
                    Thread.sleep(3000);
                }
            }
            store.saveSnapshots(collected);
            System.out.printf("Saved %d snapshot(s)%n", collected.size());
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
