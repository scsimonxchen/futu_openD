package com.stocklab.collector.client;

import com.google.protobuf.GeneratedMessageV3;

class ReqInfo {
    final int protoId;
    final Object syncEvent;
    GeneratedMessageV3 rsp;

    ReqInfo(int protoId, Object syncEvent) {
        this.protoId = protoId;
        this.syncEvent = syncEvent;
    }
}
