package com.stocklab.collector.service;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.futu.openapi.pb.QotGetBroker;
import com.futu.openapi.pb.QotGetKL;
import com.futu.openapi.pb.QotGetMarketState;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotGetRT;
import com.futu.openapi.pb.QotGetSubInfo;
import com.futu.openapi.pb.QotGetTicker;
import com.futu.openapi.pb.QotGetUserSecurityGroup;
import com.futu.openapi.pb.QotRequestHistoryKLQuota;
import com.google.protobuf.GeneratedMessageV3;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.config.AppConfig;
import com.stocklab.collector.util.KlTypeParser;

import java.util.List;
import java.util.Locale;

public enum QuoteApiType {
    GET_GLOBAL_STATE("get-global-state", Scope.GLOBAL),
    GET_SUB_INFO("get-sub-info", Scope.GLOBAL),
    REQUEST_HISTORY_KL_QUOTA("request-history-kl-quota", Scope.GLOBAL),
    GET_USER_SECURITY_GROUP("get-user-security-group", Scope.GLOBAL),

    GET_MARKET_STATE("get-market-state", Scope.SYMBOLS),
    GET_BASIC_QOT("get-basic-qot", Scope.SYMBOLS),
    GET_ORDER_BOOK("get-order-book", Scope.SYMBOL),
    GET_KL("get-kl", Scope.SYMBOL),
    GET_RT("get-rt", Scope.SYMBOL),
    GET_TICKER("get-ticker", Scope.SYMBOL),
    GET_BROKER("get-broker", Scope.SYMBOL),

    REQUEST_REHAB("request-rehab", Scope.SYMBOL),
    GET_CAPITAL_FLOW("get-capital-flow", Scope.SYMBOL),
    GET_CAPITAL_DISTRIBUTION("get-capital-distribution", Scope.SYMBOL),
    GET_OWNER_PLATE("get-owner-plate", Scope.SYMBOL),
    GET_REFERENCE("get-reference", Scope.SYMBOL),

    GET_OPTION_EXPIRATION_DATE("get-option-expiration-date", Scope.SYMBOL),
    GET_OPTION_CHAIN("get-option-chain", Scope.SYMBOL),
    GET_WARRANT("get-warrant", Scope.SYMBOL),
    GET_OPTION_VOLATILITY("get-option-volatility", Scope.SYMBOL),
    GET_OPTION_EXERCISE_PROBABILITY("get-option-exercise-probability", Scope.SYMBOL),

    GET_FINANCIALS_EARNINGS_PRICE_MOVE("get-financials-earnings-price-move", Scope.SYMBOL),
    GET_FINANCIALS_EARNINGS_PRICE_HISTORY("get-financials-earnings-price-history", Scope.SYMBOL),
    GET_FINANCIALS_STATEMENTS("get-financials-statements", Scope.SYMBOL),
    GET_FINANCIALS_REVENUE_BREAKDOWN("get-financials-revenue-breakdown", Scope.SYMBOL),

    GET_RESEARCH_ANALYST_CONSENSUS("get-research-analyst-consensus", Scope.SYMBOL),
    GET_RESEARCH_RATING_SUMMARY("get-research-rating-summary", Scope.SYMBOL),
    GET_RESEARCH_MORNINGSTAR_REPORT("get-research-morningstar-report", Scope.SYMBOL),

    GET_VALUATION_DETAIL("get-valuation-detail", Scope.SYMBOL),
    GET_VALUATION_PLATE_STOCK_LIST("get-valuation-plate-stock-list", Scope.SYMBOL),

    GET_CORPORATE_ACTIONS_DIVIDENDS("get-corporate-actions-dividends", Scope.SYMBOL),
    GET_CORPORATE_ACTIONS_BUYBACKS("get-corporate-actions-buybacks", Scope.SYMBOL),
    GET_CORPORATE_ACTIONS_STOCK_SPLITS("get-corporate-actions-stock-splits", Scope.SYMBOL),

