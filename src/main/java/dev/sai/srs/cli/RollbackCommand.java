package dev.sai.srs.cli;


import dev.sai.srs.set.CompletedSet;
import dev.sai.srs.data.Item;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.util.HashMap;

@Command(name = "rollback", description = "Rolls back items from completed state to WorkingSet state", mixinStandardHelpOptions = true)
public class RollbackCommand implements Runnable {

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Parameters(paramLabel = "ITEM_ID", description = "Unique identifier of an item", defaultValue = "0")
    private int itemId;

    @Option(names = {"-a", "--all"}, description = "Rollback all items from WorkingSet", required = false)
    private boolean all;

    @Override
    public void run() {
        validate();
        if (all) {
            HashMap<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> completedSetItems = parent.completedSet.getItems();
            if (parent.workingSet.fillSet(completedSetItems.keySet()) &&
                            parent.completedSet.clearSet()) System.out.println("Full rollback complete.");
            else System.err.println("Rollback failed.");
        } else {
            if (parent.workingSet.addItem(itemId) &&
                            parent.completedSet.removeItem(itemId))
                System.out.println("Item [" + itemId + "] rollback complete.");
            else System.err.println("Item [" + itemId + "] rollback failed.");
        }
        if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
    }

    private void validate() {
        if (itemId < 0)
            throw new ParameterException(spec.commandLine(), "ITEM_ID [" + itemId + "] cannot be non-positive.");
        if ((itemId == 0 && !all) || (itemId != 0 && all))
            throw new ParameterException(spec.commandLine(), "Either ITEM_ID must be specified, or --all must be used, they are mutually exclusive.");
        if (parent.completedSet.getItems().isEmpty())
            throw new ParameterException(spec.commandLine(), "Nothing to rollback.");
        if (itemId!=0 && !parent.completedSet.containsItem(itemId))
            throw new ParameterException(spec.commandLine(), "ITEM_ID [" + itemId + "] non-existent.");
    }

}
