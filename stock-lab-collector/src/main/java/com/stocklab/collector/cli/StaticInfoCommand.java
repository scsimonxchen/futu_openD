package com.stocklab.collector.cli;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.util.SymbolParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

@Command(name = "static-info", description = "Fetch static security info for a market")
public class StaticInfoCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--market"}, required = true, description = "Market: HK, US, SH, SZ, SG, JP, MY")
    String market;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            QotCommon.QotMarket qotMarket = SymbolParser.parseMarket(market);
            int[] stockTypes = {
                    QotCommon.SecurityType.SecurityType_Eqty_VALUE,
                    QotCommon.SecurityType.SecurityType_Index_VALUE,
                    QotCommon.SecurityType.SecurityType_Trust_VALUE,
                    QotCommon.SecurityType.SecurityType_Warrant_VALUE,
                    QotCommon.SecurityType.SecurityType_Bond_VALUE
            };
            List<QotCommon.SecurityStaticInfo> all = new ArrayList<>();
            for (int stockType : stockTypes) {
                QotGetStaticInfo.C2S c2s = QotGetStaticInfo.C2S.newBuilder()
                        .setMarket(qotMarket.getNumber())
                        .setSecType(stockType)
                        .build();
                QotGetStaticInfo.Response rsp = client.getStaticInfo(c2s);
                FutuQuoteClient.checkSuccess(rsp);
                all.addAll(rsp.getS2C().getStaticInfoListList());
                Thread.sleep(1000);
            }
            store.saveStaticInfo(all);
            System.out.printf("Saved %d static info record(s) for market %s%n", all.size(), market);
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
