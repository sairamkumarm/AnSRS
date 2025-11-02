package ansrs.cli;

import ansrs.data.Item;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

@Command(name = "complete",
        description = "Marks item as completed, and transfers them to the CompletedSet",
        mixinStandardHelpOptions = true)
public class CompleteCommand implements Runnable {

    @ParentCommand
    private SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "ITEM_ID", description = "Unique identifier of an item", defaultValue = "0")
    private int itemId;

    @Option(names = {"--update"}, paramLabel = "ITEM_POOL", description = "Optional ITEM_POOL value updated for item, choose from H M and L", required = false)
    private String poolString;

    @Option(names = "--date", paramLabel = "ITEM_LAST_RECALL", description = "Optional ITEM_LAST_RECALL specification, use YYYY-MM-DD format", required = false)
    private String date;

    @Option(names = {"-f", "--force"}, description = "Forces completion of a item non existent in WorkingSet.")
    private boolean force;

    @Option(names = {"-a", "--all"}, description = "Completes all items in the WorkingSet, but you lose the ability to update item pools")
    private boolean allComplete;

    @Override
    public void run() {
        validate();

        Set<Integer> workingSetItemIds = new HashSet<>(parent.workingSet.getItemIdSet());
        if (allComplete && itemId == 0) {
            for (int id : workingSetItemIds) {
                parent.completedSet.addItem(id, null);
                parent.workingSet.removeItem(id);
            }
            Log.info("All items in WorkingSet completed.");
            if (parent.list) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
            return;
        }

        Item.Pool pool = (poolString != null) ? Item.Pool.valueOf(poolString.toUpperCase()) : null;
        LocalDate lastRecall = (date!=null) ? LocalDate.parse(date):LocalDate.now();

        if (!workingSetItemIds.contains(itemId)) {
            if (!force) throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] doesn't exist in WorkingSet"));
            Item item = parent.db.getItemById(itemId).
                    orElseThrow(
                            () -> new ParameterException(
                                    spec.commandLine(),
                                    Log.errorMsg("ITEM_ID [" + itemId + "] doesn't exist in database"))
                    );
            parent.completedSet.addItem(item.getItemId(), item.getItemPool(), lastRecall);
        } else {
            parent.workingSet.removeItem(itemId);
            if (poolString == null || poolString.isEmpty()) parent.completedSet.addItem(itemId, null, lastRecall);
            else {
                parent.completedSet.addItem(itemId, pool, lastRecall);
            }
        }
        Log.info("Completed ITEM_ID [" + itemId + "]" + ((pool != null) ? (", and moved item to ITEM_POOL[" + pool.name() + "]") : "") + ((!lastRecall.equals(LocalDate.now()))?", with ITEM_LAST_RECALL["+lastRecall.toString()+"]":""));
        if (parent.list) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);

    }


    private void validate() {
        if (itemId < 0) {
            throw new ParameterException(spec.commandLine(),Log.errorMsg("ITEM_ID [" + itemId + "] required to be positive"));
        }
        if (itemId == 0 && !allComplete) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID can be [" + itemId + "] only when --all flag is used to completed all the items, otherwise, it is required to be positive"));
        }
        if (itemId != 0) {
            if (allComplete) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("--all / -a flag can only be used when ITEM_ID is not specified"));
            }
            if (!parent.db.contains(itemId)) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] non-existent in database."));
            }
            if (poolString != null) {
                try {
                    Item.Pool.valueOf(poolString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ParameterException(spec.commandLine(), Log.errorMsg(poolString + " is not a valid ITEM_POOL value."));
                }
            }
            if (date != null){
                try {
                    LocalDate.parse(date);
                }catch (DateTimeParseException e){
                    throw new ParameterException(spec.commandLine(), Log.errorMsg(date + " is not a valid ITEM_LAST_RECALL value. use YYYY-MM-DD format."));
                }
            }
        }


    }
}
