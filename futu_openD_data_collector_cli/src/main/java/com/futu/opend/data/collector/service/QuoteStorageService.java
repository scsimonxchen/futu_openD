package com.futu.opend.data.collector.service;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotRequestHistoryKL;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateBroker;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.QotUpdateTicker;
import com.google.protobuf.GeneratedMessageV3;
import com.futu.opend.data.collector.client.FutuQuoteClient;
import com.futu.opend.data.collector.storage.DataStore;
import com.futu.opend.data.collector.storage.NormalizedQuoteWriter;
import com.futu.opend.data.collector.util.KlTypeParser;
import com.futu.opend.data.collector.util.SymbolParser;

import java.util.ArrayList;
import java.util.List;

public class QuoteStorageService {
    private final DataStore store;

    public QuoteStorageService(DataStore store) {
        this.store = store;
    }

    public void archive(String apiName, String entityKey, GeneratedMessageV3 response) {
        store.saveQuoteArchive(apiName, entityKey, response);
    }

    public void archiveChecked(String apiName, String entityKey, GeneratedMessageV3 response) {
        FutuQuoteClient.checkSuccess(response);
        archive(apiName, entityKey, response);
    }

    public void archiveChecked(String apiName, String entityKey,
                               QotCommon.Security security, GeneratedMessageV3 response) {
        FutuQuoteClient.checkSuccess(response);
        archive(apiName, entityKey, response);
        NormalizedQuoteWriter.writeIfSupported(store, apiName, security, response);
    }

    public void saveKlines(QotCommon.Security sec, String interval, List<QotCommon.KLine> klines) {
        store.saveKlines(sec, interval, klines);
    }

    public void saveSnapshots(List<QotGetSecuritySnapshot.Snapshot> snapshots) {
        store.saveSnapshots(snapshots);
    }

    public void savePushBasic(QotUpdateBasicQot.Response rsp) {
        store.saveBasicQuote(rsp);
    }

    public void savePushOrderBook(QotUpdateOrderBook.Response rsp) {
        if (rsp.hasS2C()) {
            store.saveOrderBook(rsp.getS2C().getSecurity(), rsp);
        }
    }

    public void savePushRt(QotUpdateRT.Response rsp) {
        store.saveRealtimeTick(rsp);
    }

    public void savePushKl(QotUpdateKL.Response rsp) {
        if (!rsp.hasS2C()) {
            return;
        }
        String interval = rsp.getS2C().hasKlType()
                ? KlTypeParser.toInterval(rsp.getS2C().getKlType())
                : "unknown";
        store.saveKlines(rsp.getS2C().getSecurity(), interval, rsp.getS2C().getKlListList());
    }

    public void savePushTicker(QotUpdateTicker.Response rsp) {
        store.saveTickerPush(rsp);
    }

    public void savePushBroker(QotUpdateBroker.Response rsp) {
        store.saveBrokerPush(rsp);
    }

    public List<QotCommon.KLine> fetchAllHistoryKlines(FutuQuoteClient client,
                                                       QotCommon.Security sec,
                                                       QotCommon.KLType klType,
                                                       QotCommon.RehabType rehabType,
                                                       String from,
                                                       String to) throws InterruptedException {
        List<QotCommon.KLine> all = new ArrayList<>();
        byte[] nextKey = new byte[0];
        do {
            QotRequestHistoryKL.Response rsp = client.requestHistoryKL(
                    sec, klType, rehabType, from, to, 1000, nextKey, false);
            FutuQuoteClient.checkSuccess(rsp);
            all.addAll(rsp.getS2C().getKlListList());
            nextKey = rsp.getS2C().getNextReqKey().toByteArray();
        } while (nextKey.length > 0);
        return all;
    }

    public static String entityKey(QotCommon.Security sec) {
        return SymbolParser.format(sec);
    }
}
