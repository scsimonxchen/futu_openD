package com.stocklab.collector.client;

public class FutuApiException extends RuntimeException {
    private final int retType;

    public FutuApiException(int retType, String message) {
        super(message);
        this.retType = retType;
    }

    public int getRetType() {
        return retType;
    }
}
