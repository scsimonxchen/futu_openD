package com.futu.opend.data.collector.util;

import com.futu.openapi.pb.QotCommon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SymbolParser {
    private SymbolParser() {
    }

    public static QotCommon.Security parse(String symbol) {
        String[] parts = symbol.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid symbol '" + symbol + "'. Use MARKET:CODE (e.g. HK:00700)");
        }
        return toSecurity(parts[0].trim().toUpperCase(Locale.ROOT), parts[1].trim());
    }

    public static List<QotCommon.Security> parseList(String symbolsCsv) {
        if (symbolsCsv == null || symbolsCsv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(symbolsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SymbolParser::parse)
                .collect(Collectors.toList());
    }

    public static List<QotCommon.Security> parseFile(Path symbolsFile) throws IOException {
        List<String> lines = Files.readAllLines(symbolsFile);
        List<QotCommon.Security> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            result.add(parse(trimmed));
        }
        return result;
    }

    public static QotCommon.QotMarket parseMarket(String market) {
        switch (market.toUpperCase(Locale.ROOT)) {
            case "HK":
                return QotCommon.QotMarket.QotMarket_HK_Security;
            case "US":
                return QotCommon.QotMarket.QotMarket_US_Security;
            case "SH":
                return QotCommon.QotMarket.QotMarket_CNSH_Security;
            case "SZ":
                return QotCommon.QotMarket.QotMarket_CNSZ_Security;
            case "SG":
                return QotCommon.QotMarket.QotMarket_SG_Security;
            case "JP":
                return QotCommon.QotMarket.QotMarket_JP_Security;
            case "MY":
                return QotCommon.QotMarket.QotMarket_MY_Security;
            default:
                throw new IllegalArgumentException("Unknown market: " + market);
        }
    }

    public static String marketName(int marketValue) {
        QotCommon.QotMarket market = QotCommon.QotMarket.forNumber(marketValue);
        if (market == null) {
            return String.valueOf(marketValue);
        }
        switch (market) {
            case QotMarket_HK_Security:
                return "HK";
            case QotMarket_US_Security:
                return "US";
            case QotMarket_CNSH_Security:
                return "SH";
            case QotMarket_CNSZ_Security:
                return "SZ";
            case QotMarket_SG_Security:
                return "SG";
            case QotMarket_JP_Security:
                return "JP";
            case QotMarket_MY_Security:
                return "MY";
            default:
                return market.name();
        }
    }

    public static String format(QotCommon.Security sec) {
        return marketName(sec.getMarket()) + ":" + sec.getCode();
    }

    private static QotCommon.Security toSecurity(String market, String code) {
        return QotCommon.Security.newBuilder()
                .setMarket(parseMarket(market).getNumber())
                .setCode(code)
                .build();
    }
}
