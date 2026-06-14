package com.stocklab.collector.util;

import com.futu.openapi.pb.TrdCommon;

import java.util.Locale;

public final class TrdMarketParser {
    private TrdMarketParser() {
    }

    public static TrdCommon.TrdMarket parseMarket(String market) {
        switch (market.toUpperCase(Locale.ROOT)) {
            case "HK":
                return TrdCommon.TrdMarket.TrdMarket_HK;
            case "US":
                return TrdCommon.TrdMarket.TrdMarket_US;
            case "CN":
            case "SH":
            case "SZ":
                return TrdCommon.TrdMarket.TrdMarket_CN;
            case "SG":
                return TrdCommon.TrdMarket.TrdMarket_SG;
            case "JP":
                return TrdCommon.TrdMarket.TrdMarket_JP;
            case "MY":
                return TrdCommon.TrdMarket.TrdMarket_MY;
            default:
                throw new IllegalArgumentException("Unknown trade market: " + market);
        }
    }

    public static TrdCommon.TrdEnv parseEnv(String env) {
        if ("real".equalsIgnoreCase(env)) {
            return TrdCommon.TrdEnv.TrdEnv_Real;
        }
        return TrdCommon.TrdEnv.TrdEnv_Simulate;
    }
}
