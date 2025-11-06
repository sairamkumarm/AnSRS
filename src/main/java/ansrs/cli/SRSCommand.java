package ansrs.cli;

import ansrs.db.ArchiveManager;
import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.db.DBManager;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(name = "ansrs",
        description = "AnSRS (Pronounced \"Answers\") is a spaced repetition system.", version = """
        AnSRS version 1.2.0
        """,
        mixinStandardHelpOptions = true,
        subcommands = {AddCommand.class, CompleteCommand.class, DeleteCommand.class,
                CommitCommand.class, RecallCommand.class, RollbackCommand.class, ImportCommand.class, ArchiveCommand.class})
public class SRSCommand implements Callable<Integer> {
    public final WorkingSet workingSet;
    public final CompletedSet completedSet;
    public final DBManager itemDB;
    public final ArchiveManager archiveManager;

    public SRSCommand(WorkingSet workingSet, CompletedSet completedSet, DBManager DBManager, ArchiveManager archiveManager) {
        this.workingSet = workingSet;
        this.completedSet = completedSet;
        this.itemDB = DBManager;
        this.archiveManager = archiveManager;
    }

    @Spec
    private Model.CommandSpec spec;

    @Option(names = {"-l", "--list"}, description = "Lists set and db state", required = false)
    private boolean list;

    @Option(names = {"-s", "--set"}, description = "Use this flag with --list to print only set", required = false)
    private boolean set;

    @Option(names = {"-i", "--id"}, paramLabel = "ITEM_ID", description = "Print a specific Item", required = false, defaultValue = "-12341234")
    private int itemId;

    @Option(names = {"-n", "--name"}, paramLabel = "ITEM_NAME_QUERY", description = "Find an Item by it's name, query must be longer than one character", required = false, defaultValue = "zyxwvutsrqp")
    private String itemName;

    @Override
    public Integer call() {
        validate();
        if (itemId != -12341234) {
            System.out.println("Item:");
            Printer.printItem(itemDB.getItemById(itemId).orElse(null));
        }
        if (!itemName.equals("zyxwvutsrqp")) {
            System.out.println("Search: " + itemName);
            Printer.printItemsList(itemDB.searchItemsByName(itemName.trim()).orElse(new ArrayList<>()));
        }
        if (list) {
            if (set) {
                Printer.setStatePrinter(workingSet, completedSet, itemDB);
            } else {
                Printer.statePrinter(workingSet, completedSet, itemDB);
            }
        }
        return 0;
    }

    private void validate() {
        if (itemId != -12341234) {
            if (itemId <= 0)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID cannot be non-positive"));
        }
        if (!itemName.equals("zyxwvutsrqp")) {
            if (itemName.isBlank())
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_NAME_QUERY cannot be empty, to see all Items, use --list flag"));
            if (itemName.trim().length()==1)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_NAME_QUERY cannot be of size 1, too broad"));
        }
        if (set && !list)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--set is to be used along with --list, to display only Set statuses"));
    }
}
