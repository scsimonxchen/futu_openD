package com.stocklab.collector.cli;

import com.stocklab.collector.config.AppConfig;
import com.stocklab.collector.storage.DataStore;
import com.stocklab.collector.storage.OutputFormat;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GlobalOptions {
    @Option(names = {"--config"}, description = "Path to config.properties")
    String config;

    @Option(names = {"--db"}, description = "SQLite database path (default: ./data/stock_lab.db)")
    String db = "./data/stock_lab.db";

    @Option(names = {"--format"}, description = "Output format: sqlite, csv, stdout (default: sqlite)")
    String format = "sqlite";

    public AppConfig loadConfig() throws Exception {
        Path configPath = null;
        if (config != null && !config.isEmpty()) {
            configPath = Paths.get(config);
        } else {
            Path local = Paths.get("config.properties");
            if (local.toFile().exists()) {
                configPath = local;
            }
        }
        return AppConfig.load(configPath);
    }

    public DataStore openStore() {
        Path dbPath = Paths.get(db);
        return DataStore.open(dbPath, OutputFormat.fromString(format));
    }
}
