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
import com.futu.openapi.pb.QotGetCapitalFlow;
import com.futu.openapi.pb.QotRequestRehab;
import com.futu.opend.data.collector.util.KlTypeParser;
import com.futu.opend.data.collector.util.SymbolParser;

import java.util.List;

public class StdoutStore implements DataStore {
    @Override
    public void saveGlobalState(GetGlobalState.Response rsp) {
        System.out.println(rsp);
    }

    @Override
    public void saveSnapshots(List<QotGetSecuritySnapshot.Snapshot> snapshots) {
        for (QotGetSecuritySnapshot.Snapshot snap : snapshots) {
            QotCommon.Security sec = snap.getBasic().getSecurity();
            System.out.printf("snapshot %s:%s price=%.4f volume=%d%n",
                    SymbolParser.marketName(sec.getMarket()), sec.getCode(),
                    snap.getBasic().getCurPrice(), snap.getBasic().getVolume());
        }
    }

    @Override
    public void saveKlines(QotCommon.Security security, String interval, List<QotCommon.KLine> klines) {
        for (QotCommon.KLine kl : klines) {
            System.out.printf("kline %s:%s %s %s O=%.4f H=%.4f L=%.4f C=%.4f V=%d%n",
                    SymbolParser.marketName(security.getMarket()), security.getCode(), interval, kl.getTime(),
                    kl.getOpenPrice(), kl.getHighPrice(), kl.getLowPrice(), kl.getClosePrice(), kl.getVolume());
        }
    }

    @Override
    public void saveStaticInfo(List<QotCommon.SecurityStaticInfo> infos) {
        for (QotCommon.SecurityStaticInfo info : infos) {
            QotCommon.Security sec = info.getBasic().getSecurity();
            System.out.printf("static %s:%s name=%s lot=%d%n",
                    SymbolParser.marketName(sec.getMarket()), sec.getCode(),
                    info.getBasic().getName(), info.getBasic().getLotSize());
        }
    }

    @Override
    public void saveOrderBook(QotCommon.Security security, QotUpdateOrderBook.Response rsp) {
        System.out.println(rsp);
    }

    @Override
    public void saveRealtimeTick(QotUpdateRT.Response rsp) {
        System.out.println(rsp);
    }

    @Override
    public void saveBasicQuote(QotUpdateBasicQot.Response rsp) {
        System.out.println(rsp);
    }

    @Override
    public void saveKlinePush(QotUpdateKL.Response rsp) {
        if (rsp.hasS2C()) {
            String interval = rsp.getS2C().hasKlType()
                    ? KlTypeParser.toInterval(rsp.getS2C().getKlType())
                    : "unknown";
            saveKlines(rsp.getS2C().getSecurity(), interval, rsp.getS2C().getKlListList());
        }
    }

    @Override
    public void saveCapitalFlow(QotCommon.Security security, QotGetCapitalFlow.Response rsp) {
        System.out.printf("capital_flow %s:%s items=%d%n",
                SymbolParser.marketName(security.getMarket()), security.getCode(),
                rsp.hasS2C() ? rsp.getS2C().getFlowItemListCount() : 0);
    }

    @Override
    public void saveRehabFactors(QotCommon.Security security, QotRequestRehab.Response rsp) {
        System.out.printf("rehab_factors %s:%s items=%d%n",
                SymbolParser.marketName(security.getMarket()), security.getCode(),
                rsp.hasS2C() ? rsp.getS2C().getRehabListCount() : 0);
    }

    @Override
    public void saveAccounts(TrdGetAccList.Response rsp) {
        System.out.println(rsp);
    }

    @Override
    public void saveFunds(TrdGetFunds.Response rsp, long accId) {
        System.out.println(rsp);
    }

    @Override
    public void savePositions(TrdGetPositionList.Response rsp, long accId) {
        System.out.println(rsp);
    }

    @Override
    public void saveOrders(TrdGetOrderList.Response rsp, long accId, boolean history) {
        System.out.println(rsp);
    }

    @Override
    public void saveHistoryOrders(TrdGetHistoryOrderList.Response rsp, long accId) {
        System.out.println(rsp);
    }

    @Override
    public void close() {
    }

    @Override
    public void saveQuoteArchive(String apiName, String entityKey, com.google.protobuf.GeneratedMessageV3 response) {
        System.out.printf("[%s] %s: %s%n", apiName, entityKey, response);
    }

    @Override
    public void saveTickerPush(com.futu.openapi.pb.QotUpdateTicker.Response rsp) {
        System.out.println(rsp);
    }

    @Override
    public void saveBrokerPush(com.futu.openapi.pb.QotUpdateBroker.Response rsp) {
        System.out.println(rsp);
    }
}
