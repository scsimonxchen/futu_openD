package com.stocklab.collector.cli;

import com.futu.openapi.pb.TrdGetAccList;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.client.FutuTradeClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "accounts", description = "List trading accounts")
public class AccountsCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            FutuTradeClient trade = new FutuTradeClient();
            if (!trade.connect(config)) {
                throw new IllegalStateException("Failed to connect trade channel");
            }
            try {
                TrdGetAccList.Response rsp = trade.getAccList(config.getUserId());
                FutuQuoteClient.checkSuccess(rsp);
                store.saveAccounts(rsp);
                System.out.printf("Saved %d account(s)%n", rsp.getS2C().getAccListCount());
            } finally {
                trade.close();
            }
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
