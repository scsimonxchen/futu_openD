package com.futu.openapi;
import com.futu.openapi.pb.*;

/***
 * 行情请求返回的回调函数
 */
public interface FTSPI_Qot {
    default void onReply_GetGlobalState(FTAPI_Conn client, int nSerialNo, GetGlobalState.Response rsp){} //获取全局状态回调
    default void onReply_Sub(FTAPI_Conn client, int nSerialNo, QotSub.Response rsp){} //订阅或者反订阅回调
    default void onReply_RegQotPush(FTAPI_Conn client, int nSerialNo, QotRegQotPush.Response rsp){} //注册推送回调
    default void onReply_GetSubInfo(FTAPI_Conn client, int nSerialNo, QotGetSubInfo.Response rsp){} //获取订阅信息回调
    default void onReply_GetTicker(FTAPI_Conn client, int nSerialNo, QotGetTicker.Response rsp){} //获取逐笔,调用该接口前需要先订阅(订阅位：Qot_Common.SubType_Ticker)回调
    default void onReply_GetBasicQot(FTAPI_Conn client, int nSerialNo, QotGetBasicQot.Response rsp){} //获取基本行情,调用该接口前需要先订阅(订阅位：Qot_Common.SubType_Basic)回调
    default void onReply_GetOrderBook(FTAPI_Conn client, int nSerialNo, QotGetOrderBook.Response rsp){} //获取摆盘,调用该接口前需要先订阅(订阅位：Qot_Common.SubType_OrderBook)回调
    default void onReply_GetKL(FTAPI_Conn client, int nSerialNo, QotGetKL.Response rsp){} //获取K线，调用该接口前需要先订阅(订阅位：Qot_Common.SubType_KL_XXX)回调
    default void onReply_GetRT(FTAPI_Conn client, int nSerialNo, QotGetRT.Response rsp){} //获取分时，调用该接口前需要先订阅(订阅位：Qot_Common.SubType_RT)回调
    default void onReply_GetBroker(FTAPI_Conn client, int nSerialNo, QotGetBroker.Response rsp){} //获取经纪队列，调用该接口前需要先订阅(订阅位：Qot_Common.SubType_Broker)回调
    default void onReply_GetRehab(FTAPI_Conn client, int nSerialNo, QotGetRehab.Response rsp){} //获取本地历史复权信息回调
    default void onReply_RequestRehab(FTAPI_Conn client, int nSerialNo, QotRequestRehab.Response rsp){} //在线请求历史复权信息，不读本地历史数据DB回调
    default void onReply_RequestHistoryKL(FTAPI_Conn client, int nSerialNo, QotRequestHistoryKL.Response rsp){} //在线请求历史K线，不读本地历史数据DB回调
    default void onReply_RequestHistoryKLQuota(FTAPI_Conn client, int nSerialNo, QotRequestHistoryKLQuota.Response rsp){} //获取历史K线已经用掉的额度回调
    default void onReply_GetStaticInfo(FTAPI_Conn client, int nSerialNo, QotGetStaticInfo.Response rsp){} //获取静态信息回调
    default void onReply_GetSecuritySnapshot(FTAPI_Conn client, int nSerialNo, QotGetSecuritySnapshot.Response rsp){} //获取股票快照回调
    default void onReply_GetPlateSet(FTAPI_Conn client, int nSerialNo, QotGetPlateSet.Response rsp){} //获取板块集合下的板块回调
    default void onReply_GetPlateSecurity(FTAPI_Conn client, int nSerialNo, QotGetPlateSecurity.Response rsp){} //获取板块下的股票回调
    default void onReply_GetReference(FTAPI_Conn client, int nSerialNo, QotGetReference.Response rsp){} //获取相关股票回调
    default void onReply_GetOwnerPlate(FTAPI_Conn client, int nSerialNo, QotGetOwnerPlate.Response rsp){} //获取股票所属的板块回调
    default void onReply_GetHoldingChangeList(FTAPI_Conn client, int nSerialNo, QotGetHoldingChangeList.Response rsp){} //获取大股东持股变化列表回调
    default void onReply_GetOptionChain(FTAPI_Conn client, int nSerialNo, QotGetOptionChain.Response rsp){} //筛选期权回调
    default void onReply_GetWarrant(FTAPI_Conn client, int nSerialNo, QotGetWarrant.Response rsp){} //筛选窝轮回调
    default void onReply_GetCapitalFlow(FTAPI_Conn client, int nSerialNo, QotGetCapitalFlow.Response rsp){} //获取资金流向回调
    default void onReply_GetCapitalDistribution(FTAPI_Conn client, int nSerialNo, QotGetCapitalDistribution.Response rsp){} //获取资金分布回调
    default void onReply_GetUserSecurity(FTAPI_Conn client, int nSerialNo, QotGetUserSecurity.Response rsp){} //获取自选股分组下的股票回调
    default void onReply_ModifyUserSecurity(FTAPI_Conn client, int nSerialNo, QotModifyUserSecurity.Response rsp){} //修改自选股分组下的股票回调
    default void onReply_StockFilter(FTAPI_Conn client, int nSerialNo, QotStockFilter.Response rsp){} //条件选股
    default void onReply_GetCodeChange(FTAPI_Conn client, int nSerialNo, QotGetCodeChange.Response rsp){} //获取股票代码变化信息
    default void onReply_GetIpoList(FTAPI_Conn client, int nSerialNo, QotGetIpoList.Response rsp){} //获取IPO列表
    default void onReply_GetFutureInfo(FTAPI_Conn client, int nSerialNo, QotGetFutureInfo.Response rsp){} //获取期货合约资料
    default void onReply_RequestTradeDate(FTAPI_Conn client, int nSerialNo, QotRequestTradeDate.Response rsp){} //在线拉取交易日
    default void onReply_SetPriceReminder(FTAPI_Conn client, int nSerialNo, QotSetPriceReminder.Response rsp){} //设置到价提醒
    default void onReply_GetPriceReminder(FTAPI_Conn client, int nSerialNo, QotGetPriceReminder.Response rsp){} //获取到价提醒
    default void onReply_GetUserSecurityGroup(FTAPI_Conn client, int nSerialNo, QotGetUserSecurityGroup.Response rsp){} //获取自选股分组
    default void onReply_GetMarketState(FTAPI_Conn client, int nSerialNo, QotGetMarketState.Response rsp){} //获取指定品种的市场状态
    default void onReply_GetOptionExpirationDate(FTAPI_Conn client, int nSerialNo, QotGetOptionExpirationDate.Response rsp){} //获取指定品种的市场状态
    default void onReply_GetFinancialsEarningsPriceMove(FTAPI_Conn client, int nSerialNo, QotGetFinancialsEarningsPriceMove.Response rsp){} //获取个股财报日前后价格涨跌幅表现
    default void onReply_GetFinancialsEarningsPriceHistory(FTAPI_Conn client, int nSerialNo, QotGetFinancialsEarningsPriceHistory.Response rsp){} //获取个股财报日前后股价历史
    default void onReply_GetFinancialsStatements(FTAPI_Conn client, int nSerialNo, QotGetFinancialsStatements.Response rsp){} //获取财务报表
    default void onReply_GetFinancialsRevenueBreakdown(FTAPI_Conn client, int nSerialNo, QotGetFinancialsRevenueBreakdown.Response rsp){} //获取主营构成
    default void onReply_GetResearchAnalystConsensus(FTAPI_Conn client, int nSerialNo, QotGetResearchAnalystConsensus.Response rsp){} //获取分析师评级概述
    default void onReply_GetResearchRatingSummary(FTAPI_Conn client, int nSerialNo, QotGetResearchRatingSummary.Response rsp){} //获取评级汇总
    default void onReply_GetResearchMorningstarReport(FTAPI_Conn client, int nSerialNo, QotGetResearchMorningstarReport.Response rsp){} //获取晨星研究报告
    default void onReply_GetValuationDetail(FTAPI_Conn client, int nSerialNo, QotGetValuationDetail.Response rsp){} //获取估值详情
    default void onReply_GetValuationPlateStockList(FTAPI_Conn client, int nSerialNo, QotGetValuationPlateStockList.Response rsp){} //获取板块/指数成分股估值
    default void onReply_GetCorporateActionsDividends(FTAPI_Conn client, int nSerialNo, QotGetCorporateActionsDividends.Response rsp){} //获取分红派息
    default void onReply_GetCorporateActionsBuybacks(FTAPI_Conn client, int nSerialNo, QotGetCorporateActionsBuybacks.Response rsp){} //获取回购
    default void onReply_GetCorporateActionsStockSplits(FTAPI_Conn client, int nSerialNo, QotGetCorporateActionsStockSplits.Response rsp){} //获取拆合股
    default void onReply_GetShareholdersOverview(FTAPI_Conn client, int nSerialNo, QotGetShareholdersOverview.Response rsp){} //获取持股统计
    default void onReply_GetShareholdersHoldingChanges(FTAPI_Conn client, int nSerialNo, QotGetShareholdersHoldingChanges.Response rsp){} //获取持股变动
    default void onReply_GetShareholdersHolderDetail(FTAPI_Conn client, int nSerialNo, QotGetShareholdersHolderDetail.Response rsp){} //获取持股明细
    default void onReply_GetShareholdersInstitutional(FTAPI_Conn client, int nSerialNo, QotGetShareholdersInstitutional.Response rsp){} //获取机构持股
    default void onReply_GetInsiderHolderList(FTAPI_Conn client, int nSerialNo, QotGetInsiderHolderList.Response rsp){} //获取内部人持股列表
    default void onReply_GetInsiderTradeList(FTAPI_Conn client, int nSerialNo, QotGetInsiderTradeList.Response rsp){} //获取内部人交易列表
    default void onReply_GetCompanyProfile(FTAPI_Conn client, int nSerialNo, QotGetCompanyProfile.Response rsp){} //获取公司详情
    default void onReply_GetCompanyExecutives(FTAPI_Conn client, int nSerialNo, QotGetCompanyExecutives.Response rsp){} //获取公司高管信息
    default void onReply_GetCompanyExecutiveBackground(FTAPI_Conn client, int nSerialNo, QotGetCompanyExecutiveBackground.Response rsp){} //获取公司高管背景
    default void onReply_GetCompanyOperationalEfficiency(FTAPI_Conn client, int nSerialNo, QotGetCompanyOperationalEfficiency.Response rsp){} //获取公司经营效率
    default void onReply_GetTopTenBuySellBrokers(FTAPI_Conn client, int nSerialNo, QotGetTopTenBuySellBrokers.Response rsp){} //获取十大买卖经纪商
    default void onReply_GetDailyShortVolume(FTAPI_Conn client, int nSerialNo, QotGetDailyShortVolume.Response rsp){} //获取每日卖空
    default void onReply_GetShortInterest(FTAPI_Conn client, int nSerialNo, QotGetShortInterest.Response rsp){} //获取空头持仓
    default void onReply_GetOptionVolatility(FTAPI_Conn client, int nSerialNo, QotGetOptionVolatility.Response rsp){} //获取期权波动率分析
    default void onReply_GetOptionExerciseProbability(FTAPI_Conn client, int nSerialNo, QotGetOptionExerciseProbability.Response rsp){} //获取期权行权概率
    default void onReply_StockScreen(FTAPI_Conn client, int nSerialNo, QotStockScreen.Response rsp){} //条件选股V2
    default void onReply_OptionScreen(FTAPI_Conn client, int nSerialNo, QotOptionScreen.Response rsp){} //期权选股
    default void onReply_WarrantScreen(FTAPI_Conn client, int nSerialNo, QotWarrantScreen.Response rsp){} //窝轮筛选V2
    default void onReply_GetOptionQuote(FTAPI_Conn client, int nSerialNo, QotGetOptionQuote.Response rsp){} //获取期权行情
    default void onReply_GetOptionStrategy(FTAPI_Conn client, int nSerialNo, QotGetOptionStrategy.Response rsp){} //获取期权策略
    default void onReply_GetOptionStrategyAnalysis(FTAPI_Conn client, int nSerialNo, QotGetOptionStrategyAnalysis.Response rsp){} //获取期权策略分析
    default void onReply_GetOptionStrategySpread(FTAPI_Conn client, int nSerialNo, QotGetOptionStrategySpread.Response rsp){} //获取期权策略价差
    default void onReply_GetTechnicalUnusual(FTAPI_Conn client, int nSerialNo, SkillWrapAPI.TechnicalUnusualRsp rsp){} //技术指标异动
    default void onReply_GetFinancialUnusual(FTAPI_Conn client, int nSerialNo, SkillWrapAPI.FinancialUnusualRsp rsp){} //财务异动
    default void onReply_GetDerivativeUnusual(FTAPI_Conn client, int nSerialNo, SkillWrapAPI.DerivativeUnusualRsp rsp){} //衍生品异动
    default void onPush_Notify(FTAPI_Conn client, Notify.Response rsp){}  //推送系统通知
    default void onPush_UpdateOrderBook(FTAPI_Conn client, QotUpdateOrderBook.Response rsp){} //推送摆盘
    default void onPush_UpdateBasicQuote(FTAPI_Conn client, QotUpdateBasicQot.Response rsp){} //推送报价
    default void onPush_UpdateKL(FTAPI_Conn client, QotUpdateKL.Response rsp){} //推送K线
    default void onPush_UpdateRT(FTAPI_Conn client, QotUpdateRT.Response rsp){} //推送分时
    default void onPush_UpdateTicker(FTAPI_Conn client, QotUpdateTicker.Response rsp){} //推送逐笔
    default void onPush_UpdateBroker(FTAPI_Conn client, QotUpdateBroker.Response rsp){} //推送经纪队列
    default void onPush_UpdatePriceReminder(FTAPI_Conn client, QotUpdatePriceReminder.Response rsp){} //到价提醒通知
}

