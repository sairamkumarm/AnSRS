package dev.sai.srs.cli;

import dev.sai.srs.cache.SessionCache;
import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.data.Problem;
import dev.sai.srs.db.DuckDBManager;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "srs",
        description = "Spaced Repetition System recall practice.",
        mixinStandardHelpOptions = true,
        subcommands = {CompleteCommand.class, AddCommand.class, DeleteCommand.class,
                CommitCommand.class, RecallCommand.class, RollbackCommand.class})
public class SRSCommand implements Runnable{
    public final SessionCache sessionCache;
    public final UpdateCache updateCache;
    public final DuckDBManager db;

    public SRSCommand(SessionCache sessionCache, UpdateCache updateCache, DuckDBManager duckDBManager) {
        this.sessionCache = sessionCache;
        this.updateCache = updateCache;
        this.db = duckDBManager;
    }

    @CommandLine.Option(names = {"-d", "--debug"}, description = "Prints cache and db state", required = false)
    public boolean debug;

    @Override
    public void run() {
        // TODO: add feature to load from csv
        // TODO: add verbose mode, with more text
        // TODO: make welcome screen with state print on root command
        if(debug) Printer.statePrinter(sessionCache,updateCache,db);
    }
}
