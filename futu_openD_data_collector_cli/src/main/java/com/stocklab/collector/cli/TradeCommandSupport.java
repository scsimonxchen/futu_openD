package com.stocklab.collector.cli;

import com.futu.openapi.FTAPI;
import com.futu.openapi.pb.TrdCommon;
import com.stocklab.collector.client.FutuApiException;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.client.FutuTradeClient;
import com.stocklab.collector.config.AppConfig;
import com.stocklab.collector.storage.DataStore;

final class TradeCommandSupport {
    private TradeCommandSupport() {
    }

    static int runTrade(GlobalOptions globals, String env, String market, TradeAction action) {
        FTAPI.init();
        try {
            AppConfig config = globals.loadConfig();
            try (DataStore store = globals.openStore(config)) {
                FutuTradeClient client = new FutuTradeClient();
            if (!client.connect(config)) {
                System.err.println("Failed to connect trade channel to OpenD.");
                return 1;
            }
            try {
                if (config.getUnlockTradePwdMd5() != null && !config.getUnlockTradePwdMd5().isEmpty()) {
                    FutuQuoteClient.checkSuccess(client.unlockTrade(config, true));
                }
                TrdCommon.TrdEnv trdEnv = com.stocklab.collector.util.TrdMarketParser.parseEnv(env);
                TrdCommon.TrdMarket trdMarket = com.stocklab.collector.util.TrdMarketParser.parseMarket(market);
                long accId = config.getTrdAcc();
                action.run(client, config, store, trdEnv, trdMarket, accId);
                return 0;
            } catch (FutuApiException e) {
                System.err.printf("Futu API error (retType=%d): %s%n", e.getRetType(), e.getMessage());
                return 2;
            } finally {
                client.close();
            }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        } finally {
            FTAPI.unInit();
        }
    }

    @FunctionalInterface
    interface TradeAction {
        void run(FutuTradeClient client, AppConfig config, DataStore store,
                 TrdCommon.TrdEnv trdEnv, TrdCommon.TrdMarket trdMarket, long accId) throws Exception;
    }
}
