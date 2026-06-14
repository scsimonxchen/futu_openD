package com.stocklab.collector.cli;

import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetFunds;
import com.stocklab.collector.client.FutuQuoteClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "funds", description = "Fetch account funds")
public class FundsCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--env"}, defaultValue = "simulate", description = "Trading env: simulate or real")
    String env;

    @Option(names = {"--market"}, defaultValue = "HK", description = "Trade market: HK, US, CN, SG, JP, MY")
    String market;

    @Option(names = {"--acc-id"}, description = "Account ID (default from config)")
    Long accId;

    @Override
    public void run() {
        int code = TradeCommandSupport.runTrade(globals, env, market, (client, config, store, trdEnv, trdMarket, defaultAccId) -> {
            long accountId = accId != null ? accId : defaultAccId;
            TrdGetFunds.Response rsp = client.getFunds(accountId, trdMarket, trdEnv, false,
                    TrdCommon.Currency.Currency_Unknown);
            FutuQuoteClient.checkSuccess(rsp);
            store.saveFunds(rsp, accountId);
            System.out.printf("Saved funds for account %d%n", accountId);
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
