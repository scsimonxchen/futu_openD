package com.stocklab.collector.service;

import com.futu.openapi.pb.GetGlobalState;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetIpoList;
import com.futu.openapi.pb.QotGetUserSecurityGroup;
import com.futu.openapi.pb.QotRequestHistoryKLQuota;
import com.google.protobuf.GeneratedMessageV3;
import com.stocklab.collector.client.FutuQuoteClient;
import com.stocklab.collector.config.AppConfig;
import com.stocklab.collector.util.KlTypeParser;
import com.stocklab.collector.util.SymbolParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class QuoteHistoricalSyncService {
    private static final long RATE_LIMIT_MS = 300;

    private final FutuQuoteClient client;
    private final QuoteStorageService storage;
    private final AppConfig config;

    public QuoteHistoricalSyncService(FutuQuoteClient client, QuoteStorageService storage, AppConfig config) {
        this.client = client;
        this.storage = storage;
        this.config = config;
    }

    public void sync(List<QotCommon.Security> symbols,
                     Set<String> apiGroups,
                     String from,
                     String to,
                     String interval,
                     String rehab) throws InterruptedException {
        String begin = QuoteRequestBuilders.defaultFrom(from);
        String end = QuoteRequestBuilders.defaultTo(to);
        QotCommon.KLType klType = KlTypeParser.parse(interval);
        QotCommon.RehabType rehabType = parseRehab(rehab);

        syncMarketWide(symbols, apiGroups, begin, end);

        for (QotCommon.Security sec : symbols) {
            String key = QuoteStorageService.entityKey(sec);
            System.out.printf("Syncing %s ...%n", key);

            if (apiGroups.contains("all") || apiGroups.contains("kline")) {
                List<QotCommon.KLine> klines = storage.fetchAllHistoryKlines(
                        client, sec, klType, rehabType, begin, end);
                storage.saveKlines(sec, interval, klines);
                System.out.printf("  kline: %d bars%n", klines.size());
                sleep();
            }

            if (apiGroups.contains("all") || apiGroups.contains("rehab")) {
                syncApi("request_rehab", key,
                        client.requestRehab(QuoteRequestBuilders.rehab(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("capital-flow")) {
                syncApi("get_capital_flow", key,
                        client.getCapitalFlow(QuoteRequestBuilders.capitalFlow(sec, begin, end)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("capital-distribution")) {
                syncApi("get_capital_distribution", key,
                        client.getCapitalDistribution(QuoteRequestBuilders.capitalDistribution(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("owner-plate")) {
                syncApi("get_owner_plate", key,
                        client.getOwnerPlate(QuoteRequestBuilders.ownerPlate(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("market-state")) {
                syncApi("get_market_state", key,
                        client.getMarketState(QuoteRequestBuilders.marketState(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("reference")) {
                syncApi("get_reference", key,
                        client.getReference(QuoteRequestBuilders.reference(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("derivatives")) {
                syncApi("get_option_expiration_date", key,
                        client.getOptionExpirationDate(QuoteRequestBuilders.optionExpirationDate(sec)));
                syncApi("get_option_chain", key,
                        client.getOptionChain(QuoteRequestBuilders.optionChain(sec, begin, end)));
                syncApi("get_warrant", key,
                        client.getWarrant(QuoteRequestBuilders.warrant(sec)));
                syncApi("get_option_volatility", key,
                        client.getOptionVolatility(QuoteRequestBuilders.optionVolatility(sec)));
                syncApi("get_option_exercise_probability", key,
                        client.getOptionExerciseProbability(QuoteRequestBuilders.optionExerciseProbability(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("financials")) {
                syncApi("get_financials_earnings_price_move", key,
                        client.getFinancialsEarningsPriceMove(QuoteRequestBuilders.financialsEarningsPriceMove(sec)));
                syncApi("get_financials_earnings_price_history", key,
                        client.getFinancialsEarningsPriceHistory(QuoteRequestBuilders.financialsEarningsPriceHistory(sec)));
                syncApi("get_financials_statements", key,
                        client.getFinancialsStatements(QuoteRequestBuilders.financialsStatements(sec)));
                syncApi("get_financials_revenue_breakdown", key,
                        client.getFinancialsRevenueBreakdown(QuoteRequestBuilders.financialsRevenueBreakdown(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("research")) {
                syncApi("get_research_analyst_consensus", key,
                        client.getResearchAnalystConsensus(QuoteRequestBuilders.researchAnalystConsensus(sec)));
                syncApi("get_research_rating_summary", key,
                        client.getResearchRatingSummary(QuoteRequestBuilders.researchRatingSummary(sec)));
                syncApi("get_research_morningstar_report", key,
                        client.getResearchMorningstarReport(QuoteRequestBuilders.researchMorningstarReport(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("valuation")) {
                syncApi("get_valuation_detail", key,
                        client.getValuationDetail(QuoteRequestBuilders.valuationDetail(sec)));
                syncApi("get_valuation_plate_stock_list", key,
                        client.getValuationPlateStockList(QuoteRequestBuilders.valuationPlateStockList(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("corporate-actions")) {
                syncApi("get_corporate_actions_dividends", key,
                        client.getCorporateActionsDividends(QuoteRequestBuilders.corporateActionsDividends(sec)));
                syncApi("get_corporate_actions_buybacks", key,
                        client.getCorporateActionsBuybacks(QuoteRequestBuilders.corporateActionsBuybacks(sec)));
                syncApi("get_corporate_actions_stock_splits", key,
                        client.getCorporateActionsStockSplits(QuoteRequestBuilders.corporateActionsStockSplits(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("shareholders")) {
                syncApi("get_shareholders_overview", key,
                        client.getShareholdersOverview(QuoteRequestBuilders.shareholdersOverview(sec)));
                syncApi("get_shareholders_holding_changes", key,
                        client.getShareholdersHoldingChanges(QuoteRequestBuilders.shareholdersHoldingChanges(sec)));
                syncApi("get_shareholders_holder_detail", key,
                        client.getShareholdersHolderDetail(QuoteRequestBuilders.shareholdersHolderDetail(sec)));
                syncApi("get_shareholders_institutional", key,
                        client.getShareholdersInstitutional(QuoteRequestBuilders.shareholdersInstitutional(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("insider")) {
                syncApi("get_insider_holder_list", key,
                        client.getInsiderHolderList(QuoteRequestBuilders.insiderHolderList(sec)));
                syncApi("get_insider_trade_list", key,
                        client.getInsiderTradeList(QuoteRequestBuilders.insiderTradeList(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("company")) {
                syncApi("get_company_profile", key,
                        client.getCompanyProfile(QuoteRequestBuilders.companyProfile(sec)));
                syncApi("get_company_executives", key,
                        client.getCompanyExecutives(QuoteRequestBuilders.companyExecutives(sec)));
                syncApi("get_company_executive_background", key,
                        client.getCompanyExecutiveBackground(QuoteRequestBuilders.companyExecutiveBackground(sec)));
                syncApi("get_company_operational_efficiency", key,
                        client.getCompanyOperationalEfficiency(QuoteRequestBuilders.companyOperationalEfficiency(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("brokers")) {
                syncApi("get_top_ten_buy_sell_brokers", key,
                        client.getTopTenBuySellBrokers(QuoteRequestBuilders.topTenBuySellBrokers(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("short")) {
                syncApi("get_daily_short_volume", key,
                        client.getDailyShortVolume(QuoteRequestBuilders.dailyShortVolume(sec)));
                syncApi("get_short_interest", key,
                        client.getShortInterest(QuoteRequestBuilders.shortInterest(sec)));
            }
            if (apiGroups.contains("all") || apiGroups.contains("trade-date")) {
                syncApi("request_trade_date", key,
                        client.requestTradeDate(QuoteRequestBuilders.tradeDate(sec, begin, end)));
            }
        }
    }

    private void syncMarketWide(List<QotCommon.Security> symbols, Set<String> apiGroups,
                                String begin, String end) throws InterruptedException {
        if (!apiGroups.contains("all") && !apiGroups.contains("market-wide")) {
            return;
        }
        System.out.println("Syncing market-wide APIs...");
        syncApi("get_global_state", "global",
                client.getGlobalState(config.getUserId()));
        syncApi("request_history_kl_quota", "global",
                client.requestHistoryKLQuota(QotRequestHistoryKLQuota.C2S.newBuilder().build()));
        syncApi("get_user_security_group", "global",
                client.getUserSecurityGroup(QotGetUserSecurityGroup.C2S.newBuilder()
                        .setGroupType(QotGetUserSecurityGroup.GroupType.GroupType_All_VALUE)
                        .build()));
        if (!symbols.isEmpty()) {
            QotCommon.Security first = symbols.get(0);
            syncApi("get_ipo_list", SymbolParser.marketName(first.getMarket()),
                    client.getIpoList(QotGetIpoList.C2S.newBuilder().setMarket(first.getMarket()).build()));
            syncApi("request_trade_date", SymbolParser.marketName(first.getMarket()),
                    client.requestTradeDate(QuoteRequestBuilders.tradeDate(first, begin, end)));
        }
    }

    private void syncApi(String apiName, String entityKey, GeneratedMessageV3 response)
            throws InterruptedException {
        try {
            storage.archiveChecked(apiName, entityKey, response);
            System.out.printf("  %s: ok%n", apiName);
        } catch (Exception e) {
            System.out.printf("  %s: skipped (%s)%n", apiName, e.getMessage());
        }
        sleep();
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(RATE_LIMIT_MS);
    }

    public static Set<String> parseApiGroups(String apisCsv) {
        if (apisCsv == null || apisCsv.trim().isEmpty() || "all".equalsIgnoreCase(apisCsv.trim())) {
            return new HashSet<>(Arrays.asList("all"));
        }
        Set<String> groups = new HashSet<>();
        for (String part : apisCsv.split(",")) {
            String g = part.trim().toLowerCase(Locale.ROOT);
            if (!g.isEmpty()) {
                groups.add(g);
            }
        }
        return groups;
    }

    public static List<String> listApiGroups() {
        List<String> groups = new ArrayList<>();
        groups.add("all");
        groups.add("kline");
        groups.add("rehab");
        groups.add("capital-flow");
        groups.add("capital-distribution");
        groups.add("owner-plate");
        groups.add("market-state");
        groups.add("reference");
        groups.add("derivatives");
        groups.add("financials");
        groups.add("research");
        groups.add("valuation");
        groups.add("corporate-actions");
        groups.add("shareholders");
        groups.add("insider");
        groups.add("company");
        groups.add("brokers");
        groups.add("short");
        groups.add("trade-date");
        groups.add("market-wide");
        return groups;
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
}
