package com.futu.opend.data.collector.storage;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetCapitalFlow;
import com.futu.openapi.pb.QotRequestRehab;
import com.google.protobuf.GeneratedMessageV3;

public final class NormalizedQuoteWriter {
    private NormalizedQuoteWriter() {
    }

    public static void writeIfSupported(DataStore store, String apiName,
                                        QotCommon.Security security,
                                        GeneratedMessageV3 response) {
        if ("get_capital_flow".equals(apiName) && response instanceof QotGetCapitalFlow.Response) {
            store.saveCapitalFlow(security, (QotGetCapitalFlow.Response) response);
        } else if ("request_rehab".equals(apiName) && response instanceof QotRequestRehab.Response) {
            store.saveRehabFactors(security, (QotRequestRehab.Response) response);
        }
    }
}