    GET_SHAREHOLDERS_OVERVIEW("get-shareholders-overview", Scope.SYMBOL),
    GET_SHAREHOLDERS_HOLDING_CHANGES("get-shareholders-holding-changes", Scope.SYMBOL),
    GET_SHAREHOLDERS_HOLDER_DETAIL("get-shareholders-holder-detail", Scope.SYMBOL),
    GET_SHAREHOLDERS_INSTITUTIONAL("get-shareholders-institutional", Scope.SYMBOL),

    GET_INSIDER_HOLDER_LIST("get-insider-holder-list", Scope.SYMBOL),
    GET_INSIDER_TRADE_LIST("get-insider-trade-list", Scope.SYMBOL),

    GET_COMPANY_PROFILE("get-company-profile", Scope.SYMBOL),
    GET_COMPANY_EXECUTIVES("get-company-executives", Scope.SYMBOL),
    GET_COMPANY_EXECUTIVE_BACKGROUND("get-company-executive-background", Scope.SYMBOL),
    GET_COMPANY_OPERATIONAL_EFFICIENCY("get-company-operational-efficiency", Scope.SYMBOL),

    GET_TOP_TEN_BUY_SELL_BROKERS("get-top-ten-buy-sell-brokers", Scope.SYMBOL),
    GET_DAILY_SHORT_VOLUME("get-daily-short-volume", Scope.SYMBOL),
    GET_SHORT_INTEREST("get-short-interest", Scope.SYMBOL),

    REQUEST_TRADE_DATE("request-trade-date", Scope.SYMBOL);

    public enum Scope {
        GLOBAL, SYMBOL, SYMBOLS
    }

    private final String cliName;
    private final Scope scope;

    QuoteApiType(String cliName, Scope scope) {
        this.cliName = cliName;
        this.scope = scope;
    }

    public String getCliName() {
        return cliName;
    }

    public Scope getScope() {
        return scope;
    }

    public static QuoteApiType fromCliName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (QuoteApiType type : values()) {
            if (type.cliName.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown quote API: " + name
                + ". Use quote-pull --help for supported APIs.");
    }

    public void pull(FutuQuoteClient client,
                     QuoteStorageService storage,
                     AppConfig config,
                     PullContext ctx) throws InterruptedException {
        switch (this) {
            case GET_GLOBAL_STATE:
                storage.archiveChecked(cliName, "global",
                        client.getGlobalState(config.getUserId()));
                break;
            case GET_SUB_INFO:
                storage.archiveChecked(cliName, "global",
                        client.getSubInfo(QotGetSubInfo.C2S.newBuilder().build()));
                break;
            case REQUEST_HISTORY_KL_QUOTA:
                storage.archiveChecked(cliName, "global",
                        client.requestHistoryKLQuota(QotRequestHistoryKLQuota.C2S.newBuilder().build()));
                break;
            case GET_USER_SECURITY_GROUP:
                storage.archiveChecked(cliName, "global",
                        client.getUserSecurityGroup(QotGetUserSecurityGroup.C2S.newBuilder()
                                .setGroupType(QotGetUserSecurityGroup.GroupType.GroupType_All_VALUE)
                                .build()));
                break;
            default:
                pullSymbolScoped(client, storage, ctx);
        }
    }

    private void pullSymbolScoped(FutuQuoteClient client,
                                  QuoteStorageService storage,
                                  PullContext ctx) throws InterruptedException {
        List<QotCommon.Security> targets = ctx.getTargets(this);
        String begin = QuoteRequestBuilders.defaultFrom(ctx.from);
        String end = QuoteRequestBuilders.defaultTo(ctx.to);

        for (QotCommon.Security sec : targets) {
            String key = QuoteStorageService.entityKey(sec);
            GeneratedMessageV3 rsp = fetch(client, sec, begin, end, ctx);
            storage.archiveChecked(cliName, key, rsp);
            System.out.printf("Pulled %s for %s%n", cliName, key);
        }
    }

