package com.futu.opend.data.collector.util;

import com.futu.openapi.pb.QotCommon;

import java.util.Locale;

public final class KlTypeParser {
    private KlTypeParser() {
    }

    public static QotCommon.KLType parse(String interval) {
        switch (interval.toLowerCase(Locale.ROOT)) {
            case "1m":
                return QotCommon.KLType.KLType_1Min;
            case "5m":
                return QotCommon.KLType.KLType_5Min;
            case "15m":
                return QotCommon.KLType.KLType_15Min;
            case "30m":
                return QotCommon.KLType.KLType_30Min;
            case "60m":
                return QotCommon.KLType.KLType_60Min;
            case "day":
            case "1d":
                return QotCommon.KLType.KLType_Day;
            case "week":
                return QotCommon.KLType.KLType_Week;
            case "month":
                return QotCommon.KLType.KLType_Month;
            default:
                throw new IllegalArgumentException("Unknown interval: " + interval);
        }
    }

    public static String toInterval(int klTypeNumber) {
        QotCommon.KLType klType = QotCommon.KLType.forNumber(klTypeNumber);
        if (klType == null) {
            return "unknown";
        }
        switch (klType) {
            case KLType_1Min:
                return "1m";
            case KLType_5Min:
                return "5m";
            case KLType_15Min:
                return "15m";
            case KLType_30Min:
                return "30m";
            case KLType_60Min:
                return "60m";
            case KLType_Day:
                return "day";
            case KLType_Week:
                return "week";
            case KLType_Month:
                return "month";
            default:
                return "unknown";
        }
    }
}
