package com.stocklab.collector.storage;

public enum OutputFormat {
    MYSQL,
    SQLITE,
    CSV,
    STDOUT;

    public static OutputFormat fromString(String value) {
        if (value == null || value.isEmpty()) {
            return MYSQL;
        }
        return OutputFormat.valueOf(value.toUpperCase());
    }
}