    private GeneratedMessageV3 fetch(FutuQuoteClient client,
                                     QotCommon.Security sec,
                                     String begin,
                                     String end,
                                     PullContext ctx) throws InterruptedException {
        switch (this) {
            case GET_MARKET_STATE:
                return client.getMarketState(QuoteRequestBuilders.marketState(sec));
            case GET_BASIC_QOT:
                return client.getBasicQot(QotGetBasicQot.C2S.newBuilder().addSecurityList(sec).build());
            case GET_ORDER_BOOK:
                return client.getOrderBook(sec, ctx.num);
            case GET_KL:
                return client.getKL(QotGetKL.C2S.newBuilder()
                        .setSecurity(sec)
                        .setKlType(KlTypeParser.parse(ctx.interval).getNumber())
                        .setRehabType(parseRehab(ctx.rehab).getNumber())
                        .setReqNum(ctx.num)
                        .build());
            case GET_RT:
                return client.getRT(QotGetRT.C2S.newBuilder().setSecurity(sec).build());
            case GET_TICKER:
                return client.getTicker(QotGetTicker.C2S.newBuilder()
                        .setSecurity(sec).setMaxRetNum(ctx.num).build());
            case GET_BROKER:
                return client.getBroker(QotGetBroker.C2S.newBuilder().setSecurity(sec).build());
            case REQUEST_REHAB:
                return client.requestRehab(QuoteRequestBuilders.rehab(sec));
            case GET_CAPITAL_FLOW:
                return client.getCapitalFlow(QuoteRequestBuilders.capitalFlow(sec, begin, end));
            case GET_CAPITAL_DISTRIBUTION:
                return client.getCapitalDistribution(QuoteRequestBuilders.capitalDistribution(sec));
            case GET_OWNER_PLATE:
                return client.getOwnerPlate(QuoteRequestBuilders.ownerPlate(sec));
            case GET_REFERENCE:
                return client.getReference(QuoteRequestBuilders.reference(sec));
            case GET_OPTION_EXPIRATION_DATE:
                return client.getOptionExpirationDate(QuoteRequestBuilders.optionExpirationDate(sec));
            case GET_OPTION_CHAIN:
                return client.getOptionChain(QuoteRequestBuilders.optionChain(sec, begin, end));
            case GET_WARRANT:
                return client.getWarrant(QuoteRequestBuilders.warrant(sec));
            case GET_OPTION_VOLATILITY:
                return client.getOptionVolatility(QuoteRequestBuilders.optionVolatility(sec));
            case GET_OPTION_EXERCISE_PROBABILITY:
                return client.getOptionExerciseProbability(QuoteRequestBuilders.optionExerciseProbability(sec));
            case GET_FINANCIALS_EARNINGS_PRICE_MOVE:
                return client.getFinancialsEarningsPriceMove(QuoteRequestBuilders.financialsEarningsPriceMove(sec));
            case GET_FINANCIALS_EARNINGS_PRICE_HISTORY:
                return client.getFinancialsEarningsPriceHistory(QuoteRequestBuilders.financialsEarningsPriceHistory(sec));
            case GET_FINANCIALS_STATEMENTS:
                return client.getFinancialsStatements(QuoteRequestBuilders.financialsStatements(sec));
            case GET_FINANCIALS_REVENUE_BREAKDOWN:
                return client.getFinancialsRevenueBreakdown(QuoteRequestBuilders.financialsRevenueBreakdown(sec));
            case GET_RESEARCH_ANALYST_CONSENSUS:
                return client.getResearchAnalystConsensus(QuoteRequestBuilders.researchAnalystConsensus(sec));
            case GET_RESEARCH_RATING_SUMMARY:
                return client.getResearchRatingSummary(QuoteRequestBuilders.researchRatingSummary(sec));
            case GET_RESEARCH_MORNINGSTAR_REPORT:
                return client.getResearchMorningstarReport(QuoteRequestBuilders.researchMorningstarReport(sec));
            case GET_VALUATION_DETAIL:
                return client.getValuationDetail(QuoteRequestBuilders.valuationDetail(sec));
            case GET_VALUATION_PLATE_STOCK_LIST:
                return client.getValuationPlateStockList(QuoteRequestBuilders.valuationPlateStockList(sec));
            case GET_CORPORATE_ACTIONS_DIVIDENDS:
                return client.getCorporateActionsDividends(QuoteRequestBuilders.corporateActionsDividends(sec));
            case GET_CORPORATE_ACTIONS_BUYBACKS:
                return client.getCorporateActionsBuybacks(QuoteRequestBuilders.corporateActionsBuybacks(sec));
            case GET_CORPORATE_ACTIONS_STOCK_SPLITS:
                return client.getCorporateActionsStockSplits(QuoteRequestBuilders.corporateActionsStockSplits(sec));
            case GET_SHAREHOLDERS_OVERVIEW:
                return client.getShareholdersOverview(QuoteRequestBuilders.shareholdersOverview(sec));
            case GET_SHAREHOLDERS_HOLDING_CHANGES:
                return client.getShareholdersHoldingChanges(QuoteRequestBuilders.shareholdersHoldingChanges(sec));
            case GET_SHAREHOLDERS_HOLDER_DETAIL:
                return client.getShareholdersHolderDetail(QuoteRequestBuilders.shareholdersHolderDetail(sec));
            case GET_SHAREHOLDERS_INSTITUTIONAL:
                return client.getShareholdersInstitutional(QuoteRequestBuilders.shareholdersInstitutional(sec));
            case GET_INSIDER_HOLDER_LIST:
                return client.getInsiderHolderList(QuoteRequestBuilders.insiderHolderList(sec));
            case GET_INSIDER_TRADE_LIST:
                return client.getInsiderTradeList(QuoteRequestBuilders.insiderTradeList(sec));
            case GET_COMPANY_PROFILE:
                return client.getCompanyProfile(QuoteRequestBuilders.companyProfile(sec));
            case GET_COMPANY_EXECUTIVES:
                return client.getCompanyExecutives(QuoteRequestBuilders.companyExecutives(sec));
            case GET_COMPANY_EXECUTIVE_BACKGROUND:
                return client.getCompanyExecutiveBackground(QuoteRequestBuilders.companyExecutiveBackground(sec));
            case GET_COMPANY_OPERATIONAL_EFFICIENCY:
                return client.getCompanyOperationalEfficiency(QuoteRequestBuilders.companyOperationalEfficiency(sec));
            case GET_TOP_TEN_BUY_SELL_BROKERS:
                return client.getTopTenBuySellBrokers(QuoteRequestBuilders.topTenBuySellBrokers(sec));
            case GET_DAILY_SHORT_VOLUME:
                return client.getDailyShortVolume(QuoteRequestBuilders.dailyShortVolume(sec));
            case GET_SHORT_INTEREST:
                return client.getShortInterest(QuoteRequestBuilders.shortInterest(sec));
            case REQUEST_TRADE_DATE:
                return client.requestTradeDate(QuoteRequestBuilders.tradeDate(sec, begin, end));
            default:
                throw new IllegalStateException("Unhandled API: " + this);
        }
    }

