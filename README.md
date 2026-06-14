# futu_openD

Java Maven workspace for collecting market and trading data from [Futu OpenD](https://openapi.futunn.com/futu-api-doc/intro/intro.html) using the FTAPI4J SDK.

The main application is **Stock Lab Collector** — a CLI that pulls quote and account data from OpenD and persists it to **MySQL** (default), **SQLite**, **CSV**, or **stdout**.

## Repository layout

```
futu_openD/
├── README.md                        # This file
├── PROJECT_STRUCTURE.md             # Detailed architecture and package map
├── pom.xml                          # Parent Maven POM
├── doc/
│   └── Futu-API-Doc-zh-Java.md      # Futu API reference (Chinese)
├── FTAPI4J_10.7.6708/               # Vendored Futu OpenAPI Java SDK (v10.7.6708)
│   ├── ftapi4j/                     # SDK source
│   ├── sample/                      # Official Futu demos
│   └── lib/                         # Prebuilt JAR (install to local Maven)
└── futu_openD_data_collector_cli/   # CLI application module
    ├── README.md                    # Usage guide and command examples
    └── src/main/java/com/stocklab/collector/
```

## Components

| Component | Description |
|-----------|-------------|
| **`futu_openD_data_collector_cli`** | Runnable CLI (`stock-lab-collector`) — quote sync, realtime streaming, account queries |
| **`FTAPI4J_10.7.6708`** | Official Futu Java SDK (protobuf + API wrappers); built/installed separately |
| **`doc/`** | API documentation |

## Quick start

### Prerequisites

1. **OpenD** installed, running, and logged in (default `127.0.0.1:11111`)
2. **Java 8+** and **Maven**
3. **MySQL 8+** (optional; default storage backend) — e.g. `CREATE DATABASE stock_lab;`

### Build

Build the Futu API JAR if needed:

```bash
cd FTAPI4J_10.7.6708/ftapi4j
mvn package -DskipTests
```

Install the Futu API into your local Maven repository (once per machine, if not using the reactor build above):

```bash
mvn install:install-file \
  -Dfile=FTAPI4J_10.7.6708/lib/futu-api-10.7.6708.jar \
  -DgroupId=com.futunn.openapi \
  -DartifactId=futu-api \
  -Dversion=10.7.6708 \
  -Dpackaging=jar
```

Build the collector from the repo root:

```bash
mvn clean install -DskipTests
```

Output: `futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar`

### Configure

Copy the example config to the repo root and edit it:

```bash
cp futu_openD_data_collector_cli/src/main/resources/config.properties.example config.properties
```

Set `user.id`, MySQL credentials (`mysql.*`), and for trade commands also `trd.acc` and `unlock.trade.pwd.md5`.

### Verify connectivity

```bash
java -jar futu_openD_data_collector_cli/target/stock-lab-collector-1.0.0.jar ping
```

## CLI overview

| Category | Commands |
|----------|----------|
| Connectivity | `ping` |
| Market data | `snapshot`, `history`, `static-info`, `orderbook` |
| Bulk / streaming | `quote-sync`, `quote-stream`, `quote-pull`, `subscribe` |
| Account / trade | `accounts`, `funds`, `positions`, `orders` |

Global options: `--config`, `--format` (`mysql` \| `sqlite` \| `csv` \| `stdout`), `--db`

**Symbol format:** `MARKET:CODE` — e.g. `HK:00700`, `US:TSLA`, `SH:600000`

For full usage examples, see [`futu_openD_data_collector_cli/README.md`](futu_openD_data_collector_cli/README.md).

## Architecture

```
CLI (PicoCLI) → Services → FutuQuoteClient / FutuTradeClient → OpenD
                         → DataStore → MySQL / SQLite / CSV / stdout
```

Package layers: `cli` → `service` → `client` / `storage`, with `config` and `util` support.

See [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md) for the full package map, data flow diagram, and MySQL schema.

## Documentation

| Document | Contents |
|----------|----------|
| [`futu_openD_data_collector_cli/README.md`](futu_openD_data_collector_cli/README.md) | Command examples, MySQL tables, configuration |
| [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md) | Directory tree, architecture, class inventory |
| [`doc/Futu-API-Doc-zh-Java.md`](doc/Futu-API-Doc-zh-Java.md) | Futu OpenAPI reference (Chinese) |
| [Futu OpenAPI (English)](https://openapi.futunn.com/futu-api-doc/intro/intro.html) | Official online docs |

## Tech stack

- Java 8, Maven (multi-module)
- [PicoCLI](https://picocli.info/) 4.7.5
- Futu FTAPI4J 10.7.6708, Protobuf 3.5.1
- MySQL 8 / SQLite / CSV output
- Maven Shade (fat JAR)
