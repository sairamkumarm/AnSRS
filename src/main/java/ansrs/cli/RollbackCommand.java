package ansrs.cli;


import ansrs.set.CompletedSet;
import ansrs.data.Item;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.Callable;

@Command(name = "rollback", description = "Rolls back items from completed state to WorkingSet state", mixinStandardHelpOptions = true)
public class RollbackCommand implements Callable<Integer> {

    @Spec
    Model.CommandSpec spec;

    @ParentCommand
    SRSCommand parent;

    @Parameters(index = "0", paramLabel = "ITEM_ID", description = "Unique identifier of an item", defaultValue = "0")
    private int itemId;

    @Option(names = {"-a", "--all"}, description = "Rollback all items from WorkingSet", required = false)
    private boolean all;

    @Override
    public Integer call() {
        validate();
        if (all) {
            HashMap<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> completedSetItems = parent.completedSet.getItems();
            if (parent.workingSet.fillSet(completedSetItems.keySet()) &&
                            parent.completedSet.clearSet()) {
                Log.info("Full rollback complete.");
                return 0;
            }
            else {
                Log.error("Rollback failed.");
                return 1;
            }
        } else {
            if (parent.workingSet.addItem(itemId) &&
                            parent.completedSet.removeItem(itemId)){
                Log.info("Item [" + itemId + "] rollback complete.");
                return 0;
            }
            else {
                Log.error("Item [" + itemId + "] rollback failed.");
                return 1;
            }
        }
    }

    private void validate() {
        if (itemId < 0)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] cannot be non-positive."));
        if ((itemId == 0 && !all) || (itemId != 0 && all))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Either ITEM_ID must be specified, or --all must be used, they are mutually exclusive."));
        if (parent.completedSet.getItems().isEmpty())
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Nothing to rollback."));
        if (itemId!=0 && !parent.completedSet.containsItem(itemId))
            throw new ParameterException(spec.commandLine(),Log.errorMsg("ITEM_ID [" + itemId + "] non-existent."));
    }

}
