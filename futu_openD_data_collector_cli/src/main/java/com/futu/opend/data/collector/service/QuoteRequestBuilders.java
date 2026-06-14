package com.futu.opend.data.collector.service;

import com.futu.openapi.pb.QotCommon;
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
import com.futu.openapi.pb.QotGetInsiderHolderList;
import com.futu.openapi.pb.QotGetInsiderTradeList;
import com.futu.openapi.pb.QotGetMarketState;
import com.futu.openapi.pb.QotGetOptionChain;
import com.futu.openapi.pb.QotGetOptionExerciseProbability;
import com.futu.openapi.pb.QotGetOptionExpirationDate;
import com.futu.openapi.pb.QotGetOptionVolatility;
import com.futu.openapi.pb.QotGetOwnerPlate;
import com.futu.openapi.pb.QotGetReference;
import com.futu.openapi.pb.QotGetResearchAnalystConsensus;
import com.futu.openapi.pb.QotGetResearchMorningstarReport;
import com.futu.openapi.pb.QotGetResearchRatingSummary;
import com.futu.openapi.pb.QotGetShareholdersHolderDetail;
import com.futu.openapi.pb.QotGetShareholdersHoldingChanges;
import com.futu.openapi.pb.QotGetShareholdersInstitutional;
import com.futu.openapi.pb.QotGetShareholdersOverview;
import com.futu.openapi.pb.QotGetShortInterest;
import com.futu.openapi.pb.QotGetTopTenBuySellBrokers;
import com.futu.openapi.pb.QotGetValuationDetail;
import com.futu.openapi.pb.QotGetValuationPlateStockList;
import com.futu.openapi.pb.QotGetWarrant;
import com.futu.openapi.pb.QotRequestRehab;
import com.futu.openapi.pb.QotGetFutureInfo;
import com.futu.openapi.pb.QotGetIpoList;
import com.futu.openapi.pb.QotGetPlateSecurity;
import com.futu.openapi.pb.QotGetPlateSet;
import com.futu.openapi.pb.QotGetPriceReminder;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotGetUserSecurity;
import com.futu.openapi.pb.QotModifyUserSecurity;
import com.futu.openapi.pb.QotSetPriceReminder;
import com.futu.openapi.pb.QotStockFilter;
import com.futu.opend.data.collector.util.PlateTypeParser;
import com.futu.opend.data.collector.util.SymbolParser;

