package com.futu.opend.data.collector.client;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Trd;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Trd;
import com.futu.openapi.ProtoID;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetAccList;
import com.futu.openapi.pb.TrdGetFunds;
import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.futu.openapi.pb.TrdGetOrderList;
import com.futu.openapi.pb.TrdGetPositionList;
import com.futu.openapi.pb.TrdUnlockTrade;
import com.google.protobuf.GeneratedMessageV3;
import com.futu.opend.data.collector.config.AppConfig;

import java.util.HashMap;
import java.util.List;
import java.util.function.IntFunction;

public class FutuTradeClient implements FTSPI_Conn, FTSPI_Trd {
    private static final long DEFAULT_TIMEOUT_MS = 30_000;

    private final Object trdLock = new Object();
    private final FTAPI_Conn_Trd trd = new FTAPI_Conn_Trd();
    private final HashMap<Integer, ReqInfo> trdReqInfoMap = new HashMap<>();
    private ConnStatus trdConnStatus = ConnStatus.DISCONNECT;

    public boolean connect(AppConfig config) throws InterruptedException {
        trd.setConnSpi(this);
        trd.setTrdSpi(this);
        synchronized (trdLock) {
            boolean ret = trd.initConnect(config.getOpendHost(), (short) config.getOpendPort(), false);
            if (!ret) {
                return false;
            }
            trdLock.wait(DEFAULT_TIMEOUT_MS);
            return trdConnStatus == ConnStatus.READY;
        }
    }

    public void close() {
        trd.close();
        synchronized (trdLock) {
            trdConnStatus = ConnStatus.DISCONNECT;
        }
    }

    public TrdUnlockTrade.Response unlockTrade(AppConfig config, boolean isUnlock) throws InterruptedException {
        TrdUnlockTrade.C2S c2s = TrdUnlockTrade.C2S.newBuilder()
                .setPwdMD5(config.getUnlockTradePwdMd5())
                .setUnlock(isUnlock)
                .setSecurityFirm(config.getSecurityFirm().getNumber())
                .build();
        TrdUnlockTrade.Request req = TrdUnlockTrade.Request.newBuilder().setC2S(c2s).build();
        return sendTrdRequest(ProtoID.TRD_UNLOCKTRADE, sn -> trd.unlockTrade(req), TrdUnlockTrade.Response.class);
    }

    public TrdGetAccList.Response getAccList(long userId) throws InterruptedException {
        TrdGetAccList.C2S c2s = TrdGetAccList.C2S.newBuilder().setUserID(userId).build();
        TrdGetAccList.Request req = TrdGetAccList.Request.newBuilder().setC2S(c2s).build();
        return sendTrdRequest(ProtoID.TRD_GETACCLIST, sn -> trd.getAccList(req), TrdGetAccList.Response.class);
    }

    public TrdGetFunds.Response getFunds(long accId, TrdCommon.TrdMarket trdMarket, TrdCommon.TrdEnv trdEnv,
                                         boolean refreshCache, TrdCommon.Currency currency) throws InterruptedException {
        TrdCommon.TrdHeader header = makeTrdHeader(trdEnv, accId, trdMarket);
        TrdGetFunds.C2S c2s = TrdGetFunds.C2S.newBuilder()
                .setHeader(header)
                .setCurrency(currency.getNumber())
                .setRefreshCache(refreshCache)
                .build();
        TrdGetFunds.Request req = TrdGetFunds.Request.newBuilder().setC2S(c2s).build();
        return sendTrdRequest(ProtoID.TRD_GETFUNDS, sn -> trd.getFunds(req), TrdGetFunds.Response.class);
    }

    public TrdGetPositionList.Response getPositionList(long accId, TrdCommon.TrdMarket trdMarket,
                                                       TrdCommon.TrdEnv trdEnv,
                                                       boolean refreshCache) throws InterruptedException {
        TrdCommon.TrdHeader header = makeTrdHeader(trdEnv, accId, trdMarket);
        TrdGetPositionList.C2S c2s = TrdGetPositionList.C2S.newBuilder()
                .setHeader(header)
                .setRefreshCache(refreshCache)
                .build();
        TrdGetPositionList.Request req = TrdGetPositionList.Request.newBuilder().setC2S(c2s).build();
        return sendTrdRequest(ProtoID.TRD_GETPOSITIONLIST, sn -> trd.getPositionList(req),
                TrdGetPositionList.Response.class);
    }

    public TrdGetOrderList.Response getOrderList(long accId, TrdCommon.TrdMarket trdMarket, TrdCommon.TrdEnv trdEnv,
                                                 boolean refreshCache) throws InterruptedException {
        TrdCommon.TrdHeader header = makeTrdHeader(trdEnv, accId, trdMarket);
        TrdGetOrderList.C2S c2s = TrdGetOrderList.C2S.newBuilder()
                .setHeader(header)
                .setRefreshCache(refreshCache)
                .build();
        TrdGetOrderList.Request req = TrdGetOrderList.Request.newBuilder().setC2S(c2s).build();
        return sendTrdRequest(ProtoID.TRD_GETORDERLIST, sn -> trd.getOrderList(req), TrdGetOrderList.Response.class);
    }

