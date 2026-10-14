package ekw.app;

import ekw.core.Todo;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * The ekw command. Each subcommand is a thin shell over the services in the other modules.
 */
@Command(name = "ekw", mixinStandardHelpOptions = true, version = "ekw 0.1.0-SNAPSHOT (learning skeleton)",
        description = "Engineering knowledge wiki.",
        subcommands = {Main.PingModel.class, Main.Spike.class})
public final class Main implements Callable<Integer> {

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Chapter 7: your first paid model call, capped at one cent by the budget guard.
     */
    @Command(name = "ping-model", description = "Send one tiny request to a model and print the reply, tokens and cost.")
    static final class PingModel implements Callable<Integer> {
        @Option(names = "--model", defaultValue = "claude-haiku-5-5", description = "Model id (default: ${DEFAULT-VALUE}).")
        String model;

        @Option(names = "--limit", defaultValue = "0.01", description = "Budget limit in USD (default: ${DEFAULT-VALUE}).")
        BigDecimal limit;

        @Override
        public Integer call() {
            throw Todo.chapter(7, "Main.PingModel: build the client, the model, the guard; send one message; print");
        }
    }

    /**
     * Chapter 12: spike S1.
     */
    @Command(name = "spike", description = "Spike S1: estimate | run | summarize.")
    static final class Spike implements Callable<Integer> {
        @Parameters(index = "0", description = "estimate, run or summarize")
        String action;

        @Option(names = "--work", defaultValue = ".runs/spike-s1", description = "Working folder (default: ${DEFAULT-VALUE}).")
        Path work;

        @Option(names = "--budget", defaultValue = "5.00", description = "Hard limit in USD (default: ${DEFAULT-VALUE}).")
        BigDecimal budget;

        @Override
        public Integer call() {
            throw Todo.chapter(12, "Main.Spike: build SpikeS1.Config and call its steps");
        }
    }
}
