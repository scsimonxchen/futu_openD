package com.futu.opend.data.collector.cli;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotSub;
import com.futu.opend.data.collector.client.FutuQuoteClient;
import com.futu.opend.data.collector.util.SubTypeParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "unsubscribe", description = "Unsubscribe from real-time quote pushes")
public class UnsubscribeCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--symbols"}, description = "Comma-separated MARKET:CODE symbols")
    String symbols;

    @Option(names = {"--symbols-file"}, description = "File with one MARKET:CODE per line")
    String symbolsFile;

    @Option(names = {"--types"}, defaultValue = "basic,rt",
            description = "Subscription types to cancel: basic, rt, orderbook, kl_1m, ticker, broker")
    String types;

    @Option(names = {"--all"}, description = "Cancel all subscriptions")
    boolean all;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            if (all) {
                QotSub.Response rsp = client.unsubscribeAll();
                FutuQuoteClient.checkSuccess(rsp);
                System.out.println("Unsubscribed all.");
                return;
            }
            List<QotCommon.Security> secList = CommandSupport.resolveSymbols(symbols, symbolsFile);
            List<QotCommon.SubType> subTypes = SubTypeParser.parse(types);
            QotSub.Response rsp = client.subscribe(secList, subTypes, false, false);
            FutuQuoteClient.checkSuccess(rsp);
            System.out.printf("Unsubscribed %d symbol(s) for types: %s%n", secList.size(), types);
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
