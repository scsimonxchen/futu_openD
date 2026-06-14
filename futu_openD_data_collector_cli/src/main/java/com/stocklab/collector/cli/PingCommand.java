package com.stocklab.collector.cli;

import com.futu.openapi.pb.GetGlobalState;
import com.stocklab.collector.client.FutuQuoteClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "ping", description = "Verify OpenD connectivity")
public class PingCommand implements Runnable {
    @Mixin
    GlobalOptions globals;

    @Override
    public void run() {
        int code = CommandSupport.runQuote(globals, (client, config, store) -> {
            GetGlobalState.Response rsp = client.getGlobalState(config.getUserId());
            FutuQuoteClient.checkSuccess(rsp);
            store.saveGlobalState(rsp);
            if (rsp.hasS2C()) {
                System.out.printf("OpenD connected. QotLogined=%s TrdLogined=%s ServerVer=%s%n",
                        rsp.getS2C().getQotLogined(), rsp.getS2C().getTrdLogined(), rsp.getS2C().getServerVer());
            } else {
                System.out.println("OpenD connected.");
            }
        });
        if (code != 0) {
            System.exit(code);
        }
    }
}
