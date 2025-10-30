package dev.sai.srs.cli;

import dev.sai.srs.set.WorkingSet;
import dev.sai.srs.set.CompletedSet;
import dev.sai.srs.db.DuckDBManager;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine;

@CommandLine.Command(name = "srs",
        description = "Spaced Repetition System recall practice.",
        mixinStandardHelpOptions = true,
        subcommands = {CompleteCommand.class, AddCommand.class, DeleteCommand.class,
                CommitCommand.class, RecallCommand.class, RollbackCommand.class})
public class SRSCommand implements Runnable{
    public final WorkingSet workingSet;
    public final CompletedSet completedSet;
    public final DuckDBManager db;

    public SRSCommand(WorkingSet workingSet, CompletedSet completedSet, DuckDBManager duckDBManager) {
        this.workingSet = workingSet;
        this.completedSet = completedSet;
        this.db = duckDBManager;
    }

    @CommandLine.Option(names = {"-d", "--debug"}, description = "Prints set and db state", required = false)
    public boolean debug;

    @Override
    public void run() {
        // TODO: add feature to load from csv
        // TODO: add verbose mode, with more text
        // TODO: make welcome screen with state print on root command
        if(debug) Printer.statePrinter(workingSet, completedSet,db);
    }
}
