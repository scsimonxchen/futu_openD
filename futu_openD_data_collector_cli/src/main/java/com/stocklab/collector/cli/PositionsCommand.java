package com.stocklab.collector.cli;

import com.futu.openapi.pb.TrdGetPositionList;
import com.stocklab.collector.client.FutuQuoteClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "positions", description = "Fetch account positions")
public class PositionsCommand implements Runnable {
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
            TrdGetPositionList.Response rsp = client.getPositionList(accountId, trdMarket, trdEnv, false);
            FutuQuoteClient.checkSuccess(rsp);
            store.savePositions(rsp, accountId);
            System.out.printf("Saved %d position(s) for account %d%n",
                    rsp.getS2C().getPositionListCount(), accountId);
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