    public TrdGetHistoryOrderList.Response getHistoryOrderList(long accId, TrdCommon.TrdMarket trdMarket,
                                                               TrdCommon.TrdEnv trdEnv) throws InterruptedException {
        TrdCommon.TrdHeader header = makeTrdHeader(trdEnv, accId, trdMarket);
        TrdGetHistoryOrderList.C2S c2s = TrdGetHistoryOrderList.C2S.newBuilder()
                .setHeader(header)
                .build();
        TrdGetHistoryOrderList.Request req = TrdGetHistoryOrderList.Request.newBuilder().setC2S(c2s).build();
        return sendTrdRequest(ProtoID.TRD_GETHISTORYORDERLIST, sn -> trd.getHistoryOrderList(req),
                TrdGetHistoryOrderList.Response.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends GeneratedMessageV3> T sendTrdRequest(int protoId, IntFunction<Integer> sender, Class<T> type)
            throws InterruptedException {
        ReqInfo reqInfo;
        Object syncEvent = new Object();

        synchronized (syncEvent) {
            synchronized (trdLock) {
                if (trdConnStatus != ConnStatus.READY) {
                    throw new IllegalStateException("Trade connection not ready");
                }
                int sn = sender.apply(0);
                if (sn == 0) {
                    throw new IllegalStateException("Failed to send trade request");
                }
                reqInfo = new ReqInfo(protoId, syncEvent);
                trdReqInfoMap.put(sn, reqInfo);
            }
            syncEvent.wait(DEFAULT_TIMEOUT_MS);
            if (reqInfo.rsp == null) {
                throw new IllegalStateException("Trade request timed out");
            }
            return (T) reqInfo.rsp;
        }
    }

    private void handleTrdOnReply(int serialNo, int protoId, GeneratedMessageV3 rsp) {
        ReqInfo reqInfo = getTrdReqInfo(serialNo, protoId);
        if (reqInfo != null) {
            synchronized (reqInfo.syncEvent) {
                reqInfo.rsp = rsp;
                reqInfo.syncEvent.notifyAll();
            }
        }
    }

    private ReqInfo getTrdReqInfo(int serialNo, int protoId) {
        synchronized (trdLock) {
            ReqInfo info = trdReqInfoMap.get(serialNo);
            if (info != null && info.protoId == protoId) {
                trdReqInfoMap.remove(serialNo);
                return info;
            }
        }
        return null;
    }

    private static TrdCommon.TrdHeader makeTrdHeader(TrdCommon.TrdEnv trdEnv, long accId,
                                                     TrdCommon.TrdMarket trdMarket) {
        return TrdCommon.TrdHeader.newBuilder()
                .setTrdEnv(trdEnv.getNumber())
                .setAccID(accId)
                .setTrdMarket(trdMarket.getNumber())
                .build();
    }

    @Override
    public void onInitConnect(FTAPI_Conn client, long errCode, String desc) {
        synchronized (trdLock) {
            if (errCode == 0) {
                trdConnStatus = ConnStatus.READY;
            }
            trdLock.notifyAll();
        }
    }

    @Override
    public void onDisconnect(FTAPI_Conn client, long errCode) {
        synchronized (trdLock) {
            trdConnStatus = ConnStatus.DISCONNECT;
        }
    }

    @Override
    public void onReply_UnlockTrade(FTAPI_Conn client, int nSerialNo, TrdUnlockTrade.Response rsp) {
        handleTrdOnReply(nSerialNo, ProtoID.TRD_UNLOCKTRADE, rsp);
    }

    @Override
    public void onReply_GetAccList(FTAPI_Conn client, int nSerialNo, TrdGetAccList.Response rsp) {
        handleTrdOnReply(nSerialNo, ProtoID.TRD_GETACCLIST, rsp);
    }

    @Override
    public void onReply_GetFunds(FTAPI_Conn client, int nSerialNo, TrdGetFunds.Response rsp) {
        handleTrdOnReply(nSerialNo, ProtoID.TRD_GETFUNDS, rsp);
    }

    @Override
    public void onReply_GetOrderList(FTAPI_Conn client, int nSerialNo, TrdGetOrderList.Response rsp) {
        handleTrdOnReply(nSerialNo, ProtoID.TRD_GETORDERLIST, rsp);
    }

    @Override
    public void onReply_GetHistoryOrderList(FTAPI_Conn client, int nSerialNo, TrdGetHistoryOrderList.Response rsp) {
        handleTrdOnReply(nSerialNo, ProtoID.TRD_GETHISTORYORDERLIST, rsp);
    }

    @Override
    public void onReply_GetPositionList(FTAPI_Conn client, int nSerialNo, TrdGetPositionList.Response rsp) {
        handleTrdOnReply(nSerialNo, ProtoID.TRD_GETPOSITIONLIST, rsp);
    }
}
