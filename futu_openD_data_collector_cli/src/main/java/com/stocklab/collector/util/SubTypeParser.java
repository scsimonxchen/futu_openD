package com.stocklab.collector.util;

import com.futu.openapi.pb.QotCommon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SubTypeParser {
    private SubTypeParser() {
    }

    public static List<QotCommon.SubType> parse(String typesCsv) {
        if (typesCsv == null || typesCsv.trim().isEmpty()) {
            return Arrays.asList(QotCommon.SubType.SubType_Basic, QotCommon.SubType.SubType_RT);
        }
        return Arrays.stream(typesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SubTypeParser::parseOne)
                .collect(Collectors.toList());
    }

    private static QotCommon.SubType parseOne(String type) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "basic":
                return QotCommon.SubType.SubType_Basic;
            case "rt":
                return QotCommon.SubType.SubType_RT;
            case "orderbook":
                return QotCommon.SubType.SubType_OrderBook;
            case "broker":
                return QotCommon.SubType.SubType_Broker;
            case "ticker":
                return QotCommon.SubType.SubType_Ticker;
            case "kl_day":
            case "kl-day":
                return QotCommon.SubType.SubType_KL_Day;
            case "kl_1m":
            case "kl-1m":
                return QotCommon.SubType.SubType_KL_1Min;
            case "kl_5m":
            case "kl-5m":
                return QotCommon.SubType.SubType_KL_5Min;
            case "kl_15m":
            case "kl-15m":
                return QotCommon.SubType.SubType_KL_15Min;
            case "kl_30m":
            case "kl-30m":
                return QotCommon.SubType.SubType_KL_30Min;
            case "kl_60m":
            case "kl-60m":
                return QotCommon.SubType.SubType_KL_60Min;
            default:
                throw new IllegalArgumentException("Unknown subscription type: " + type);
        }
    }

    public static List<QotCommon.SubType> orderBookTypes() {
        List<QotCommon.SubType> types = new ArrayList<>();
        types.add(QotCommon.SubType.SubType_OrderBook);
        return types;
    }
}
