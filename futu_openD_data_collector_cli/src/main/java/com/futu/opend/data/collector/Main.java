package com.futu.opend.data.collector;

import com.futu.opend.data.collector.cli.AccountsCommand;
import com.futu.opend.data.collector.cli.FundsCommand;
import com.futu.opend.data.collector.cli.HistoryCommand;
import com.futu.opend.data.collector.cli.OrderBookCommand;
import com.futu.opend.data.collector.cli.OrdersCommand;
import com.futu.opend.data.collector.cli.PingCommand;
import com.futu.opend.data.collector.cli.PositionsCommand;
import com.futu.opend.data.collector.cli.SnapshotCommand;
import com.futu.opend.data.collector.cli.StaticInfoCommand;
import com.futu.opend.data.collector.cli.SubscribeCommand;
import com.futu.opend.data.collector.cli.UnsubscribeCommand;
import com.futu.opend.data.collector.cli.QuotePullCommand;
import com.futu.opend.data.collector.cli.QuoteStreamCommand;
import com.futu.opend.data.collector.cli.QuoteSyncCommand;
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
                UnsubscribeCommand.class,
                QuoteSyncCommand.class,
                QuoteStreamCommand.class,
                QuotePullCommand.class,
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
