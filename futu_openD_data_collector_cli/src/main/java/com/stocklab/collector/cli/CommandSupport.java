package com.stocklab.collector.cli;

import com.futu.openapi.FTAPI;
import com.futu.openapi.pb.QotCommon;
import com.stocklab.collector.client.FutuApiException;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.config.AppConfig;
import com.stocklab.collector.storage.DataStore;
import com.stocklab.collector.util.SymbolParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class CommandSupport {
    private CommandSupport() {
    }

    static List<QotCommon.Security> resolveSymbols(String symbols, String symbolsFile) throws Exception {
        List<QotCommon.Security> result = new ArrayList<>();
        if (symbols != null && !symbols.trim().isEmpty()) {
            result.addAll(SymbolParser.parseList(symbols));
        }
        if (symbolsFile != null && !symbolsFile.isEmpty()) {
            result.addAll(SymbolParser.parseFile(Paths.get(symbolsFile)));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Provide --symbols and/or --symbols-file");
        }
        return result;
    }

    static int runQuote(GlobalOptions globals, QuoteAction action) {
        FTAPI.init();
        try {
            AppConfig config = globals.loadConfig();
            try (DataStore store = globals.openStore(config)) {
                FutuQuoteClient client = new FutuQuoteClient();
            if (!client.connect(config)) {
                System.err.println("Failed to connect to OpenD at " + config.getOpendHost() + ":" + config.getOpendPort());
                System.err.println("Ensure OpenD is running and logged in.");
                return 1;
            }
            try {
                action.run(client, config, store);
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
    interface QuoteAction {
        void run(FutuQuoteClient client, AppConfig config, DataStore store) throws Exception;
    }
}