    private static QotCommon.RehabType parseRehab(String rehab) {
        if (rehab == null) {
            return QotCommon.RehabType.RehabType_Forward;
        }
        switch (rehab.toLowerCase(Locale.ROOT)) {
            case "none":
                return QotCommon.RehabType.RehabType_None;
            case "backward":
                return QotCommon.RehabType.RehabType_Backward;
            default:
                return QotCommon.RehabType.RehabType_Forward;
        }
    }

    public static class PullContext {
        public final List<QotCommon.Security> symbols;
        public final QotCommon.Security singleSymbol;
        public final String from;
        public final String to;
        public final String interval;
        public final String rehab;
        public final int num;

        public PullContext(List<QotCommon.Security> symbols,
                           QotCommon.Security singleSymbol,
                           String from,
                           String to,
                           String interval,
                           String rehab,
                           int num) {
            this.symbols = symbols;
            this.singleSymbol = singleSymbol;
            this.from = from;
            this.to = to;
            this.interval = interval;
            this.rehab = rehab;
            this.num = num;
        }

        public List<QotCommon.Security> getTargets(QuoteApiType type) {
            if (type.scope == Scope.SYMBOL) {
                if (singleSymbol == null) {
                    throw new IllegalArgumentException(type.cliName + " requires --symbol");
                }
                return java.util.Collections.singletonList(singleSymbol);
            }
            if (symbols == null || symbols.isEmpty()) {
                throw new IllegalArgumentException(type.cliName + " requires --symbols");
            }
            return symbols;
        }
    }
}
