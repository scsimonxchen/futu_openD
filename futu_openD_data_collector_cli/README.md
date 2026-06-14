# Stock Lab Collector

CLI application to retrieve market and account data from [Futu OpenD](https://openapi.futunn.com/futu-api-doc/intro/intro.html) using the FTAPI4J SDK. All quote APIs are wrapped via `FutuQuoteClient`; data is persisted to **MySQL** by default for historical analysis and realtime trading.

Part of the [futu_openD](../README.md) workspace. For architecture and package layout, see [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md).

## Prerequisites

1. Install and start **OpenD** on your machine (default `127.0.0.1:11111`).
2. Log in with your Futu/Moomoo platform account and ensure quote permissions for your target markets.
3. **MySQL 8+** running locally (or remote) with a database created, e.g. `CREATE DATABASE stock_lab;`
4. Build the Futu API JAR (if not already present):

```bash
cd FTAPI4J_10.7.6708/ftapi4j
mvn package -DskipTests
```

5. Copy and edit configuration (from the repo root):

```bash
cp futu_openD_data_collector_cli/src/main/resources/config.properties.example config.properties
```

Set `user.id`, MySQL credentials (`mysql.*`), and for trade commands also `trd.acc` and `unlock.trade.pwd.md5`.

Environment variables override config file values: `OPEND_HOST`, `OPEND_PORT`, `FUTU_USER_ID`, `FUTU_TRD_ACC`, `FUTU_UNLOCK_MD5`, `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`.

## Build

From the repo root, build the SDK and collector together (recommended):

```bash
mvn clean install -DskipTests
```

Alternatively, install the prebuilt Futu API JAR into your local Maven repository (once per machine):

```bash
mvn install:install-file \
  -Dfile=FTAPI4J_10.7.6708/lib/futu-api-10.7.6708.jar \
  -DgroupId=com.futunn.openapi \
  -DartifactId=futu-api \
  -Dversion=10.7.6708 \
  -Dpackaging=jar
```

Then build only the collector:

```bash
mvn package -DskipTests -pl futu_openD_data_collector_cli
```

The runnable JAR is at `futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar`.

## Usage

Global options (all commands):

| Option | Default | Description |
|--------|---------|-------------|
| `--config` | `config.properties` | Config file path |
| `--db` | `./data/stock_lab.db` | SQLite path when `--format sqlite` |
| `--format` | `mysql` | `mysql`, `sqlite`, `csv`, or `stdout` |

### Connectivity

```bash
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar ping
```

### Historical sync (analysis)

Bulk-download historical and fundamental data into MySQL:

```bash
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar quote-sync \
  --symbols HK:00700,US:TSLA \
  --from 2020-01-01 --to 2025-12-31 \
  --interval day \
  --apis all
```

API groups for `--apis`: `all`, `kline`, `rehab`, `capital-flow`, `financials`, `research`, `valuation`, `corporate-actions`, `shareholders`, `insider`, `company`, `derivatives`, `market-wide`, `market-screening`, `personalization`, etc.

Recommended production workflow for model training + trading:

```bash
# 1. Backfill training data
java -jar .../stock-lab-collector-1.0.0.jar quote-sync \
  --symbols HK:00700,US:TSLA --from 2018-01-01 --apis all

# 2. Run persistent realtime collector (auto-reconnect)
java -jar .../stock-lab-collector-1.0.0.jar quote-stream \
  --symbols HK:00700,US:TSLA --types basic,rt,orderbook,ticker,broker,kl_1m \
  --pull-interval 5 --reconnect
```

### Realtime streaming (trading)

```bash
# Trading-focused stream with all push types + optional pull polling (auto-reconnect on by default)
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar quote-stream \
  --symbols HK:00700 --types basic,rt,orderbook,ticker,broker,kl_1m \
  --pull-interval 5 --reconnect

# Basic subscribe (same engine, lighter defaults)
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar subscribe \
  --symbols HK:00700 --types basic,rt,kl_1m

# Cancel subscriptions
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar unsubscribe \
  --symbols HK:00700 --types basic,rt
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar unsubscribe --all
```

### Single API pull

```bash
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar quote-pull \
  get-capital-flow --symbol US:TSLA --from 2025-01-01 --to 2025-12-31

java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar quote-pull \
  get-plate-set --market HK --plate-type industry

java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar quote-pull \
  request-history-kl --symbol US:TSLA --from 2020-01-01 --to 2025-12-31 --interval day
```

`quote-pull` supports all documented quote APIs including: `get-global-state`, `get-sub-info`, `request-history-kl`, `get-security-snapshot`, `get-static-info`, `stock-filter`, `get-plate-set`, `get-plate-security`, `get-future-info`, `get-user-security`, `modify-user-security`, `set-price-reminder`, `get-price-reminder`, `get-ipo-list`, and all per-symbol fundamental/derivative APIs.

### Market data (individual commands)

```bash
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar snapshot --symbols HK:00700,US:TSLA
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar history \
  --symbol US:TSLA --interval day --from 2025-01-01 --to 2025-12-31
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar static-info --market HK
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar orderbook --symbol HK:00700
```

### Account data (defaults to simulate env)

```bash
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar accounts
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar funds --market HK
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar positions --market HK
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar orders --market HK
```

## MySQL schema

| Table | Purpose |
|-------|---------|
| `klines` | Historical + realtime K-lines (upsert by symbol/interval/time; push uses real interval e.g. `1m`) |
| `capital_flow` | Normalized daily fund-flow time series for model features |
| `rehab_factors` | Normalized adjustment factors for backtest alignment |
| `quote_api_archive` | All other pull API responses (JSON text) |
| `basic_quotes`, `orderbook`, `realtime_ticks` | Realtime push data |
| `rt_tickers`, `rt_broker_queue` | Tick-by-tick and broker queue pushes |
| `snapshots`, `static_info` | Point-in-time market data |

Tables are auto-created on first connection.

## Symbol format

Use `MARKET:CODE` — e.g. `HK:00700`, `US:TSLA`, `SH:600000`, `SZ:000001`.

## Notes

- `quote-sync` rate-limits API calls (~300 ms between requests) to respect Futu quotas.
- `quote-stream` supports `--reconnect` (default true) for long-running collection; unsubscribes cleanly on shutdown.
- Use `--format sqlite` or `--format csv` to fall back to file-based storage without MySQL.
- Trade commands require a valid unlock password MD5 in config when OpenD enforces trade unlock.

## See also

- [Workspace README](../README.md) — repo overview and quick start
- [Project structure](../PROJECT_STRUCTURE.md) — architecture and package map
- [Futu API doc (Chinese)](../doc/Futu-API-Doc-zh-Java.md)
