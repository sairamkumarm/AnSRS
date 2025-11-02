package ansrs.cli;

import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

@Command(name = "delete",
        description = "Remove from WorkingSet, CompletedSet and db, depending on the flags, by default it removes from WorkingSet",
        mixinStandardHelpOptions = true)
public class DeleteCommand implements Runnable {

    @ParentCommand
    private SRSCommand parent;

    @Spec
    private Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "ITEM_ID", description = "Unique identifier of an item", defaultValue = "0")
    private int itemId;

    @Option(names = {"-c", "--completed"}, description = "Removes from items that are completed but, are yet to be commited to the database.")
    private boolean deleteFromCompletedSet;

    @Option(names = {"-d", "--database"}, description = "Removes a item from the database and sets")
    private boolean deleteFromDatabase;

    @Option(names = "--sure", description = "A defensive fallback to prevent accidental deletions", required = true)
    private boolean sure;

    @Option(names = {"--hard-reset"}, description = "Hard resets all the persistent data, sets included")
    private boolean reset;

    @Override
    public void run() {
        if (!sure) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag is needed to perform deletions."));
        }
        if (itemId == 0) {
            if (reset) {
                if (
                        parent.db.clearDatabase() &&
                                parent.completedSet.clearSet() &&
                                parent.workingSet.clearSet()
                )
                    Log.info("Reset successful");
                else Log.error("Complete Reset failed, use --list flag to check status");
                return;
            } else
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID can be skipped only during the --hard-reset command"));
        }

        if (itemId < 0) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] cannot be non-positive"));
        }
        if (deleteFromDatabase) {
            if (!parent.db.deleteItemsById(itemId)) {
                Log.error("Error in deleting ITEM_ID [" + itemId + "] from DB, use --list to confirm its existence");
                return;
            }
            boolean cs = parent.completedSet.removeItem(itemId);
            boolean ws = parent.workingSet.removeItem(itemId);
            Log.info("ITEM_ID [" + itemId + "] deleted from the database" + ((cs)?", and the CompletedSet.":"") + ((ws)?", and the WorkingSet.":"")+((!ws&&!cs)?".":""));
        } else if (deleteFromCompletedSet) {
            if (!parent.completedSet.removeItem(itemId)) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] non-existent in CompletedSet"));
            } else {
                Log.info("ITEM_ID [" + itemId + "] deleted from CompletedSet");
            }
        } else {
            if (!parent.workingSet.removeItem(itemId)) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] non-existent in WorkingSet"));
            } else {
                Log.info("ITEM_ID [" + itemId + "] deleted from WorkingSet");
            }
        }

        if (parent.list) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);

    }

}
