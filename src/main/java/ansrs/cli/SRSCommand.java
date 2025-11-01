package ansrs.cli;

import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.db.DBManager;
import ansrs.util.Printer;
import picocli.CommandLine;

@CommandLine.Command(name = "ansrs",
        description = "AnSRS (Pronounced \"Answers\") is a spaced repetition system.", version = """
        AnSRS version 1.0.0 2025-11-01
        """,
        mixinStandardHelpOptions = true,
        subcommands = {AddCommand.class, CompleteCommand.class, DeleteCommand.class,
                CommitCommand.class, RecallCommand.class, RollbackCommand.class, ImportCommand.class})
public class SRSCommand implements Runnable{
    public final WorkingSet workingSet;
    public final CompletedSet completedSet;
    public final DBManager db;

    public SRSCommand(WorkingSet workingSet, CompletedSet completedSet, DBManager DBManager) {
        this.workingSet = workingSet;
        this.completedSet = completedSet;
        this.db = DBManager;
    }

    @CommandLine.Option(names = {"-d", "--debug"}, description = "Prints set and db state", required = false)
    public boolean debug;

    @Override
    public void run() {
        // TODO: standardize logging
        // TODO: make welcome screen with state print on root command
        if(debug) Printer.statePrinter(workingSet, completedSet,db);
    }
}
