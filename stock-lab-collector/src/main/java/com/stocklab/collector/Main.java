package com.stocklab.collector;

import com.stocklab.collector.cli.AccountsCommand;
import com.stocklab.collector.cli.FundsCommand;
import com.stocklab.collector.cli.HistoryCommand;
import com.stocklab.collector.cli.OrderBookCommand;
import com.stocklab.collector.cli.OrdersCommand;
import com.stocklab.collector.cli.PingCommand;
import com.stocklab.collector.cli.PositionsCommand;
import com.stocklab.collector.cli.SnapshotCommand;
import com.stocklab.collector.cli.StaticInfoCommand;
import com.stocklab.collector.cli.SubscribeCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "stock-lab-collector",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Futu OpenD CLI data collector",
        subcommands = {
                PingCommand.class,
                SnapshotCommand.class,
                HistoryCommand.class,
                StaticInfoCommand.class,
                OrderBookCommand.class,
                SubscribeCommand.class,
                AccountsCommand.class,
                FundsCommand.class,
                PositionsCommand.class,
                OrdersCommand.class
        }
)
public class Main implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
