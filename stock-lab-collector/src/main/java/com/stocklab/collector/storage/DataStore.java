package com.stocklab.collector.storage;

import com.futu.openapi.pb.GetGlobalState;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotRequestHistoryKL;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetAccList;
import com.futu.openapi.pb.TrdGetFunds;
import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.futu.openapi.pb.TrdGetOrderList;
import com.futu.openapi.pb.TrdGetPositionList;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;

public interface DataStore extends Closeable {
    void saveGlobalState(GetGlobalState.Response rsp);

    void saveSnapshots(List<QotGetSecuritySnapshot.Snapshot> snapshots);

    void saveKlines(QotCommon.Security security, String interval, List<QotCommon.KLine> klines);

    void saveStaticInfo(List<QotCommon.SecurityStaticInfo> infos);

    void saveOrderBook(QotCommon.Security security, QotUpdateOrderBook.Response rsp);

    void saveRealtimeTick(QotUpdateRT.Response rsp);

    void saveBasicQuote(QotUpdateBasicQot.Response rsp);

    void saveKlinePush(QotUpdateKL.Response rsp);

    void saveAccounts(TrdGetAccList.Response rsp);

    void saveFunds(TrdGetFunds.Response rsp, long accId);

    void savePositions(TrdGetPositionList.Response rsp, long accId);

    void saveOrders(TrdGetOrderList.Response rsp, long accId, boolean history);

    void saveHistoryOrders(TrdGetHistoryOrderList.Response rsp, long accId);

    static DataStore open(Path dbPath, OutputFormat format) {
        if (format == OutputFormat.STDOUT) {
            return new StdoutStore();
        }
        if (format == OutputFormat.CSV) {
            return new CsvStore(dbPath.getParent() != null ? dbPath.getParent() : dbPath);
        }
        return new SqliteStore(dbPath);
    }
}
