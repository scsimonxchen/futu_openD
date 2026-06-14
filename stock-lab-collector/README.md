# Stock Lab Collector

CLI application to retrieve market and account data from [Futu OpenD](https://openapi.futunn.com/futu-api-doc/intro/intro.html) using the FTAPI4J SDK.

## Prerequisites

1. Install and start **OpenD** on your machine (default `127.0.0.1:11111`).
2. Log in with your Futu/Moomoo platform account and ensure quote permissions for your target markets.
3. Build the Futu API JAR (if not already present):

```bash
cd FTAPI4J_10.7.6708/ftapi4j
mvn package -DskipTests
```

4. Copy and edit configuration:

```bash
cp stock-lab-collector/src/main/resources/config.properties.example config.properties
```

Set `user.id`, and for trade commands also `trd.acc` and `unlock.trade.pwd.md5`.

Environment variables override config file values: `OPEND_HOST`, `OPEND_PORT`, `FUTU_USER_ID`, `FUTU_TRD_ACC`, `FUTU_UNLOCK_MD5`.

## Build

Install the Futu API JAR into your local Maven repository (once per machine):

```bash
mvn install:install-file \
  -Dfile=FTAPI4J_10.7.6708/lib/futu-api-10.7.6708.jar \
  -DgroupId=com.futu.openapi \
  -DartifactId=futu-api \
  -Dversion=10.7.6708 \
  -Dpackaging=jar
```

Then build the collector:

```bash
# from stock_lab root
mvn package -DskipTests
```

The runnable JAR is at `stock-lab-collector/target/stock-lab-collector-1.0.0.jar`.

## Usage

Global options (all commands):

| Option | Default | Description |
|--------|---------|-------------|
| `--config` | `config.properties` | Config file path |
| `--db` | `./data/stock_lab.db` | SQLite output path |
| `--format` | `sqlite` | `sqlite`, `csv`, or `stdout` |

### Connectivity

```bash
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar ping
```

### Market data (pull)

```bash
# Snapshots
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar snapshot --symbols HK:00700,US:TSLA

# Batch symbols from file
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar snapshot \
  --symbols-file symbols.txt --format csv

# Historical K-lines
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar history \
  --symbol US:TSLA --interval day --from 2025-01-01 --to 2025-12-31

# Static info for a market
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar static-info --market HK

# Order book snapshot
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar orderbook --symbol HK:00700

# Watch live order book for 60 seconds
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar orderbook \
  --symbol HK:00700 --watch --duration 60
```

### Real-time streaming

```bash
# Stream until Ctrl+C
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar subscribe \
  --symbols HK:00700,HK:01810 --types basic,rt,kl_1m

# Stream for 5 minutes
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar subscribe \
  --symbols HK:00700 --types rt,orderbook --duration 300
```

### Account data (defaults to simulate env)

```bash
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar accounts
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar funds --market HK
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar positions --market HK
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar orders --market HK
java -jar stock-lab-collector/target/stock-lab-collector-1.0.0.jar orders --market HK --history
```

Use `--env real` only when you intend to query a live account.

## Symbol format

Use `MARKET:CODE` — e.g. `HK:00700`, `US:TSLA`, `SH:600000`, `SZ:000001`.

Symbols files accept one symbol per line; lines starting with `#` are ignored.

## Output

- **sqlite** (default): data written to `--db` path with tables for snapshots, klines, orderbook, realtime_ticks, accounts, funds, positions, orders, etc.
- **csv**: rows appended to CSV files in the directory containing the `--db` path
- **stdout**: human-readable summary printed to the terminal

## Notes

- Snapshot requests are batched in groups of 200 with a 3-second pause between batches (Futu rate limits).
- `subscribe` and `orderbook --watch` block until duration elapses or you press Ctrl+C.
- Trade commands require a valid unlock password MD5 in config when OpenD enforces trade unlock.
