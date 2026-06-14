package com.futu.opend.data.collector.util;

import com.futu.openapi.pb.QotCommon;

import java.util.Locale;

public final class PlateTypeParser {
    private PlateTypeParser() {
    }

    public static int parse(String plateType) {
        if (plateType == null || plateType.trim().isEmpty()) {
            return QotCommon.PlateSetType.PlateSetType_All_VALUE;
        }
        switch (plateType.trim().toLowerCase(Locale.ROOT)) {
            case "all":
                return QotCommon.PlateSetType.PlateSetType_All_VALUE;
            case "industry":
                return QotCommon.PlateSetType.PlateSetType_Industry_VALUE;
            case "region":
                return QotCommon.PlateSetType.PlateSetType_Region_VALUE;
            case "concept":
                return QotCommon.PlateSetType.PlateSetType_Concept_VALUE;
            default:
                throw new IllegalArgumentException("Unknown plate type: " + plateType
                        + ". Use all, industry, region, or concept.");
        }
    }
}
