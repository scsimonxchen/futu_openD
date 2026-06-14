package com.stocklab.collector.storage;

import com.futu.openapi.pb.QotUpdateBroker;
import com.futu.openapi.pb.QotUpdateTicker;
import com.google.protobuf.GeneratedMessageV3;
import com.stocklab.collector.config.MySqlConfig;

import com.futu.openapi.pb.GetGlobalState;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetSecuritySnapshot;
import com.futu.openapi.pb.QotGetStaticInfo;
import com.futu.openapi.pb.QotRequestHistoryKL;
import com.futu.openapi.pb.QotUpdateBasicQot;
import com.futu.openapi.pb.QotUpdateKL;
import com.futu.openapi.pb.QotUpdateOrderBook;
import com.futu.openapi.pb.QotUpdateRT;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetAccList;
import com.futu.openapi.pb.TrdGetFunds;
import com.futu.openapi.pb.TrdGetHistoryOrderList;
import com.futu.openapi.pb.TrdGetOrderList;
import com.futu.openapi.pb.TrdGetPositionList;
import com.stocklab.collector.util.SymbolParser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

public class MySqlStore implements DataStore {
    private final Connection conn;
    private final String fetchedAt = Instant.now().toString();

    public MySqlStore(MySqlConfig config) {
        try {
            conn = DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());
            initSchema();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open MySQL database: " + config.getDatabase(), e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS global_state (" +
                    "fetched_at DATETIME(3), qot_logined INT, trd_logined INT, server_ver VARCHAR(32))");
            stmt.execute("CREATE TABLE IF NOT EXISTS snapshots (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), cur_price DOUBLE, open_price DOUBLE, " +
                    "high_price DOUBLE, low_price DOUBLE, volume BIGINT, turnover DOUBLE, market_state INT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS klines (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), interval_type VARCHAR(16), time_key VARCHAR(32), " +
                    "open_price DOUBLE, close_price DOUBLE, high_price DOUBLE, low_price DOUBLE, volume BIGINT, turnover DOUBLE, " +
                    "UNIQUE KEY uq_kline (market, code, interval_type, time_key))");
            stmt.execute("CREATE TABLE IF NOT EXISTS static_info (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), name VARCHAR(128), lot_size INT, sec_type INT, " +
                    "list_time VARCHAR(32), delisting INT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS orderbook (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), side VARCHAR(8), position INT, price DOUBLE, volume BIGINT, order_count INT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS realtime_ticks (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), time_key VARCHAR(32), price DOUBLE, volume BIGINT, turnover DOUBLE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS basic_quotes (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), cur_price DOUBLE, open_price DOUBLE, high_price DOUBLE, " +
                    "low_price DOUBLE, volume BIGINT, turnover DOUBLE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS accounts (" +
                    "fetched_at DATETIME(3), acc_id BIGINT, trd_env INT, acc_type INT, card_num VARCHAR(64), " +
                    "security_firm INT, market_auth_list TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS funds (" +
                    "fetched_at DATETIME(3), acc_id BIGINT, power DOUBLE, max_power_short DOUBLE, net_cash_power DOUBLE, " +
                    "total_assets DOUBLE, securities_assets DOUBLE, fund_assets DOUBLE, bond_assets DOUBLE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS positions (" +
                    "fetched_at DATETIME(3), acc_id BIGINT, code VARCHAR(32), name VARCHAR(128), qty DOUBLE, can_sell_qty DOUBLE, " +
                    "price DOUBLE, cost_price DOUBLE, pl_val DOUBLE, pl_ratio DOUBLE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "fetched_at DATETIME(3), acc_id BIGINT, is_history INT, order_id VARCHAR(64), code VARCHAR(32), name VARCHAR(128), " +
                    "trd_side INT, order_type INT, order_status INT, qty DOUBLE, price DOUBLE, " +
                    "create_time VARCHAR(32), updated_time VARCHAR(32))");
            stmt.execute("CREATE TABLE IF NOT EXISTS quote_api_archive (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, api_name VARCHAR(64) NOT NULL, " +
                    "entity_key VARCHAR(128), fetched_at DATETIME(3) NOT NULL, response_data LONGTEXT NOT NULL, " +
                    "INDEX idx_api_entity (api_name, entity_key), INDEX idx_fetched (fetched_at))");
            stmt.execute("CREATE TABLE IF NOT EXISTS rt_tickers (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), time_key VARCHAR(32), " +
                    "price DOUBLE, volume BIGINT, turnover DOUBLE, direction INT, " +
                    "INDEX idx_rt_ticker (market, code, time_key))");
            stmt.execute("CREATE TABLE IF NOT EXISTS rt_broker_queue (" +
                    "fetched_at DATETIME(3), market VARCHAR(16), code VARCHAR(32), side VARCHAR(8), " +
                    "position INT, broker_id BIGINT, broker_name VARCHAR(64), " +
                    "INDEX idx_broker (market, code, fetched_at))");
        }
    }

    @Override
    public void saveGlobalState(GetGlobalState.Response rsp) {
        if (!rsp.hasS2C()) {
            return;
        }
        String sql = "INSERT INTO global_state (fetched_at, qot_logined, trd_logined, server_ver) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fetchedAt);
            ps.setInt(2, rsp.getS2C().getQotLogined() ? 1 : 0);
            ps.setInt(3, rsp.getS2C().getTrdLogined() ? 1 : 0);
            ps.setString(4, String.valueOf(rsp.getS2C().getServerVer()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save global state", e);
        }
    }

    @Override
    public void saveSnapshots(List<QotGetSecuritySnapshot.Snapshot> snapshots) {
        String sql = "INSERT INTO snapshots (fetched_at, market, code, cur_price, open_price, high_price, " +
                "low_price, volume, turnover, market_state) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QotGetSecuritySnapshot.Snapshot snap : snapshots) {
                QotCommon.Security sec = snap.getBasic().getSecurity();
                ps.setString(1, fetchedAt);
                ps.setString(2, SymbolParser.marketName(sec.getMarket()));
                ps.setString(3, sec.getCode());
                ps.setDouble(4, snap.getBasic().getCurPrice());
                ps.setDouble(5, snap.getBasic().getOpenPrice());
                ps.setDouble(6, snap.getBasic().getHighPrice());
                ps.setDouble(7, snap.getBasic().getLowPrice());
                ps.setLong(8, snap.getBasic().getVolume());
                ps.setDouble(9, snap.getBasic().getTurnover());
                ps.setInt(10, snap.getBasic().getSecStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save snapshots", e);
        }
    }

    @Override
    public void saveKlines(QotCommon.Security security, String interval, List<QotCommon.KLine> klines) {
        String sql = "INSERT INTO klines (fetched_at, market, code, interval_type, time_key, open_price, " +
                "close_price, high_price, low_price, volume, turnover) VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE fetched_at=VALUES(fetched_at), open_price=VALUES(open_price), " +
                "close_price=VALUES(close_price), high_price=VALUES(high_price), low_price=VALUES(low_price), " +
                "volume=VALUES(volume), turnover=VALUES(turnover)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QotCommon.KLine kl : klines) {
                ps.setString(1, fetchedAt);
                ps.setString(2, SymbolParser.marketName(security.getMarket()));
                ps.setString(3, security.getCode());
                ps.setString(4, interval);
                ps.setString(5, kl.getTime());
                ps.setDouble(6, kl.getOpenPrice());
                ps.setDouble(7, kl.getClosePrice());
                ps.setDouble(8, kl.getHighPrice());
                ps.setDouble(9, kl.getLowPrice());
                ps.setLong(10, kl.getVolume());
                ps.setDouble(11, kl.getTurnover());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save klines", e);
        }
    }

    @Override
    public void saveStaticInfo(List<QotCommon.SecurityStaticInfo> infos) {
        String sql = "INSERT INTO static_info (fetched_at, market, code, name, lot_size, sec_type, list_time, delisting) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QotCommon.SecurityStaticInfo info : infos) {
                QotCommon.Security sec = info.getBasic().getSecurity();
                ps.setString(1, fetchedAt);
                ps.setString(2, SymbolParser.marketName(sec.getMarket()));
                ps.setString(3, sec.getCode());
                ps.setString(4, info.getBasic().getName());
                ps.setInt(5, info.getBasic().getLotSize());
                ps.setInt(6, info.getBasic().getSecType());
                ps.setString(7, info.getBasic().getListTime());
                ps.setInt(8, info.getBasic().getDelisting() ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save static info", e);
        }
    }

    @Override
    public void saveOrderBook(QotCommon.Security security, QotUpdateOrderBook.Response rsp) {
        if (!rsp.hasS2C()) {
            return;
        }
        String sql = "INSERT INTO orderbook (fetched_at, market, code, side, position, price, volume, order_count) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            insertOrderBookLevels(ps, security, "ask", rsp.getS2C().getOrderBookAskListList());
            insertOrderBookLevels(ps, security, "bid", rsp.getS2C().getOrderBookBidListList());
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save order book", e);
        }
    }

    private void insertOrderBookLevels(PreparedStatement ps, QotCommon.Security security, String side,
                                       List<QotCommon.OrderBook> levels) throws SQLException {
        for (int i = 0; i < levels.size(); i++) {
            QotCommon.OrderBook level = levels.get(i);
            ps.setString(1, Instant.now().toString());
            ps.setString(2, SymbolParser.marketName(security.getMarket()));
            ps.setString(3, security.getCode());
            ps.setString(4, side);
            ps.setInt(5, i);
            ps.setDouble(6, level.getPrice());
            ps.setLong(7, level.getVolume());
            ps.setInt(8, level.getOrederCount());
            ps.addBatch();
        }
    }

    @Override
    public void saveRealtimeTick(QotUpdateRT.Response rsp) {
        if (!rsp.hasS2C() || rsp.getS2C().getRtListCount() == 0) {
            return;
        }
        QotCommon.Security sec = rsp.getS2C().getSecurity();
        String sql = "INSERT INTO realtime_ticks (fetched_at, market, code, time_key, price, volume, turnover) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QotCommon.TimeShare rt : rsp.getS2C().getRtListList()) {
                ps.setString(1, Instant.now().toString());
                ps.setString(2, SymbolParser.marketName(sec.getMarket()));
                ps.setString(3, sec.getCode());
                ps.setString(4, rt.getTime());
                ps.setDouble(5, rt.getPrice());
                ps.setLong(6, rt.getVolume());
                ps.setDouble(7, rt.getTurnover());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save realtime ticks", e);
        }
    }

    @Override
    public void saveBasicQuote(QotUpdateBasicQot.Response rsp) {
        if (!rsp.hasS2C() || rsp.getS2C().getBasicQotListCount() == 0) {
            return;
        }
        String sql = "INSERT INTO basic_quotes (fetched_at, market, code, cur_price, open_price, high_price, " +
                "low_price, volume, turnover) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QotCommon.BasicQot q : rsp.getS2C().getBasicQotListList()) {
                QotCommon.Security sec = q.getSecurity();
                ps.setString(1, Instant.now().toString());
                ps.setString(2, SymbolParser.marketName(sec.getMarket()));
                ps.setString(3, sec.getCode());
                ps.setDouble(4, q.getCurPrice());
                ps.setDouble(5, q.getOpenPrice());
                ps.setDouble(6, q.getHighPrice());
                ps.setDouble(7, q.getLowPrice());
                ps.setLong(8, q.getVolume());
                ps.setDouble(9, q.getTurnover());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save basic quote", e);
        }
    }

    @Override
    public void saveKlinePush(QotUpdateKL.Response rsp) {
        if (!rsp.hasS2C()) {
            return;
        }
        saveKlines(rsp.getS2C().getSecurity(), "push", rsp.getS2C().getKlListList());
    }

    @Override
    public void saveAccounts(TrdGetAccList.Response rsp) {
        if (!rsp.hasS2C()) {
            return;
        }
        String sql = "INSERT INTO accounts (fetched_at, acc_id, trd_env, acc_type, card_num, security_firm, market_auth_list) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TrdCommon.TrdAcc acc : rsp.getS2C().getAccListList()) {
                ps.setString(1, fetchedAt);
                ps.setLong(2, acc.getAccID());
                ps.setInt(3, acc.getTrdEnv());
                ps.setInt(4, acc.getAccType());
                ps.setString(5, acc.getCardNum());
                ps.setInt(6, acc.getSecurityFirm());
                ps.setString(7, acc.getTrdMarketAuthListList().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save accounts", e);
        }
    }

    @Override
    public void saveFunds(TrdGetFunds.Response rsp, long accId) {
        if (!rsp.hasS2C() || !rsp.getS2C().hasFunds()) {
            return;
        }
        TrdCommon.Funds f = rsp.getS2C().getFunds();
        String sql = "INSERT INTO funds (fetched_at, acc_id, power, max_power_short, net_cash_power, total_assets, " +
                "securities_assets, fund_assets, bond_assets) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fetchedAt);
            ps.setLong(2, accId);
            ps.setDouble(3, f.getPower());
            ps.setDouble(4, f.getMaxPowerShort());
            ps.setDouble(5, f.getNetCashPower());
            ps.setDouble(6, f.getTotalAssets());
            ps.setDouble(7, f.getSecuritiesAssets());
            ps.setDouble(8, f.getFundAssets());
            ps.setDouble(9, f.getBondAssets());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save funds", e);
        }
    }

    @Override
    public void savePositions(TrdGetPositionList.Response rsp, long accId) {
        if (!rsp.hasS2C()) {
            return;
        }
        String sql = "INSERT INTO positions (fetched_at, acc_id, code, name, qty, can_sell_qty, price, cost_price, " +
                "pl_val, pl_ratio) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TrdCommon.Position pos : rsp.getS2C().getPositionListList()) {
                ps.setString(1, fetchedAt);
                ps.setLong(2, accId);
                ps.setString(3, pos.getCode());
                ps.setString(4, pos.getName());
                ps.setDouble(5, pos.getQty());
                ps.setDouble(6, pos.getCanSellQty());
                ps.setDouble(7, pos.getPrice());
                ps.setDouble(8, pos.getCostPrice());
                ps.setDouble(9, pos.getPlVal());
                ps.setDouble(10, pos.getPlRatio());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save positions", e);
        }
    }

    @Override
    public void saveOrders(TrdGetOrderList.Response rsp, long accId, boolean history) {
        if (!rsp.hasS2C()) {
            return;
        }
        insertOrders(accId, history ? 1 : 0, rsp.getS2C().getOrderListList());
    }

    @Override
    public void saveHistoryOrders(TrdGetHistoryOrderList.Response rsp, long accId) {
        if (!rsp.hasS2C()) {
            return;
        }
        insertOrders(accId, 1, rsp.getS2C().getOrderListList());
    }

    private void insertOrders(long accId, int isHistory, List<TrdCommon.Order> orders) {
        String sql = "INSERT INTO orders (fetched_at, acc_id, is_history, order_id, code, name, trd_side, " +
                "order_type, order_status, qty, price, create_time, updated_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TrdCommon.Order order : orders) {
                ps.setString(1, fetchedAt);
                ps.setLong(2, accId);
                ps.setInt(3, isHistory);
                ps.setString(4, order.getOrderIDEx());
                ps.setString(5, order.getCode());
                ps.setString(6, order.getName());
                ps.setInt(7, order.getTrdSide());
                ps.setInt(8, order.getOrderType());
                ps.setInt(9, order.getOrderStatus());
                ps.setDouble(10, order.getQty());
                ps.setDouble(11, order.getPrice());
                ps.setString(12, order.getCreateTime());
                ps.setString(13, order.getUpdateTime());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save orders", e);
        }
    }


    @Override
    public void saveQuoteArchive(String apiName, String entityKey, GeneratedMessageV3 response) {
        String sql = "INSERT INTO quote_api_archive (api_name, entity_key, fetched_at, response_data) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, apiName);
            ps.setString(2, entityKey);
            ps.setString(3, fetchedAt);
            ps.setString(4, QuoteResponseSerializer.toJson(response));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save quote archive: " + apiName, e);
        }
    }

    @Override
    public void saveTickerPush(QotUpdateTicker.Response rsp) {
        if (!rsp.hasS2C()) return;
        QotCommon.Security sec = rsp.getS2C().getSecurity();
        String sql = "INSERT INTO rt_tickers (fetched_at, market, code, time_key, price, volume, turnover, direction) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QotCommon.Ticker t : rsp.getS2C().getTickerListList()) {
                ps.setString(1, Instant.now().toString());
                ps.setString(2, SymbolParser.marketName(sec.getMarket()));
                ps.setString(3, sec.getCode());
                ps.setString(4, t.getTime());
                ps.setDouble(5, t.getPrice());
                ps.setLong(6, t.getVolume());
                ps.setDouble(7, t.getTurnover());
                ps.setInt(8, t.getDir());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save ticker push", e);
        }
    }

    @Override
    public void saveBrokerPush(QotUpdateBroker.Response rsp) {
        if (!rsp.hasS2C()) return;
        QotCommon.Security sec = rsp.getS2C().getSecurity();
        String sql = "INSERT INTO rt_broker_queue (fetched_at, market, code, side, position, broker_id, broker_name) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            insertBrokerLevels(ps, sec, "ask", rsp.getS2C().getBrokerAskListList());
            insertBrokerLevels(ps, sec, "bid", rsp.getS2C().getBrokerBidListList());
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save broker push", e);
        }
    }

    private void insertBrokerLevels(PreparedStatement ps, QotCommon.Security security, String side,
                                    java.util.List<QotCommon.Broker> levels) throws SQLException {
        for (int i = 0; i < levels.size(); i++) {
            QotCommon.Broker b = levels.get(i);
            ps.setString(1, Instant.now().toString());
            ps.setString(2, SymbolParser.marketName(security.getMarket()));
            ps.setString(3, security.getCode());
            ps.setString(4, side);
            ps.setInt(5, i);
            ps.setLong(6, b.getId());
            ps.setString(7, b.getName());
            ps.addBatch();
        }
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close database", e);
        }
    }
}
