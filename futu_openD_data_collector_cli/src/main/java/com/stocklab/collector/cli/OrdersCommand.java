package com.stocklab.collector.cli;

import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.futu.openapi.pb.TrdGetOrderList;
import com.stocklab.collector.client.FutuQuoteClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "orders", description = "Fetch account orders")
public class OrdersCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Option(names = {"--env"}, defaultValue = "simulate", description = "Trading env: simulate or real")
    String env;

    @Option(names = {"--market"}, defaultValue = "HK", description = "Trade market: HK, US, CN, SG, JP, MY")
    String market;

    @Option(names = {"--acc-id"}, description = "Account ID (default from config)")
    Long accId;

    @Option(names = {"--history"}, description = "Fetch history orders instead of active orders")
    boolean history;

    @Override
    public void run() {
        int code = TradeCommandSupport.runTrade(globals, env, market, (client, config, store, trdEnv, trdMarket, defaultAccId) -> {
            long accountId = accId != null ? accId : defaultAccId;
            if (history) {
                TrdGetHistoryOrderList.Response rsp = client.getHistoryOrderList(accountId, trdMarket, trdEnv);
                FutuQuoteClient.checkSuccess(rsp);
                store.saveHistoryOrders(rsp, accountId);
                System.out.printf("Saved %d history order(s) for account %d%n",
                        rsp.getS2C().getOrderListCount(), accountId);
            } else {
                TrdGetOrderList.Response rsp = client.getOrderList(accountId, trdMarket, trdEnv, false);
                FutuQuoteClient.checkSuccess(rsp);
                store.saveOrders(rsp, accountId, false);
                System.out.printf("Saved %d active order(s) for account %d%n",
                        rsp.getS2C().getOrderListCount(), accountId);
            }
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
