package com.futu.opend.data.collector.storage;

import com.futu.openapi.pb.GetGlobalState;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.TrdGetAccList;
import com.futu.openapi.pb.TrdGetFunds;
import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.futu.openapi.pb.TrdGetOrderList;
import com.futu.openapi.pb.TrdGetPositionList;
import com.futu.openapi.pb.QotUpdateBroker;
import com.futu.openapi.pb.QotUpdateTicker;
import com.google.protobuf.GeneratedMessageV3;
import com.futu.opend.data.collector.util.SymbolParser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvStore implements DataStore {
    private final Path outputDir;
    private final String fetchedAt = Instant.now().toString();
    private final Set<String> headersWritten = new HashSet<>();

    public CsvStore(Path outputDir) {
        this.outputDir = outputDir;
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create CSV output directory", e);
        }
    }

    @Override
    public void saveGlobalState(GetGlobalState.Response rsp) {
        writeRow("global_state.csv",
                "fetched_at,qot_logined,trd_logined,server_ver",
                String.format("%s,%s,%s,%s", fetchedAt,
                        rsp.getS2C().getQotLogined(), rsp.getS2C().getTrdLogined(), rsp.getS2C().getServerVer()));
    }

    @Override
    public void saveSnapshots(List<QotGetSecuritySnapshot.Snapshot> snapshots) {
        for (QotGetSecuritySnapshot.Snapshot snap : snapshots) {
            QotCommon.Security sec = snap.getBasic().getSecurity();
            writeRow("snapshots.csv",
                    "fetched_at,market,code,cur_price,open_price,high_price,low_price,volume,turnover",
                    String.format("%s,%s,%s,%.6f,%.6f,%.6f,%.6f,%d,%.2f",
                            fetchedAt, SymbolParser.marketName(sec.getMarket()), sec.getCode(),
                            snap.getBasic().getCurPrice(), snap.getBasic().getOpenPrice(),
                            snap.getBasic().getHighPrice(), snap.getBasic().getLowPrice(),
                            snap.getBasic().getVolume(), snap.getBasic().getTurnover()));
        }
    }

    @Override
    public void saveKlines(QotCommon.Security security, String interval, List<QotCommon.KLine> klines) {
        for (QotCommon.KLine kl : klines) {
            writeRow("klines.csv",
                    "fetched_at,market,code,interval,time_key,open,close,high,low,volume,turnover",
                    String.format("%s,%s,%s,%s,%s,%.6f,%.6f,%.6f,%.6f,%d,%.2f",
                            fetchedAt, SymbolParser.marketName(security.getMarket()), security.getCode(),
                            interval, kl.getTime(), kl.getOpenPrice(), kl.getClosePrice(),
                            kl.getHighPrice(), kl.getLowPrice(), kl.getVolume(), kl.getTurnover()));
        }
    }

    @Override
    public void saveStaticInfo(List<QotCommon.SecurityStaticInfo> infos) {
        for (QotCommon.SecurityStaticInfo info : infos) {
            QotCommon.Security sec = info.getBasic().getSecurity();
            writeRow("static_info.csv",
                    "fetched_at,market,code,name,lot_size,sec_type,list_time",
                    String.format("%s,%s,%s,%s,%d,%d,%s",
                            fetchedAt, SymbolParser.marketName(sec.getMarket()), sec.getCode(),
                            escape(info.getBasic().getName()), info.getBasic().getLotSize(),
                            info.getBasic().getSecType(), info.getBasic().getListTime()));
        }
    }

    @Override
    public void saveOrderBook(QotCommon.Security security, QotUpdateOrderBook.Response rsp) {
        writeRow("orderbook.csv", "fetched_at,market,code,payload",
                String.format("%s,%s,%s,%s", Instant.now(), SymbolParser.marketName(security.getMarket()),
                        security.getCode(), escape(rsp.toString())));
    }

    @Override
    public void saveRealtimeTick(QotUpdateRT.Response rsp) {
        writeRow("realtime_ticks.csv", "fetched_at,payload",
                String.format("%s,%s", Instant.now(), escape(rsp.toString())));
    }

    @Override
    public void saveBasicQuote(QotUpdateBasicQot.Response rsp) {
        writeRow("basic_quotes.csv", "fetched_at,payload",
                String.format("%s,%s", Instant.now(), escape(rsp.toString())));
    }

    @Override
    public void saveKlinePush(QotUpdateKL.Response rsp) {
        if (rsp.hasS2C()) {
            saveKlines(rsp.getS2C().getSecurity(), "push", rsp.getS2C().getKlListList());
        }
    }

    @Override
    public void saveAccounts(TrdGetAccList.Response rsp) {
        writeRow("accounts.csv", "fetched_at,payload",
                String.format("%s,%s", fetchedAt, escape(rsp.toString())));
    }

    @Override
    public void saveFunds(TrdGetFunds.Response rsp, long accId) {
        writeRow("funds.csv", "fetched_at,acc_id,payload",
                String.format("%s,%d,%s", fetchedAt, accId, escape(rsp.toString())));
    }

    @Override
    public void savePositions(TrdGetPositionList.Response rsp, long accId) {
        writeRow("positions.csv", "fetched_at,acc_id,payload",
                String.format("%s,%d,%s", fetchedAt, accId, escape(rsp.toString())));
    }

    @Override
    public void saveOrders(TrdGetOrderList.Response rsp, long accId, boolean history) {
        writeRow("orders.csv", "fetched_at,acc_id,is_history,payload",
                String.format("%s,%d,%d,%s", fetchedAt, accId, history ? 1 : 0, escape(rsp.toString())));
    }

    @Override
    public void saveHistoryOrders(TrdGetHistoryOrderList.Response rsp, long accId) {
        writeRow("orders.csv", "fetched_at,acc_id,is_history,payload",
                String.format("%s,%d,1,%s", fetchedAt, accId, escape(rsp.toString())));
    }

    @Override
    public void saveQuoteArchive(String apiName, String entityKey, GeneratedMessageV3 response) {
        writeRow("quote_api_archive.csv", "fetched_at,api_name,entity_key,payload",
                String.format("%s,%s,%s,%s", fetchedAt, apiName, entityKey, escape(response.toString())));
    }

    @Override
    public void saveTickerPush(QotUpdateTicker.Response rsp) {
        writeRow("rt_tickers.csv", "fetched_at,payload",
                String.format("%s,%s", Instant.now(), escape(rsp.toString())));
    }

    @Override
    public void saveBrokerPush(QotUpdateBroker.Response rsp) {
        writeRow("rt_broker_queue.csv", "fetched_at,payload",
                String.format("%s,%s", Instant.now(), escape(rsp.toString())));
    }

    private void writeRow(String fileName, String header, String row) {
        Path file = outputDir.resolve(fileName);
        try {
            boolean writeHeader = !headersWritten.contains(fileName) && !Files.exists(file);
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                if (writeHeader) {
                    writer.write(header);
                    writer.newLine();
                    headersWritten.add(fileName);
                }
                writer.write(row);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV: " + file, e);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void close() {
    }
}
