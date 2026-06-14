package com.futu.opend.data.collector.service;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetBasicQot;
import com.futu.openapi.pb.QotGetBroker;
import com.futu.openapi.pb.QotGetKL;
import com.futu.openapi.pb.QotGetMarketState;
import com.futu.openapi.pb.QotGetOrderBook;
import com.futu.openapi.pb.QotGetRT;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotGetSubInfo;
import com.futu.openapi.pb.QotGetTicker;
import com.futu.openapi.pb.QotGetUserSecurityGroup;
import com.futu.openapi.pb.QotRequestHistoryKLQuota;
import com.google.protobuf.GeneratedMessageV3;
import com.futu.opend.data.collector.client.FutuQuoteClient;
import com.futu.opend.data.collector.config.AppConfig;
import com.futu.opend.data.collector.util.KlTypeParser;
import com.futu.opend.data.collector.util.SymbolParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum QuoteApiType {
    GET_GLOBAL_STATE("get-global-state", Scope.GLOBAL),
    GET_SUB_INFO("get-sub-info", Scope.GLOBAL),
    REQUEST_HISTORY_KL_QUOTA("request-history-kl-quota", Scope.GLOBAL),
    GET_USER_SECURITY_GROUP("get-user-security-group", Scope.GLOBAL),
    STOCK_FILTER("stock-filter", Scope.GLOBAL),
    GET_PLATE_SET("get-plate-set", Scope.GLOBAL),
    GET_PLATE_SECURITY("get-plate-security", Scope.GLOBAL),
    GET_USER_SECURITY("get-user-security", Scope.GLOBAL),
    MODIFY_USER_SECURITY("modify-user-security", Scope.GLOBAL),
    GET_PRICE_REMINDER("get-price-reminder", Scope.GLOBAL),
    GET_IPO_LIST("get-ipo-list", Scope.GLOBAL),
    GET_STATIC_INFO("get-static-info", Scope.GLOBAL),

    GET_MARKET_STATE("get-market-state", Scope.SYMBOLS),
    GET_BASIC_QOT("get-basic-qot", Scope.SYMBOLS),
    GET_SECURITY_SNAPSHOT("get-security-snapshot", Scope.SYMBOLS),
    GET_FUTURE_INFO("get-future-info", Scope.SYMBOLS),
    REQUEST_HISTORY_KL("request-history-kl", Scope.SYMBOL),

    GET_ORDER_BOOK("get-order-book", Scope.SYMBOL),
    GET_KL("get-kl", Scope.SYMBOL),
    GET_RT("get-rt", Scope.SYMBOL),
    GET_TICKER("get-ticker", Scope.SYMBOL),
    GET_BROKER("get-broker", Scope.SYMBOL),
    SET_PRICE_REMINDER("set-price-reminder", Scope.SYMBOL),

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
            case STOCK_FILTER:
                requireMarket(ctx);
                storage.archiveChecked(cliName, ctx.market,
                        client.stockFilter(QuoteRequestBuilders.stockFilter(ctx.market, ctx.num)));
                break;
            case GET_PLATE_SET:
                requireMarket(ctx);
                storage.archiveChecked(cliName, ctx.market,
                        client.getPlateSet(QuoteRequestBuilders.plateSet(ctx.market, ctx.plateType)));
                break;
            case GET_PLATE_SECURITY:
                requirePlateCode(ctx);
                QotCommon.Security plate = SymbolParser.parse(ctx.plateCode);
                storage.archiveChecked(cliName, ctx.plateCode,
                        client.getPlateSecurity(QuoteRequestBuilders.plateSecurity(plate)));
                break;
            case GET_USER_SECURITY:
                requireGroupName(ctx);
                storage.archiveChecked(cliName, ctx.groupName,
                        client.getUserSecurity(QuoteRequestBuilders.userSecurity(ctx.groupName)));
                break;
            case MODIFY_USER_SECURITY:
                requireGroupName(ctx);
                List<QotCommon.Security> modifyTargets = ctx.getTargets(this);
                storage.archiveChecked(cliName, ctx.groupName,
                        client.modifyUserSecurity(
                                QuoteRequestBuilders.modifyUserSecurity(ctx.groupName, ctx.op, modifyTargets)));
                break;
            case GET_PRICE_REMINDER:
                requireMarket(ctx);
                storage.archiveChecked(cliName, ctx.market,
                        client.getPriceReminder(QuoteRequestBuilders.priceReminder(ctx.market)));
                break;
            case GET_IPO_LIST:
                requireMarket(ctx);
                storage.archiveChecked(cliName, ctx.market,
                        client.getIpoList(QuoteRequestBuilders.ipoList(ctx.market)));
                break;
            case GET_STATIC_INFO:
                requireMarket(ctx);
                pullStaticInfo(client, storage, ctx);
                break;
            case GET_SECURITY_SNAPSHOT:
                pullSnapshots(client, storage, ctx);
                break;
            case GET_FUTURE_INFO:
                pullFutureInfo(client, storage, ctx);
                break;
            case REQUEST_HISTORY_KL:
                pullHistoryKlines(client, storage, ctx);
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
            storage.archiveChecked(cliName, key, sec, rsp);
            System.out.printf("Pulled %s for %s%n", cliName, key);
        }
    }

    private void pullHistoryKlines(FutuQuoteClient client,
                                   QuoteStorageService storage,
                                   PullContext ctx) throws InterruptedException {
        QotCommon.Security sec = ctx.getTargets(this).get(0);
        String begin = QuoteRequestBuilders.defaultFrom(ctx.from);
        String end = QuoteRequestBuilders.defaultTo(ctx.to);
        QotCommon.KLType klType = KlTypeParser.parse(ctx.interval);
        QotCommon.RehabType rehabType = parseRehab(ctx.rehab);
        List<QotCommon.KLine> klines = storage.fetchAllHistoryKlines(
                client, sec, klType, rehabType, begin, end);
        storage.saveKlines(sec, ctx.interval, klines);
        System.out.printf("Pulled request-history-kl for %s (%d bars)%n",
                QuoteStorageService.entityKey(sec), klines.size());
    }

    private void pullSnapshots(FutuQuoteClient client,
                               QuoteStorageService storage,
                               PullContext ctx) throws InterruptedException {
        List<QotCommon.Security> all = ctx.getTargets(this);
        List<QotGetSecuritySnapshot.Snapshot> collected = new ArrayList<>();
        for (int i = 0; i < all.size(); i += 200) {
            int end = Math.min(i + 200, all.size());
            List<QotCommon.Security> batch = all.subList(i, end);
            QotGetSecuritySnapshot.Response rsp = client.getSecuritySnapshot(batch);
            storage.archiveChecked(cliName, "batch-" + i, rsp);
            FutuQuoteClient.checkSuccess(rsp);
            collected.addAll(rsp.getS2C().getSnapshotListList());
            if (end < all.size()) {
                Thread.sleep(3000);
            }
        }
        storage.saveSnapshots(collected);
        System.out.printf("Pulled get-security-snapshot (%d snapshots)%n", collected.size());
    }

    private void pullFutureInfo(FutuQuoteClient client,
                                QuoteStorageService storage,
                                PullContext ctx) throws InterruptedException {
        List<QotCommon.Security> targets = ctx.getTargets(this);
        GeneratedMessageV3 rsp = client.getFutureInfo(QuoteRequestBuilders.futureInfo(targets));
        storage.archiveChecked(cliName, "futures", rsp);
        System.out.printf("Pulled get-future-info for %d symbol(s)%n", targets.size());
    }

    private void pullStaticInfo(FutuQuoteClient client,
                                QuoteStorageService storage,
                                PullContext ctx) throws InterruptedException {
        int[] stockTypes = {
                QotCommon.SecurityType.SecurityType_Eqty_VALUE,
                QotCommon.SecurityType.SecurityType_Index_VALUE,
                QotCommon.SecurityType.SecurityType_Trust_VALUE,
                QotCommon.SecurityType.SecurityType_Warrant_VALUE,
                QotCommon.SecurityType.SecurityType_Bond_VALUE
        };
        List<QotCommon.SecurityStaticInfo> all = new ArrayList<>();
        for (int stockType : stockTypes) {
            QotGetStaticInfo.C2S c2s = QuoteRequestBuilders.staticInfo(ctx.market, stockType);
            QotGetStaticInfo.Response rsp = client.getStaticInfo(c2s);
            storage.archiveChecked(cliName, ctx.market + ":" + stockType, rsp);
            FutuQuoteClient.checkSuccess(rsp);
            all.addAll(rsp.getS2C().getStaticInfoListList());
            Thread.sleep(1000);
        }
        storage.saveStaticInfo(all);
        System.out.printf("Pulled get-static-info for %s (%d records)%n", ctx.market, all.size());
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
            case SET_PRICE_REMINDER:
                return client.setPriceReminder(QuoteRequestBuilders.setPriceReminder(
                        sec, ctx.op, ctx.reminderType, ctx.reminderValue, ctx.reminderKey, ctx.reminderNote));
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

    private static void requireMarket(PullContext ctx) {
        if (ctx.market == null || ctx.market.isEmpty()) {
            throw new IllegalArgumentException("Requires --market");
        }
    }

    private static void requirePlateCode(PullContext ctx) {
        if (ctx.plateCode == null || ctx.plateCode.isEmpty()) {
            throw new IllegalArgumentException("Requires --plate-code");
        }
    }

    private static void requireGroupName(PullContext ctx) {
        if (ctx.groupName == null || ctx.groupName.isEmpty()) {
            throw new IllegalArgumentException("Requires --group-name");
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
        public final String market;
        public final String plateCode;
        public final String plateType;
        public final String groupName;
        public final String op;
        public final int reminderType;
        public final double reminderValue;
        public final long reminderKey;
        public final String reminderNote;

        public PullContext(List<QotCommon.Security> symbols,
                           QotCommon.Security singleSymbol,
                           String from,
                           String to,
                           String interval,
                           String rehab,
                           int num,
                           String market,
                           String plateCode,
                           String plateType,
                           String groupName,
                           String op,
                           int reminderType,
                           double reminderValue,
                           long reminderKey,
                           String reminderNote) {
            this.symbols = symbols;
            this.singleSymbol = singleSymbol;
            this.from = from;
            this.to = to;
            this.interval = interval;
            this.rehab = rehab;
            this.num = num;
            this.market = market;
            this.plateCode = plateCode;
            this.plateType = plateType;
            this.groupName = groupName;
            this.op = op;
            this.reminderType = reminderType;
            this.reminderValue = reminderValue;
            this.reminderKey = reminderKey;
            this.reminderNote = reminderNote;
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
