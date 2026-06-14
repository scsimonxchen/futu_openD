package com.stocklab.collector.storage;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

public class SqliteStore implements DataStore {
    private final Connection conn;
    private final String fetchedAt = Instant.now().toString();

    public SqliteStore(Path dbPath) {
        try {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            initSchema();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open SQLite database: " + dbPath, e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS global_state (" +
                    "fetched_at TEXT, qot_logined INTEGER, trd_logined INTEGER, server_ver TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS snapshots (" +
                    "fetched_at TEXT, market TEXT, code TEXT, cur_price REAL, open_price REAL, " +
                    "high_price REAL, low_price REAL, volume INTEGER, turnover REAL, market_state INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS klines (" +
                    "fetched_at TEXT, market TEXT, code TEXT, interval_type TEXT, time_key TEXT, " +
                    "open_price REAL, close_price REAL, high_price REAL, low_price REAL, volume INTEGER, turnover REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS static_info (" +
                    "fetched_at TEXT, market TEXT, code TEXT, name TEXT, lot_size INTEGER, sec_type INTEGER, " +
                    "list_time TEXT, delisting INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS orderbook (" +
                    "fetched_at TEXT, market TEXT, code TEXT, side TEXT, position INTEGER, price REAL, volume INTEGER, order_count INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS realtime_ticks (" +
                    "fetched_at TEXT, market TEXT, code TEXT, time_key TEXT, price REAL, volume INTEGER, turnover REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS basic_quotes (" +
                    "fetched_at TEXT, market TEXT, code TEXT, cur_price REAL, open_price REAL, high_price REAL, " +
                    "low_price REAL, volume INTEGER, turnover REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS accounts (" +
                    "fetched_at TEXT, acc_id INTEGER, trd_env INTEGER, acc_type INTEGER, card_num TEXT, " +
                    "security_firm INTEGER, market_auth_list TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS funds (" +
                    "fetched_at TEXT, acc_id INTEGER, power REAL, max_power_short REAL, net_cash_power REAL, " +
                    "total_assets REAL, securities_assets REAL, fund_assets REAL, bond_assets REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS positions (" +
                    "fetched_at TEXT, acc_id INTEGER, code TEXT, name TEXT, qty REAL, can_sell_qty REAL, " +
                    "price REAL, cost_price REAL, pl_val REAL, pl_ratio REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "fetched_at TEXT, acc_id INTEGER, is_history INTEGER, order_id TEXT, code TEXT, name TEXT, " +
                    "trd_side INTEGER, order_type INTEGER, order_status INTEGER, qty REAL, price REAL, " +
                    "create_time TEXT, updated_time TEXT)");
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
                "close_price, high_price, low_price, volume, turnover) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
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
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close database", e);
        }
    }
}