import com.futu.openapi.pb.QotRequestTradeDate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class QuoteRequestBuilders {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private QuoteRequestBuilders() {
    }

    public static QotRequestRehab.C2S rehab(QotCommon.Security sec) {
        return QotRequestRehab.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCapitalFlow.C2S capitalFlow(QotCommon.Security sec, String from, String to) {
        return QotGetCapitalFlow.C2S.newBuilder()
                .setSecurity(sec)
                .setPeriodType(QotCommon.PeriodType.PeriodType_DAY_VALUE)
                .setBeginTime(from)
                .setEndTime(to)
                .build();
    }

    public static QotGetCapitalDistribution.C2S capitalDistribution(QotCommon.Security sec) {
        return QotGetCapitalDistribution.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetOwnerPlate.C2S ownerPlate(QotCommon.Security sec) {
        return QotGetOwnerPlate.C2S.newBuilder().addSecurityList(sec).build();
    }

    public static QotGetMarketState.C2S marketState(QotCommon.Security sec) {
        return QotGetMarketState.C2S.newBuilder().addSecurityList(sec).build();
    }

    public static QotGetReference.C2S reference(QotCommon.Security sec) {
        return QotGetReference.C2S.newBuilder()
                .setSecurity(sec)
                .setReferenceType(QotGetReference.ReferenceType.ReferenceType_Warrant_VALUE)
                .build();
    }

    public static QotGetOptionExpirationDate.C2S optionExpirationDate(QotCommon.Security sec) {
        return QotGetOptionExpirationDate.C2S.newBuilder().setOwner(sec).build();
    }

    public static QotGetOptionChain.C2S optionChain(QotCommon.Security sec, String from, String to) {
        return QotGetOptionChain.C2S.newBuilder()
                .setOwner(sec)
                .setBeginTime(from)
                .setEndTime(to)
                .build();
    }

    public static QotGetWarrant.C2S warrant(QotCommon.Security sec) {
        return QotGetWarrant.C2S.newBuilder()
                .setOwner(sec)
                .setBegin(0)
                .setNum(200)
                .build();
    }

    public static QotGetOptionVolatility.C2S optionVolatility(QotCommon.Security sec) {
        return QotGetOptionVolatility.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetOptionExerciseProbability.C2S optionExerciseProbability(QotCommon.Security sec) {
        return QotGetOptionExerciseProbability.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotRequestTradeDate.C2S tradeDate(QotCommon.Security sec, String from, String to) {
        return QotRequestTradeDate.C2S.newBuilder()
                .setMarket(sec.getMarket())
                .setBeginTime(from)
                .setEndTime(to)
                .build();
    }

    public static QotGetFinancialsEarningsPriceMove.C2S financialsEarningsPriceMove(QotCommon.Security sec) {
        return QotGetFinancialsEarningsPriceMove.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetFinancialsEarningsPriceHistory.C2S financialsEarningsPriceHistory(QotCommon.Security sec) {
        return QotGetFinancialsEarningsPriceHistory.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetFinancialsStatements.C2S financialsStatements(QotCommon.Security sec) {
        return QotGetFinancialsStatements.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetFinancialsRevenueBreakdown.C2S financialsRevenueBreakdown(QotCommon.Security sec) {
        return QotGetFinancialsRevenueBreakdown.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetResearchAnalystConsensus.C2S researchAnalystConsensus(QotCommon.Security sec) {
        return QotGetResearchAnalystConsensus.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetResearchRatingSummary.C2S researchRatingSummary(QotCommon.Security sec) {
        return QotGetResearchRatingSummary.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetResearchMorningstarReport.C2S researchMorningstarReport(QotCommon.Security sec) {
        return QotGetResearchMorningstarReport.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetValuationDetail.C2S valuationDetail(QotCommon.Security sec) {
        return QotGetValuationDetail.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetValuationPlateStockList.C2S valuationPlateStockList(QotCommon.Security sec) {
        return QotGetValuationPlateStockList.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCorporateActionsDividends.C2S corporateActionsDividends(QotCommon.Security sec) {
        return QotGetCorporateActionsDividends.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCorporateActionsBuybacks.C2S corporateActionsBuybacks(QotCommon.Security sec) {
        return QotGetCorporateActionsBuybacks.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCorporateActionsStockSplits.C2S corporateActionsStockSplits(QotCommon.Security sec) {
        return QotGetCorporateActionsStockSplits.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetShareholdersOverview.C2S shareholdersOverview(QotCommon.Security sec) {
        return QotGetShareholdersOverview.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetShareholdersHoldingChanges.C2S shareholdersHoldingChanges(QotCommon.Security sec) {
        return QotGetShareholdersHoldingChanges.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetShareholdersHolderDetail.C2S shareholdersHolderDetail(QotCommon.Security sec) {
        return QotGetShareholdersHolderDetail.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetShareholdersInstitutional.C2S shareholdersInstitutional(QotCommon.Security sec) {
        return QotGetShareholdersInstitutional.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetInsiderHolderList.C2S insiderHolderList(QotCommon.Security sec) {
        return QotGetInsiderHolderList.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetInsiderTradeList.C2S insiderTradeList(QotCommon.Security sec) {
        return QotGetInsiderTradeList.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCompanyProfile.C2S companyProfile(QotCommon.Security sec) {
        return QotGetCompanyProfile.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCompanyExecutives.C2S companyExecutives(QotCommon.Security sec) {
        return QotGetCompanyExecutives.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCompanyExecutiveBackground.C2S companyExecutiveBackground(QotCommon.Security sec) {
        return QotGetCompanyExecutiveBackground.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetCompanyOperationalEfficiency.C2S companyOperationalEfficiency(QotCommon.Security sec) {
        return QotGetCompanyOperationalEfficiency.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetTopTenBuySellBrokers.C2S topTenBuySellBrokers(QotCommon.Security sec) {
        return QotGetTopTenBuySellBrokers.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetDailyShortVolume.C2S dailyShortVolume(QotCommon.Security sec) {
        return QotGetDailyShortVolume.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetShortInterest.C2S shortInterest(QotCommon.Security sec) {
        return QotGetShortInterest.C2S.newBuilder().setSecurity(sec).build();
    }

    public static QotGetPlateSet.C2S plateSet(String market, String plateType) {
        return QotGetPlateSet.C2S.newBuilder()
                .setMarket(SymbolParser.parseMarket(market).getNumber())
                .setPlateSetType(PlateTypeParser.parse(plateType))
                .build();
    }

    public static QotGetPlateSecurity.C2S plateSecurity(QotCommon.Security plate) {
        return QotGetPlateSecurity.C2S.newBuilder().setPlate(plate).build();
    }

    public static QotGetFutureInfo.C2S futureInfo(List<QotCommon.Security> securities) {
        return QotGetFutureInfo.C2S.newBuilder().addAllSecurityList(securities).build();
    }

    public static QotStockFilter.C2S stockFilter(String market, int num) {
        QotStockFilter.BaseFilter filter = QotStockFilter.BaseFilter.newBuilder()
                .setFieldName(QotStockFilter.StockField.StockField_CurPrice_VALUE)
                .setFilterMin(0.01)
                .setIsNoFilter(false)
                .build();
        return QotStockFilter.C2S.newBuilder()
                .setBegin(0)
                .setNum(num)
                .setMarket(SymbolParser.parseMarket(market).getNumber())
                .addBaseFilterList(filter)
                .build();
    }

    public static QotGetUserSecurity.C2S userSecurity(String groupName) {
        return QotGetUserSecurity.C2S.newBuilder().setGroupName(groupName).build();
    }

    public static QotModifyUserSecurity.C2S modifyUserSecurity(String groupName, String op,
                                                                 List<QotCommon.Security> securities) {
        int modifyOp = parseModifyOp(op);
        return QotModifyUserSecurity.C2S.newBuilder()
                .setGroupName(groupName)
                .setModifyOp(modifyOp)
                .addAllSecurityList(securities)
                .build();
    }

    public static QotSetPriceReminder.C2S setPriceReminder(QotCommon.Security sec, String op, int reminderType,
                                                           double value, long key, String note) {
        QotSetPriceReminder.C2S.Builder builder = QotSetPriceReminder.C2S.newBuilder()
                .setSecurity(sec)
                .setOp(parseReminderOp(op));
        if (key > 0) {
            builder.setKey(key);
        }
        if (reminderType > 0) {
            builder.setReminderType(reminderType);
        }
        if (value > 0) {
            builder.setValue(value);
        }
        if (note != null && !note.isEmpty()) {
            builder.setNote(note);
        }
        return builder.build();
    }

    public static QotGetPriceReminder.C2S priceReminder(String market) {
        return QotGetPriceReminder.C2S.newBuilder()
                .setMarket(SymbolParser.parseMarket(market).getNumber())
                .build();
    }

    public static QotGetIpoList.C2S ipoList(String market) {
        return QotGetIpoList.C2S.newBuilder()
                .setMarket(SymbolParser.parseMarket(market).getNumber())
                .build();
    }

    public static QotGetStaticInfo.C2S staticInfo(String market, int secType) {
        return QotGetStaticInfo.C2S.newBuilder()
                .setMarket(SymbolParser.parseMarket(market).getNumber())
                .setSecType(secType)
                .build();
    }

    private static int parseModifyOp(String op) {
        if (op == null) {
            throw new IllegalArgumentException("--op is required (add or del)");
        }
        switch (op.toLowerCase(Locale.ROOT)) {
            case "add":
                return QotModifyUserSecurity.ModifyUserSecurityOp.ModifyUserSecurityOp_Add_VALUE;
            case "del":
            case "delete":
                return QotModifyUserSecurity.ModifyUserSecurityOp.ModifyUserSecurityOp_Del_VALUE;
            default:
                throw new IllegalArgumentException("Unknown modify op: " + op + ". Use add or del.");
        }
    }

    private static int parseReminderOp(String op) {
        if (op == null) {
            return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_Add_VALUE;
        }
        switch (op.toLowerCase(Locale.ROOT)) {
            case "add":
                return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_Add_VALUE;
            case "del":
            case "delete":
                return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_Del_VALUE;
            case "enable":
                return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_Enable_VALUE;
            case "disable":
                return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_Disable_VALUE;
            case "modify":
                return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_Modify_VALUE;
            case "delall":
            case "del-all":
                return QotSetPriceReminder.SetPriceReminderOp.SetPriceReminderOp_DelAll_VALUE;
            default:
                throw new IllegalArgumentException("Unknown reminder op: " + op);
        }
    }

    public static String defaultFrom(String from) {
        if (from != null && !from.isEmpty()) {
            return from;
        }
        return LocalDate.now().minusYears(1).format(DATE);
    }

    public static String defaultTo(String to) {
        if (to != null && !to.isEmpty()) {
            return to;
        }
        return LocalDate.now().format(DATE);
    }

    public static List<QotCommon.Security> securities(QotCommon.Security sec) {
        return java.util.Collections.singletonList(sec);
    }
}
