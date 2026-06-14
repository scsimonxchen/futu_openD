package com.futu.opend.data.collector.storage;

import com.google.protobuf.GeneratedMessageV3;

public final class QuoteResponseSerializer {
    private QuoteResponseSerializer() {
    }

    public static String toJson(GeneratedMessageV3 message) {
        return message.toString();
    }
}
