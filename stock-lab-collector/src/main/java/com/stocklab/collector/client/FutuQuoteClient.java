package com.stocklab.collector.client;

import com.futu.openapi.FTAPI_Conn;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTSPI_Conn;
import com.futu.openapi.FTSPI_Qot;
import com.futu.openapi.ProtoID;
import com.futu.openapi.pb.Common;
import com.futu.openapi.pb.GetGlobalState;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotRequestHistoryKL;
import com.futu.openapi.pb.QotSub;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;
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
}
