package ansrs.cli;

import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.db.DBManager;
import ansrs.util.Printer;
import picocli.CommandLine.*;

@Command(name = "ansrs",
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

    @Option(names = {"-l", "--list"}, description = "Lists set and db state", required = false)
    public boolean list;

    @Override
    public void run() {
       if(list) Printer.statePrinter(workingSet, completedSet,db);
    }
}
