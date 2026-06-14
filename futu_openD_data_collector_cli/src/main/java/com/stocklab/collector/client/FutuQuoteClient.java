package com.stocklab.collector.client;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Qot;
import com.futu.openapi.ProtoID;
import com.futu.openapi.pb.Common;
import com.futu.openapi.pb.GetGlobalState;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.futu.openapi.pb.QotGetBroker;
import com.futu.openapi.pb.QotGetCapitalDistribution;
import com.futu.openapi.pb.QotGetCapitalFlow;
import com.futu.openapi.pb.QotGetCompanyExecutiveBackground;
import com.futu.openapi.pb.QotGetCompanyExecutives;
import com.futu.openapi.pb.QotGetCompanyOperationalEfficiency;
import com.futu.openapi.pb.QotGetCompanyProfile;
import com.futu.openapi.pb.QotGetCorporateActionsBuybacks;
import com.futu.openapi.pb.QotGetCorporateActionsDividends;
import com.futu.openapi.pb.QotGetCorporateActionsStockSplits;
import com.futu.openapi.pb.QotGetDailyShortVolume;
import com.futu.openapi.pb.QotGetFinancialsEarningsPriceHistory;
import com.futu.openapi.pb.QotGetFinancialsEarningsPriceMove;
import com.futu.openapi.pb.QotGetFinancialsRevenueBreakdown;
import com.futu.openapi.pb.QotGetFinancialsStatements;
import com.futu.openapi.pb.QotGetFutureInfo;
import com.futu.openapi.pb.QotGetInsiderHolderList;
import com.futu.openapi.pb.QotGetInsiderTradeList;
import com.futu.openapi.pb.QotGetIpoList;
import com.futu.openapi.pb.QotGetKL;
import com.futu.openapi.pb.QotGetMarketState;
import com.futu.openapi.pb.QotGetOptionChain;
import com.futu.openapi.pb.QotGetOptionExerciseProbability;
import com.futu.openapi.pb.QotGetOptionExpirationDate;
import com.futu.openapi.pb.QotGetOptionQuote;
import com.futu.openapi.pb.QotGetOptionStrategy;
import com.futu.openapi.pb.QotGetOptionStrategyAnalysis;
import com.futu.openapi.pb.QotGetOptionStrategySpread;
import com.futu.openapi.pb.QotGetOptionVolatility;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotGetOwnerPlate;
import com.futu.openapi.pb.QotGetPlateSecurity;
import com.futu.openapi.pb.QotGetPlateSet;
import com.futu.openapi.pb.QotGetPriceReminder;
import com.futu.openapi.pb.QotGetRT;
import com.futu.openapi.pb.QotGetReference;
import com.futu.openapi.pb.QotGetResearchAnalystConsensus;
import com.futu.openapi.pb.QotGetResearchMorningstarReport;
import com.futu.openapi.pb.QotGetResearchRatingSummary;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotGetShareholdersHolderDetail;
import com.futu.openapi.pb.QotGetShareholdersHoldingChanges;
import com.futu.openapi.pb.QotGetShareholdersInstitutional;
import com.futu.openapi.pb.QotGetShareholdersOverview;
import com.futu.openapi.pb.QotGetShortInterest;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotGetSubInfo;
import com.futu.openapi.pb.QotGetTicker;
import com.futu.openapi.pb.QotGetTopTenBuySellBrokers;
import com.futu.openapi.pb.QotGetUserSecurity;
import com.futu.openapi.pb.QotGetUserSecurityGroup;
import com.futu.openapi.pb.QotGetValuationDetail;
import com.futu.openapi.pb.QotGetValuationPlateStockList;
import com.futu.openapi.pb.QotGetWarrant;
import com.futu.openapi.pb.QotModifyUserSecurity;
import com.futu.openapi.pb.QotOptionScreen;
import com.futu.openapi.pb.QotRequestHistoryKL;
import com.futu.openapi.pb.QotRequestHistoryKLQuota;
import com.futu.openapi.pb.QotRequestRehab;
import com.futu.openapi.pb.QotRequestTradeDate;
import com.futu.openapi.pb.QotSetPriceReminder;
import com.futu.openapi.pb.QotStockFilter;
import com.futu.openapi.pb.QotStockScreen;
import com.futu.openapi.pb.QotSub;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateBroker;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdatePriceReminder;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.QotUpdateTicker;
import com.futu.openapi.pb.QotWarrantScreen;
import com.futu.openapi.pb.SkillWrapAPI;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.stocklab.collector.config.AppConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class FutuQuoteClient implements FTSPI_Conn, FTSPI_Qot {
    private static final long DEFAULT_TIMEOUT_MS = 30_000;

    private final Object qotLock = new Object();
    private final FTAPI_Conn_Qot qot = new FTAPI_Conn_Qot();
    private final HashMap<Integer, ReqInfo> qotReqInfoMap = new HashMap<>();
    private ConnStatus qotConnStatus = ConnStatus.DISCONNECT;
    private PushListener pushListener;

    public void setPushListener(PushListener pushListener) {
        this.pushListener = pushListener;
    }

    public boolean connect(AppConfig config) throws InterruptedException {
        return connect(config, false);
    }

    public boolean connect(AppConfig config, boolean enableEncrypt) throws InterruptedException {
        qot.setClientInfo("stock-lab-collector", 1);
        qot.setConnSpi(this);
        qot.setQotSpi(this);

        if (enableEncrypt && config.getRsaKeyFile() != null && !config.getRsaKeyFile().isEmpty()) {
            try {
                byte[] buf = Files.readAllBytes(Paths.get(config.getRsaKeyFile()));
                qot.setRSAPrivateKey(new String(buf, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read RSA key: " + config.getRsaKeyFile(), e);
            }
        }

        synchronized (qotLock) {
            boolean ret = qot.initConnect(config.getOpendHost(), (short) config.getOpendPort(), enableEncrypt);
            if (!ret) {
                return false;
            }
            qotLock.wait(DEFAULT_TIMEOUT_MS);
            return qotConnStatus == ConnStatus.READY;
        }
    }

    public void close() {
        qot.close();
        synchronized (qotLock) {
            qotConnStatus = ConnStatus.DISCONNECT;
        }
    }

    public GetGlobalState.Response getGlobalState(long userId) throws InterruptedException {
        GetGlobalState.C2S c2s = GetGlobalState.C2S.newBuilder().setUserID(userId).build();
        GetGlobalState.Request req = GetGlobalState.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.GETGLOBALSTATE, sn -> qot.getGlobalState(req), GetGlobalState.Response.class);
    }

    public QotGetSecuritySnapshot.Response getSecuritySnapshot(List<QotCommon.Security> secList)
            throws InterruptedException {
        QotGetSecuritySnapshot.C2S c2s = QotGetSecuritySnapshot.C2S.newBuilder()
                .addAllSecurityList(secList)
                .build();
        QotGetSecuritySnapshot.Request req = QotGetSecuritySnapshot.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSECURITYSNAPSHOT, sn -> qot.getSecuritySnapshot(req),
                QotGetSecuritySnapshot.Response.class);
    }

    public QotGetStaticInfo.Response getStaticInfo(QotGetStaticInfo.C2S c2s) throws InterruptedException {
        QotGetStaticInfo.Request req = QotGetStaticInfo.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSTATICINFO, sn -> qot.getStaticInfo(req),
                QotGetStaticInfo.Response.class);
    }

    public QotSub.Response subscribe(List<QotCommon.Security> secList,
                                     List<QotCommon.SubType> subTypes,
                                     boolean isSub,
                                     boolean isRegPush) throws InterruptedException {
        QotSub.C2S c2s = QotSub.C2S.newBuilder()
                .addAllSecurityList(secList)
                .addAllSubTypeList(subTypes.stream().mapToInt(QotCommon.SubType::getNumber).boxed().collect(Collectors.toList()))
                .setIsSubOrUnSub(isSub)
                .setIsRegOrUnRegPush(isRegPush)
                .setIsFirstPush(true)
                .build();
        QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_SUB, sn -> qot.sub(req), QotSub.Response.class);
    }

    public QotGetOrderBook.Response getOrderBook(QotCommon.Security sec, int num) throws InterruptedException {
        QotGetOrderBook.C2S c2s = QotGetOrderBook.C2S.newBuilder()
                .setSecurity(sec)
                .setNum(num)
                .build();
        QotGetOrderBook.Request req = QotGetOrderBook.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETORDERBOOK, sn -> qot.getOrderBook(req), QotGetOrderBook.Response.class);
    }

    public QotRequestHistoryKL.Response requestHistoryKL(QotCommon.Security sec,
                                                         QotCommon.KLType klType,
                                                         QotCommon.RehabType rehabType,
                                                         String beginTime,
                                                         String endTime,
                                                         Integer count,
                                                         byte[] nextReqKey,
                                                         boolean extendedTime) throws InterruptedException {
        QotRequestHistoryKL.C2S.Builder c2s = QotRequestHistoryKL.C2S.newBuilder()
                .setSecurity(sec)
                .setKlType(klType.getNumber())
                .setRehabType(rehabType.getNumber())
                .setBeginTime(beginTime)
                .setEndTime(endTime)
                .setExtendedTime(extendedTime);
        if (count != null) {
            c2s.setMaxAckKLNum(count);
        }
        if (nextReqKey != null && nextReqKey.length > 0) {
            c2s.setNextReqKey(ByteString.copyFrom(nextReqKey));
        }
        QotRequestHistoryKL.Request req = QotRequestHistoryKL.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_REQUESTHISTORYKL, sn -> qot.requestHistoryKL(req),
                QotRequestHistoryKL.Response.class);
    }



    public QotGetSubInfo.Response getSubInfo(QotGetSubInfo.C2S c2s) throws InterruptedException {
        QotGetSubInfo.Request req = QotGetSubInfo.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSUBINFO, sn -> qot.getSubInfo(req), QotGetSubInfo.Response.class);
    }
    public QotGetTicker.Response getTicker(QotGetTicker.C2S c2s) throws InterruptedException {
        QotGetTicker.Request req = QotGetTicker.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETTICKER, sn -> qot.getTicker(req), QotGetTicker.Response.class);
    }
    public QotGetBasicQot.Response getBasicQot(QotGetBasicQot.C2S c2s) throws InterruptedException {
        QotGetBasicQot.Request req = QotGetBasicQot.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETBASICQOT, sn -> qot.getBasicQot(req), QotGetBasicQot.Response.class);
    }
    public QotGetKL.Response getKL(QotGetKL.C2S c2s) throws InterruptedException {
        QotGetKL.Request req = QotGetKL.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETKL, sn -> qot.getKL(req), QotGetKL.Response.class);
    }
    public QotGetRT.Response getRT(QotGetRT.C2S c2s) throws InterruptedException {
        QotGetRT.Request req = QotGetRT.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETRT, sn -> qot.getRT(req), QotGetRT.Response.class);
    }
    public QotGetBroker.Response getBroker(QotGetBroker.C2S c2s) throws InterruptedException {
        QotGetBroker.Request req = QotGetBroker.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETBROKER, sn -> qot.getBroker(req), QotGetBroker.Response.class);
    }
    public QotRequestRehab.Response requestRehab(QotRequestRehab.C2S c2s) throws InterruptedException {
        QotRequestRehab.Request req = QotRequestRehab.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_REQUESTREHAB, sn -> qot.requestRehab(req), QotRequestRehab.Response.class);
    }
    public QotRequestHistoryKLQuota.Response requestHistoryKLQuota(QotRequestHistoryKLQuota.C2S c2s) throws InterruptedException {
        QotRequestHistoryKLQuota.Request req = QotRequestHistoryKLQuota.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_REQUESTHISTORYKLQUOTA, sn -> qot.requestHistoryKLQuota(req), QotRequestHistoryKLQuota.Response.class);
    }
    public QotGetPlateSet.Response getPlateSet(QotGetPlateSet.C2S c2s) throws InterruptedException {
        QotGetPlateSet.Request req = QotGetPlateSet.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETPLATESET, sn -> qot.getPlateSet(req), QotGetPlateSet.Response.class);
    }
    public QotGetPlateSecurity.Response getPlateSecurity(QotGetPlateSecurity.C2S c2s) throws InterruptedException {
        QotGetPlateSecurity.Request req = QotGetPlateSecurity.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETPLATESECURITY, sn -> qot.getPlateSecurity(req), QotGetPlateSecurity.Response.class);
    }
    public QotGetReference.Response getReference(QotGetReference.C2S c2s) throws InterruptedException {
        QotGetReference.Request req = QotGetReference.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETREFERENCE, sn -> qot.getReference(req), QotGetReference.Response.class);
    }
    public QotGetOwnerPlate.Response getOwnerPlate(QotGetOwnerPlate.C2S c2s) throws InterruptedException {
        QotGetOwnerPlate.Request req = QotGetOwnerPlate.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOWNERPLATE, sn -> qot.getOwnerPlate(req), QotGetOwnerPlate.Response.class);
    }
    public QotGetOptionChain.Response getOptionChain(QotGetOptionChain.C2S c2s) throws InterruptedException {
        QotGetOptionChain.Request req = QotGetOptionChain.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONCHAIN, sn -> qot.getOptionChain(req), QotGetOptionChain.Response.class);
    }
    public QotGetOptionExpirationDate.Response getOptionExpirationDate(QotGetOptionExpirationDate.C2S c2s) throws InterruptedException {
        QotGetOptionExpirationDate.Request req = QotGetOptionExpirationDate.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONEXPIRATIONDATE, sn -> qot.getOptionExpirationDate(req), QotGetOptionExpirationDate.Response.class);
    }
    public QotGetWarrant.Response getWarrant(QotGetWarrant.C2S c2s) throws InterruptedException {
        QotGetWarrant.Request req = QotGetWarrant.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETWARRANT, sn -> qot.getWarrant(req), QotGetWarrant.Response.class);
    }
    public QotGetCapitalFlow.Response getCapitalFlow(QotGetCapitalFlow.C2S c2s) throws InterruptedException {
        QotGetCapitalFlow.Request req = QotGetCapitalFlow.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCAPITALFLOW, sn -> qot.getCapitalFlow(req), QotGetCapitalFlow.Response.class);
    }
    public QotGetCapitalDistribution.Response getCapitalDistribution(QotGetCapitalDistribution.C2S c2s) throws InterruptedException {
        QotGetCapitalDistribution.Request req = QotGetCapitalDistribution.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCAPITALDISTRIBUTION, sn -> qot.getCapitalDistribution(req), QotGetCapitalDistribution.Response.class);
    }
    public QotGetUserSecurity.Response getUserSecurity(QotGetUserSecurity.C2S c2s) throws InterruptedException {
        QotGetUserSecurity.Request req = QotGetUserSecurity.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETUSERSECURITY, sn -> qot.getUserSecurity(req), QotGetUserSecurity.Response.class);
    }
    public QotModifyUserSecurity.Response modifyUserSecurity(QotModifyUserSecurity.C2S c2s) throws InterruptedException {
        QotModifyUserSecurity.Request req = QotModifyUserSecurity.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_MODIFYUSERSECURITY, sn -> qot.modifyUserSecurity(req), QotModifyUserSecurity.Response.class);
    }
    public QotGetUserSecurityGroup.Response getUserSecurityGroup(QotGetUserSecurityGroup.C2S c2s) throws InterruptedException {
        QotGetUserSecurityGroup.Request req = QotGetUserSecurityGroup.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETUSERSECURITYGROUP, sn -> qot.getUserSecurityGroup(req), QotGetUserSecurityGroup.Response.class);
    }
    public QotStockFilter.Response stockFilter(QotStockFilter.C2S c2s) throws InterruptedException {
        QotStockFilter.Request req = QotStockFilter.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_STOCKFILTER, sn -> qot.stockFilter(req), QotStockFilter.Response.class);
    }
    public QotGetIpoList.Response getIpoList(QotGetIpoList.C2S c2s) throws InterruptedException {
        QotGetIpoList.Request req = QotGetIpoList.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETIPOLIST, sn -> qot.getIpoList(req), QotGetIpoList.Response.class);
    }
    public QotGetFutureInfo.Response getFutureInfo(QotGetFutureInfo.C2S c2s) throws InterruptedException {
        QotGetFutureInfo.Request req = QotGetFutureInfo.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETFUTUREINFO, sn -> qot.getFutureInfo(req), QotGetFutureInfo.Response.class);
    }
    public QotRequestTradeDate.Response requestTradeDate(QotRequestTradeDate.C2S c2s) throws InterruptedException {
        QotRequestTradeDate.Request req = QotRequestTradeDate.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_REQUESTTRADEDATE, sn -> qot.requestTradeDate(req), QotRequestTradeDate.Response.class);
    }
    public QotSetPriceReminder.Response setPriceReminder(QotSetPriceReminder.C2S c2s) throws InterruptedException {
        QotSetPriceReminder.Request req = QotSetPriceReminder.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_SETPRICEREMINDER, sn -> qot.setPriceReminder(req), QotSetPriceReminder.Response.class);
    }
    public QotGetPriceReminder.Response getPriceReminder(QotGetPriceReminder.C2S c2s) throws InterruptedException {
        QotGetPriceReminder.Request req = QotGetPriceReminder.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETPRICEREMINDER, sn -> qot.getPriceReminder(req), QotGetPriceReminder.Response.class);
    }
    public QotGetMarketState.Response getMarketState(QotGetMarketState.C2S c2s) throws InterruptedException {
        QotGetMarketState.Request req = QotGetMarketState.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETMARKETSTATE, sn -> qot.getMarketState(req), QotGetMarketState.Response.class);
    }
    public QotGetFinancialsEarningsPriceMove.Response getFinancialsEarningsPriceMove(QotGetFinancialsEarningsPriceMove.C2S c2s) throws InterruptedException {
        QotGetFinancialsEarningsPriceMove.Request req = QotGetFinancialsEarningsPriceMove.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETFINANCIALSEARNINGSPRICEMOVE, sn -> qot.getFinancialsEarningsPriceMove(req), QotGetFinancialsEarningsPriceMove.Response.class);
    }
    public QotGetFinancialsEarningsPriceHistory.Response getFinancialsEarningsPriceHistory(QotGetFinancialsEarningsPriceHistory.C2S c2s) throws InterruptedException {
        QotGetFinancialsEarningsPriceHistory.Request req = QotGetFinancialsEarningsPriceHistory.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETFINANCIALSEARNINGSPRICEHISTORY, sn -> qot.getFinancialsEarningsPriceHistory(req), QotGetFinancialsEarningsPriceHistory.Response.class);
    }
    public QotGetFinancialsStatements.Response getFinancialsStatements(QotGetFinancialsStatements.C2S c2s) throws InterruptedException {
        QotGetFinancialsStatements.Request req = QotGetFinancialsStatements.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETFINANCIALSSTATEMENTS, sn -> qot.getFinancialsStatements(req), QotGetFinancialsStatements.Response.class);
    }
    public QotGetFinancialsRevenueBreakdown.Response getFinancialsRevenueBreakdown(QotGetFinancialsRevenueBreakdown.C2S c2s) throws InterruptedException {
        QotGetFinancialsRevenueBreakdown.Request req = QotGetFinancialsRevenueBreakdown.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETFINANCIALSREVENUEBREAKDOWN, sn -> qot.getFinancialsRevenueBreakdown(req), QotGetFinancialsRevenueBreakdown.Response.class);
    }
    public QotGetResearchAnalystConsensus.Response getResearchAnalystConsensus(QotGetResearchAnalystConsensus.C2S c2s) throws InterruptedException {
        QotGetResearchAnalystConsensus.Request req = QotGetResearchAnalystConsensus.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETRESEARCHANALYSTCONSENSUS, sn -> qot.getResearchAnalystConsensus(req), QotGetResearchAnalystConsensus.Response.class);
    }
    public QotGetResearchRatingSummary.Response getResearchRatingSummary(QotGetResearchRatingSummary.C2S c2s) throws InterruptedException {
        QotGetResearchRatingSummary.Request req = QotGetResearchRatingSummary.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETRESEARCHRATINGSUMMARY, sn -> qot.getResearchRatingSummary(req), QotGetResearchRatingSummary.Response.class);
    }
    public QotGetResearchMorningstarReport.Response getResearchMorningstarReport(QotGetResearchMorningstarReport.C2S c2s) throws InterruptedException {
        QotGetResearchMorningstarReport.Request req = QotGetResearchMorningstarReport.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETRESEARCHMORNINGSTARREPORT, sn -> qot.getResearchMorningstarReport(req), QotGetResearchMorningstarReport.Response.class);
    }
    public QotGetValuationDetail.Response getValuationDetail(QotGetValuationDetail.C2S c2s) throws InterruptedException {
        QotGetValuationDetail.Request req = QotGetValuationDetail.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETVALUATIONDETAIL, sn -> qot.getValuationDetail(req), QotGetValuationDetail.Response.class);
    }
    public QotGetValuationPlateStockList.Response getValuationPlateStockList(QotGetValuationPlateStockList.C2S c2s) throws InterruptedException {
        QotGetValuationPlateStockList.Request req = QotGetValuationPlateStockList.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETVALUATIONPLATESTOCKLIST, sn -> qot.getValuationPlateStockList(req), QotGetValuationPlateStockList.Response.class);
    }
    public QotGetCorporateActionsDividends.Response getCorporateActionsDividends(QotGetCorporateActionsDividends.C2S c2s) throws InterruptedException {
        QotGetCorporateActionsDividends.Request req = QotGetCorporateActionsDividends.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCORPORATEACTIONSDIVIDENDS, sn -> qot.getCorporateActionsDividends(req), QotGetCorporateActionsDividends.Response.class);
    }
    public QotGetCorporateActionsBuybacks.Response getCorporateActionsBuybacks(QotGetCorporateActionsBuybacks.C2S c2s) throws InterruptedException {
        QotGetCorporateActionsBuybacks.Request req = QotGetCorporateActionsBuybacks.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCORPORATEACTIONSBUYBACKS, sn -> qot.getCorporateActionsBuybacks(req), QotGetCorporateActionsBuybacks.Response.class);
    }
    public QotGetCorporateActionsStockSplits.Response getCorporateActionsStockSplits(QotGetCorporateActionsStockSplits.C2S c2s) throws InterruptedException {
        QotGetCorporateActionsStockSplits.Request req = QotGetCorporateActionsStockSplits.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCORPORATEACTIONSSTOCKSPLITS, sn -> qot.getCorporateActionsStockSplits(req), QotGetCorporateActionsStockSplits.Response.class);
    }
    public QotGetShareholdersOverview.Response getShareholdersOverview(QotGetShareholdersOverview.C2S c2s) throws InterruptedException {
        QotGetShareholdersOverview.Request req = QotGetShareholdersOverview.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSHAREHOLDERSOVERVIEW, sn -> qot.getShareholdersOverview(req), QotGetShareholdersOverview.Response.class);
    }
    public QotGetShareholdersHoldingChanges.Response getShareholdersHoldingChanges(QotGetShareholdersHoldingChanges.C2S c2s) throws InterruptedException {
        QotGetShareholdersHoldingChanges.Request req = QotGetShareholdersHoldingChanges.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSHAREHOLDERSHOLDINGCHANGES, sn -> qot.getShareholdersHoldingChanges(req), QotGetShareholdersHoldingChanges.Response.class);
    }
    public QotGetShareholdersHolderDetail.Response getShareholdersHolderDetail(QotGetShareholdersHolderDetail.C2S c2s) throws InterruptedException {
        QotGetShareholdersHolderDetail.Request req = QotGetShareholdersHolderDetail.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSHAREHOLDERSHOLDERDETAIL, sn -> qot.getShareholdersHolderDetail(req), QotGetShareholdersHolderDetail.Response.class);
    }
    public QotGetShareholdersInstitutional.Response getShareholdersInstitutional(QotGetShareholdersInstitutional.C2S c2s) throws InterruptedException {
        QotGetShareholdersInstitutional.Request req = QotGetShareholdersInstitutional.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSHAREHOLDERSINSTITUTIONAL, sn -> qot.getShareholdersInstitutional(req), QotGetShareholdersInstitutional.Response.class);
    }
    public QotGetInsiderHolderList.Response getInsiderHolderList(QotGetInsiderHolderList.C2S c2s) throws InterruptedException {
        QotGetInsiderHolderList.Request req = QotGetInsiderHolderList.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETINSIDERHOLDERLIST, sn -> qot.getInsiderHolderList(req), QotGetInsiderHolderList.Response.class);
    }
    public QotGetInsiderTradeList.Response getInsiderTradeList(QotGetInsiderTradeList.C2S c2s) throws InterruptedException {
        QotGetInsiderTradeList.Request req = QotGetInsiderTradeList.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETINSIDERTRADELIST, sn -> qot.getInsiderTradeList(req), QotGetInsiderTradeList.Response.class);
    }
    public QotGetCompanyProfile.Response getCompanyProfile(QotGetCompanyProfile.C2S c2s) throws InterruptedException {
        QotGetCompanyProfile.Request req = QotGetCompanyProfile.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCOMPANYPROFILE, sn -> qot.getCompanyProfile(req), QotGetCompanyProfile.Response.class);
    }
    public QotGetCompanyExecutives.Response getCompanyExecutives(QotGetCompanyExecutives.C2S c2s) throws InterruptedException {
        QotGetCompanyExecutives.Request req = QotGetCompanyExecutives.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCOMPANYEXECUTIVES, sn -> qot.getCompanyExecutives(req), QotGetCompanyExecutives.Response.class);
    }
    public QotGetCompanyExecutiveBackground.Response getCompanyExecutiveBackground(QotGetCompanyExecutiveBackground.C2S c2s) throws InterruptedException {
        QotGetCompanyExecutiveBackground.Request req = QotGetCompanyExecutiveBackground.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCOMPANYEXECUTIVEBACKGROUND, sn -> qot.getCompanyExecutiveBackground(req), QotGetCompanyExecutiveBackground.Response.class);
    }
    public QotGetCompanyOperationalEfficiency.Response getCompanyOperationalEfficiency(QotGetCompanyOperationalEfficiency.C2S c2s) throws InterruptedException {
        QotGetCompanyOperationalEfficiency.Request req = QotGetCompanyOperationalEfficiency.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETCOMPANYOPERATIONALEFFICIENCY, sn -> qot.getCompanyOperationalEfficiency(req), QotGetCompanyOperationalEfficiency.Response.class);
    }
    public QotGetTopTenBuySellBrokers.Response getTopTenBuySellBrokers(QotGetTopTenBuySellBrokers.C2S c2s) throws InterruptedException {
        QotGetTopTenBuySellBrokers.Request req = QotGetTopTenBuySellBrokers.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETTOPTENBUYSELLBROKERS, sn -> qot.getTopTenBuySellBrokers(req), QotGetTopTenBuySellBrokers.Response.class);
    }
    public QotGetDailyShortVolume.Response getDailyShortVolume(QotGetDailyShortVolume.C2S c2s) throws InterruptedException {
        QotGetDailyShortVolume.Request req = QotGetDailyShortVolume.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETDAILYSHORTVOLUME, sn -> qot.getDailyShortVolume(req), QotGetDailyShortVolume.Response.class);
    }
    public QotGetShortInterest.Response getShortInterest(QotGetShortInterest.C2S c2s) throws InterruptedException {
        QotGetShortInterest.Request req = QotGetShortInterest.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETSHORTINTEREST, sn -> qot.getShortInterest(req), QotGetShortInterest.Response.class);
    }
    public QotGetOptionVolatility.Response getOptionVolatility(QotGetOptionVolatility.C2S c2s) throws InterruptedException {
        QotGetOptionVolatility.Request req = QotGetOptionVolatility.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONVOLATILITY, sn -> qot.getOptionVolatility(req), QotGetOptionVolatility.Response.class);
    }
    public QotGetOptionExerciseProbability.Response getOptionExerciseProbability(QotGetOptionExerciseProbability.C2S c2s) throws InterruptedException {
        QotGetOptionExerciseProbability.Request req = QotGetOptionExerciseProbability.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONEXERCISEPROBABILITY, sn -> qot.getOptionExerciseProbability(req), QotGetOptionExerciseProbability.Response.class);
    }
    public QotStockScreen.Response getStockScreen(QotStockScreen.C2S c2s) throws InterruptedException {
        QotStockScreen.Request req = QotStockScreen.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_STOCKSCREEN, sn -> qot.getStockScreen(req), QotStockScreen.Response.class);
    }
    public QotOptionScreen.Response getOptionScreen(QotOptionScreen.C2S c2s) throws InterruptedException {
        QotOptionScreen.Request req = QotOptionScreen.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_OPTIONSCREEN, sn -> qot.getOptionScreen(req), QotOptionScreen.Response.class);
    }
    public QotWarrantScreen.Response getWarrantScreen(QotWarrantScreen.C2S c2s) throws InterruptedException {
        QotWarrantScreen.Request req = QotWarrantScreen.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_WARRANTSCREEN, sn -> qot.getWarrantScreen(req), QotWarrantScreen.Response.class);
    }
    public QotGetOptionQuote.Response getOptionQuote(QotGetOptionQuote.C2S c2s) throws InterruptedException {
        QotGetOptionQuote.Request req = QotGetOptionQuote.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONQUOTE, sn -> qot.getOptionQuote(req), QotGetOptionQuote.Response.class);
    }
    public QotGetOptionStrategy.Response getOptionStrategy(QotGetOptionStrategy.C2S c2s) throws InterruptedException {
        QotGetOptionStrategy.Request req = QotGetOptionStrategy.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONSTRATEGY, sn -> qot.getOptionStrategy(req), QotGetOptionStrategy.Response.class);
    }
    public QotGetOptionStrategyAnalysis.Response getOptionStrategyAnalysis(QotGetOptionStrategyAnalysis.C2S c2s) throws InterruptedException {
        QotGetOptionStrategyAnalysis.Request req = QotGetOptionStrategyAnalysis.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONSTRATEGYANALYSIS, sn -> qot.getOptionStrategyAnalysis(req), QotGetOptionStrategyAnalysis.Response.class);
    }
    public QotGetOptionStrategySpread.Response getOptionStrategySpread(QotGetOptionStrategySpread.C2S c2s) throws InterruptedException {
        QotGetOptionStrategySpread.Request req = QotGetOptionStrategySpread.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_GETOPTIONSTRATEGYSPREAD, sn -> qot.getOptionStrategySpread(req), QotGetOptionStrategySpread.Response.class);
    }
    public QotSub.Response unsubscribeAll() throws InterruptedException {
        QotSub.C2S c2s = QotSub.C2S.newBuilder()
                .setIsSubOrUnSub(false)
                .setIsUnsubAll(true)
                .setIsRegOrUnRegPush(false)
                .build();
        QotSub.Request req = QotSub.Request.newBuilder().setC2S(c2s).build();
        return sendQotRequest(ProtoID.QOT_SUB, sn -> qot.sub(req), QotSub.Response.class);
    }
    public SkillWrapAPI.TechnicalUnusualRsp getTechnicalUnusual(SkillWrapAPI.TechnicalUnusualReq req) throws InterruptedException {
        return sendQotRequest(ProtoID.QOT_GETTECHNICALUNUSUAL, sn -> qot.getTechnicalUnusual(req), SkillWrapAPI.TechnicalUnusualRsp.class);
    }
    public SkillWrapAPI.FinancialUnusualRsp getFinancialUnusual(SkillWrapAPI.FinancialUnusualReq req) throws InterruptedException {
        return sendQotRequest(ProtoID.QOT_GETFINANCIALUNUSUAL, sn -> qot.getFinancialUnusual(req), SkillWrapAPI.FinancialUnusualRsp.class);
    }
    public SkillWrapAPI.DerivativeUnusualRsp getDerivativeUnusual(SkillWrapAPI.DerivativeUnusualReq req) throws InterruptedException {
        return sendQotRequest(ProtoID.QOT_GETDERIVATIVEUNUSUAL, sn -> qot.getDerivativeUnusual(req), SkillWrapAPI.DerivativeUnusualRsp.class);
    }
    
    public static void checkSuccess(GeneratedMessageV3 rsp) {
        try {
            int retType = (int) rsp.getClass().getMethod("getRetType").invoke(rsp);
            if (retType != Common.RetType.RetType_Succeed_VALUE) {
                String msg = (String) rsp.getClass().getMethod("getRetMsg").invoke(rsp);
                throw new FutuApiException(retType, msg);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unexpected response type", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends GeneratedMessageV3> T sendQotRequest(int protoId, IntFunction<Integer> sender, Class<T> type)
            throws InterruptedException {
        ReqInfo reqInfo;
        Object syncEvent = new Object();

        synchronized (syncEvent) {
            synchronized (qotLock) {
                if (qotConnStatus != ConnStatus.READY) {
                    throw new IllegalStateException("Quote connection not ready");
                }
                int sn = sender.apply(0);
                if (sn == 0) {
                    throw new IllegalStateException("Failed to send quote request");
                }
                reqInfo = new ReqInfo(protoId, syncEvent);
                qotReqInfoMap.put(sn, reqInfo);
            }
            syncEvent.wait(DEFAULT_TIMEOUT_MS);
            if (reqInfo.rsp == null) {
                throw new IllegalStateException("Quote request timed out");
            }
            return (T) reqInfo.rsp;
        }
    }

    private void handleQotOnReply(int serialNo, int protoId, GeneratedMessageV3 rsp) {
        ReqInfo reqInfo = getQotReqInfo(serialNo, protoId);
        if (reqInfo != null) {
            synchronized (reqInfo.syncEvent) {
                reqInfo.rsp = rsp;
                reqInfo.syncEvent.notifyAll();
            }
        }
    }

    private ReqInfo getQotReqInfo(int serialNo, int protoId) {
        synchronized (qotLock) {
            ReqInfo info = qotReqInfoMap.get(serialNo);
            if (info != null && info.protoId == protoId) {
                qotReqInfoMap.remove(serialNo);
                return info;
            }
        }
        return null;
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        synchronized (qotLock) {
            if (errCode == 0) {
                qotConnStatus = ConnStatus.READY;
            }
            qotLock.notifyAll();
        }
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        synchronized (qotLock) {
            qotConnStatus = ConnStatus.DISCONNECT;
        }
    }

    @Override
    public void onReply_GetGlobalState(FTAPI_Conn client, int nSerialNo, GetGlobalState.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.GETGLOBALSTATE, rsp);
    }

    @Override
    public void onReply_GetSecuritySnapshot(FTAPI_Conn client, int nSerialNo, QotGetSecuritySnapshot.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSECURITYSNAPSHOT, rsp);
    }

    @Override
    public void onReply_GetStaticInfo(FTAPI_Conn client, int nSerialNo, QotGetStaticInfo.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSTATICINFO, rsp);
    }

    @Override
    public void onReply_Sub(FTAPI_Conn client, int nSerialNo, QotSub.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_SUB, rsp);
    }

    @Override
    public void onReply_GetOrderBook(FTAPI_Conn client, int nSerialNo, QotGetOrderBook.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETORDERBOOK, rsp);
    }

    @Override
    public void onReply_RequestHistoryKL(FTAPI_Conn client, int nSerialNo, QotRequestHistoryKL.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_REQUESTHISTORYKL, rsp);
    }

    @Override
    public void onReply_GetSubInfo(FTAPI_Conn client, int nSerialNo, QotGetSubInfo.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSUBINFO, rsp);
    }
    @Override
    public void onReply_GetTicker(FTAPI_Conn client, int nSerialNo, QotGetTicker.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETTICKER, rsp);
    }
    @Override
    public void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETBASICQOT, rsp);
    }
    @Override
    public void onReply_GetKL(FTAPI_Conn client, int nSerialNo, QotGetKL.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETKL, rsp);
    }
    @Override
    public void onReply_GetRT(FTAPI_Conn client, int nSerialNo, QotGetRT.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETRT, rsp);
    }
    @Override
    public void onReply_GetBroker(FTAPI_Conn client, int nSerialNo, QotGetBroker.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETBROKER, rsp);
    }
    @Override
    public void onReply_RequestRehab(FTAPI_Conn client, int nSerialNo, QotRequestRehab.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_REQUESTREHAB, rsp);
    }
    @Override
    public void onReply_RequestHistoryKLQuota(FTAPI_Conn client, int nSerialNo, QotRequestHistoryKLQuota.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_REQUESTHISTORYKLQUOTA, rsp);
    }
    @Override
    public void onReply_GetPlateSet(FTAPI_Conn client, int nSerialNo, QotGetPlateSet.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETPLATESET, rsp);
    }
    @Override
    public void onReply_GetPlateSecurity(FTAPI_Conn client, int nSerialNo, QotGetPlateSecurity.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETPLATESECURITY, rsp);
    }
    @Override
    public void onReply_GetReference(FTAPI_Conn client, int nSerialNo, QotGetReference.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETREFERENCE, rsp);
    }
    @Override
    public void onReply_GetOwnerPlate(FTAPI_Conn client, int nSerialNo, QotGetOwnerPlate.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOWNERPLATE, rsp);
    }
    @Override
    public void onReply_GetOptionChain(FTAPI_Conn client, int nSerialNo, QotGetOptionChain.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONCHAIN, rsp);
    }
    @Override
    public void onReply_GetOptionExpirationDate(FTAPI_Conn client, int nSerialNo, QotGetOptionExpirationDate.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONEXPIRATIONDATE, rsp);
    }
    @Override
    public void onReply_GetWarrant(FTAPI_Conn client, int nSerialNo, QotGetWarrant.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETWARRANT, rsp);
    }
    @Override
    public void onReply_GetCapitalFlow(FTAPI_Conn client, int nSerialNo, QotGetCapitalFlow.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCAPITALFLOW, rsp);
    }
    @Override
    public void onReply_GetCapitalDistribution(FTAPI_Conn client, int nSerialNo, QotGetCapitalDistribution.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCAPITALDISTRIBUTION, rsp);
    }
    @Override
    public void onReply_GetUserSecurity(FTAPI_Conn client, int nSerialNo, QotGetUserSecurity.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETUSERSECURITY, rsp);
    }
    @Override
    public void onReply_ModifyUserSecurity(FTAPI_Conn client, int nSerialNo, QotModifyUserSecurity.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_MODIFYUSERSECURITY, rsp);
    }
    @Override
    public void onReply_GetUserSecurityGroup(FTAPI_Conn client, int nSerialNo, QotGetUserSecurityGroup.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETUSERSECURITYGROUP, rsp);
    }
    @Override
    public void onReply_StockFilter(FTAPI_Conn client, int nSerialNo, QotStockFilter.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_STOCKFILTER, rsp);
    }
    @Override
    public void onReply_GetIpoList(FTAPI_Conn client, int nSerialNo, QotGetIpoList.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETIPOLIST, rsp);
    }
    @Override
    public void onReply_GetFutureInfo(FTAPI_Conn client, int nSerialNo, QotGetFutureInfo.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETFUTUREINFO, rsp);
    }
    @Override
    public void onReply_RequestTradeDate(FTAPI_Conn client, int nSerialNo, QotRequestTradeDate.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_REQUESTTRADEDATE, rsp);
    }
    @Override
    public void onReply_SetPriceReminder(FTAPI_Conn client, int nSerialNo, QotSetPriceReminder.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_SETPRICEREMINDER, rsp);
    }
    @Override
    public void onReply_GetPriceReminder(FTAPI_Conn client, int nSerialNo, QotGetPriceReminder.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETPRICEREMINDER, rsp);
    }
    @Override
    public void onReply_GetMarketState(FTAPI_Conn client, int nSerialNo, QotGetMarketState.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETMARKETSTATE, rsp);
    }
    @Override
    public void onReply_GetFinancialsEarningsPriceMove(FTAPI_Conn client, int nSerialNo, QotGetFinancialsEarningsPriceMove.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETFINANCIALSEARNINGSPRICEMOVE, rsp);
    }
    @Override
    public void onReply_GetFinancialsEarningsPriceHistory(FTAPI_Conn client, int nSerialNo, QotGetFinancialsEarningsPriceHistory.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETFINANCIALSEARNINGSPRICEHISTORY, rsp);
    }
    @Override
    public void onReply_GetFinancialsStatements(FTAPI_Conn client, int nSerialNo, QotGetFinancialsStatements.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETFINANCIALSSTATEMENTS, rsp);
    }
    @Override
    public void onReply_GetFinancialsRevenueBreakdown(FTAPI_Conn client, int nSerialNo, QotGetFinancialsRevenueBreakdown.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETFINANCIALSREVENUEBREAKDOWN, rsp);
    }
    @Override
    public void onReply_GetResearchAnalystConsensus(FTAPI_Conn client, int nSerialNo, QotGetResearchAnalystConsensus.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETRESEARCHANALYSTCONSENSUS, rsp);
    }
    @Override
    public void onReply_GetResearchRatingSummary(FTAPI_Conn client, int nSerialNo, QotGetResearchRatingSummary.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETRESEARCHRATINGSUMMARY, rsp);
    }
    @Override
    public void onReply_GetResearchMorningstarReport(FTAPI_Conn client, int nSerialNo, QotGetResearchMorningstarReport.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETRESEARCHMORNINGSTARREPORT, rsp);
    }
    @Override
    public void onReply_GetValuationDetail(FTAPI_Conn client, int nSerialNo, QotGetValuationDetail.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETVALUATIONDETAIL, rsp);
    }
    @Override
    public void onReply_GetValuationPlateStockList(FTAPI_Conn client, int nSerialNo, QotGetValuationPlateStockList.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETVALUATIONPLATESTOCKLIST, rsp);
    }
    @Override
    public void onReply_GetCorporateActionsDividends(FTAPI_Conn client, int nSerialNo, QotGetCorporateActionsDividends.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCORPORATEACTIONSDIVIDENDS, rsp);
    }
    @Override
    public void onReply_GetCorporateActionsBuybacks(FTAPI_Conn client, int nSerialNo, QotGetCorporateActionsBuybacks.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCORPORATEACTIONSBUYBACKS, rsp);
    }
    @Override
    public void onReply_GetCorporateActionsStockSplits(FTAPI_Conn client, int nSerialNo, QotGetCorporateActionsStockSplits.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCORPORATEACTIONSSTOCKSPLITS, rsp);
    }
    @Override
    public void onReply_GetShareholdersOverview(FTAPI_Conn client, int nSerialNo, QotGetShareholdersOverview.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSHAREHOLDERSOVERVIEW, rsp);
    }
    @Override
    public void onReply_GetShareholdersHoldingChanges(FTAPI_Conn client, int nSerialNo, QotGetShareholdersHoldingChanges.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSHAREHOLDERSHOLDINGCHANGES, rsp);
    }
    @Override
    public void onReply_GetShareholdersHolderDetail(FTAPI_Conn client, int nSerialNo, QotGetShareholdersHolderDetail.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSHAREHOLDERSHOLDERDETAIL, rsp);
    }
    @Override
    public void onReply_GetShareholdersInstitutional(FTAPI_Conn client, int nSerialNo, QotGetShareholdersInstitutional.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSHAREHOLDERSINSTITUTIONAL, rsp);
    }
    @Override
    public void onReply_GetInsiderHolderList(FTAPI_Conn client, int nSerialNo, QotGetInsiderHolderList.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETINSIDERHOLDERLIST, rsp);
    }
    @Override
    public void onReply_GetInsiderTradeList(FTAPI_Conn client, int nSerialNo, QotGetInsiderTradeList.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETINSIDERTRADELIST, rsp);
    }
    @Override
    public void onReply_GetCompanyProfile(FTAPI_Conn client, int nSerialNo, QotGetCompanyProfile.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCOMPANYPROFILE, rsp);
    }
    @Override
    public void onReply_GetCompanyExecutives(FTAPI_Conn client, int nSerialNo, QotGetCompanyExecutives.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCOMPANYEXECUTIVES, rsp);
    }
    @Override
    public void onReply_GetCompanyExecutiveBackground(FTAPI_Conn client, int nSerialNo, QotGetCompanyExecutiveBackground.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCOMPANYEXECUTIVEBACKGROUND, rsp);
    }
    @Override
    public void onReply_GetCompanyOperationalEfficiency(FTAPI_Conn client, int nSerialNo, QotGetCompanyOperationalEfficiency.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETCOMPANYOPERATIONALEFFICIENCY, rsp);
    }
    @Override
    public void onReply_GetTopTenBuySellBrokers(FTAPI_Conn client, int nSerialNo, QotGetTopTenBuySellBrokers.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETTOPTENBUYSELLBROKERS, rsp);
    }
    @Override
    public void onReply_GetDailyShortVolume(FTAPI_Conn client, int nSerialNo, QotGetDailyShortVolume.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETDAILYSHORTVOLUME, rsp);
    }
    @Override
    public void onReply_GetShortInterest(FTAPI_Conn client, int nSerialNo, QotGetShortInterest.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETSHORTINTEREST, rsp);
    }
    @Override
    public void onReply_GetOptionVolatility(FTAPI_Conn client, int nSerialNo, QotGetOptionVolatility.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONVOLATILITY, rsp);
    }
    @Override
    public void onReply_GetOptionExerciseProbability(FTAPI_Conn client, int nSerialNo, QotGetOptionExerciseProbability.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONEXERCISEPROBABILITY, rsp);
    }
    @Override
    public void onReply_StockScreen(FTAPI_Conn client, int nSerialNo, QotStockScreen.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_STOCKSCREEN, rsp);
    }
    @Override
    public void onReply_OptionScreen(FTAPI_Conn client, int nSerialNo, QotOptionScreen.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_OPTIONSCREEN, rsp);
    }
    @Override
    public void onReply_WarrantScreen(FTAPI_Conn client, int nSerialNo, QotWarrantScreen.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_WARRANTSCREEN, rsp);
    }
    @Override
    public void onReply_GetOptionQuote(FTAPI_Conn client, int nSerialNo, QotGetOptionQuote.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONQUOTE, rsp);
    }
    @Override
    public void onReply_GetOptionStrategy(FTAPI_Conn client, int nSerialNo, QotGetOptionStrategy.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONSTRATEGY, rsp);
    }
    @Override
    public void onReply_GetOptionStrategyAnalysis(FTAPI_Conn client, int nSerialNo, QotGetOptionStrategyAnalysis.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONSTRATEGYANALYSIS, rsp);
    }
    @Override
    public void onReply_GetOptionStrategySpread(FTAPI_Conn client, int nSerialNo, QotGetOptionStrategySpread.Response rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETOPTIONSTRATEGYSPREAD, rsp);
    }
    @Override
    public void onReply_GetTechnicalUnusual(FTAPI_Conn client, int nSerialNo, SkillWrapAPI.TechnicalUnusualRsp rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETTECHNICALUNUSUAL, rsp);
    }
    @Override
    public void onReply_GetFinancialUnusual(FTAPI_Conn client, int nSerialNo, SkillWrapAPI.FinancialUnusualRsp rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETFINANCIALUNUSUAL, rsp);
    }
    @Override
    public void onReply_GetDerivativeUnusual(FTAPI_Conn client, int nSerialNo, SkillWrapAPI.DerivativeUnusualRsp rsp) {
        handleQotOnReply(nSerialNo, ProtoID.QOT_GETDERIVATIVEUNUSUAL, rsp);
    }
    @Override
    public void onPush_UpdateBasicQuote(FTAPI_Conn client, QotUpdateBasicQot.Response rsp) {
        if (pushListener != null) {
            pushListener.onUpdateBasicQuote(rsp);
        }
    }

    @Override
    public void onPush_UpdateKL(FTAPI_Conn client, QotUpdateKL.Response rsp) {
        if (pushListener != null) {
            pushListener.onUpdateKL(rsp);
        }
    }

    @Override
    public void onPush_UpdateRT(FTAPI_Conn client, QotUpdateRT.Response rsp) {
        if (pushListener != null) {
            pushListener.onUpdateRT(rsp);
        }
    }

    @Override
    public void onPush_UpdateOrderBook(FTAPI_Conn client, QotUpdateOrderBook.Response rsp) {
        if (pushListener != null) {
            pushListener.onUpdateOrderBook(rsp);
        }
    }

    @Override
    public void onPush_UpdateTicker(FTAPI_Conn client, QotUpdateTicker.Response rsp) {
        if (pushListener != null) pushListener.onUpdateTicker(rsp);
    }
    @Override
    public void onPush_UpdateBroker(FTAPI_Conn client, QotUpdateBroker.Response rsp) {
        if (pushListener != null) pushListener.onUpdateBroker(rsp);
    }
    @Override
    public void onPush_UpdatePriceReminder(FTAPI_Conn client, QotUpdatePriceReminder.Response rsp) {
        if (pushListener != null) pushListener.onUpdatePriceReminder(rsp);
    }
}
