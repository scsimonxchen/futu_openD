package com.stocklab.collector.storage;

public enum OutputFormat {
    SQLITE,
    CSV,
    STDOUT;

    public static OutputFormat fromString(String value) {
        if (value == null || value.isEmpty()) {
            return SQLITE;
        }
        return OutputFormat.valueOf(value.toUpperCase());
    }
}
