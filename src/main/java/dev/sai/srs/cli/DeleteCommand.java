package dev.sai.srs.cli;

import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

@Command(name = "delete",
        description = "delete lets you remove from WorkingSet, CompletedSet and db, depending on the flags, by default it removes from WorkingSet",
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
            throw new ParameterException(spec.commandLine(),"--sure flag is needed to perform deletions.");
        }
        if (itemId==0){
            if (reset) {
                if (
                parent.db.clearDatabase()&&
                parent.completedSet.clearSet()&&
                parent.workingSet.clearSet()
                )
                    System.out.println("Reset successful");
                else System.err.println("Reset failed");
                return;
            }
            else throw new ParameterException(spec.commandLine(),"ITEM_ID can be skipped only during the --reset command");
        }

        if (itemId < 0) {
            throw new ParameterException(spec.commandLine(),"ITEM_ID ["+itemId+"] cannot be non-positive");
        }
        if (deleteFromDatabase) {
            if (!parent.db.deleteItemsById(itemId)) {
                System.err.println("Error in deleting ITEM_ID ["+itemId+"] from DB, use --debug to confirm its existence");
                return;
            }
            System.out.println("ITEM_ID ["+itemId+"] deleted from database");
        }
        if (deleteFromCompletedSet) {
            if (!parent.completedSet.removeItem(itemId)) {
                throw new ParameterException(spec.commandLine(),"ITEM_ID ["+itemId+"] non-existent in CompletedSet");
            } else {
                System.out.println("ITEM_ID ["+itemId+"] deleted from CompletedSet");
            }
        }
        if (!parent.workingSet.removeItem(itemId)) {
            throw new ParameterException(spec.commandLine(),"ITEM_ID ["+itemId+"] non-existent in WorkingSet");
        } else {
            System.out.println("ITEM_ID ["+itemId+"] deleted from WorkingSet");
        }

        if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);

    }

}
