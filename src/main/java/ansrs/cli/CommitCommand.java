package ansrs.cli;

import ansrs.data.Item;
import ansrs.set.CompletedSet;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(name = "commit",
        description = "Save completed items in WorkingSet to the database",
        mixinStandardHelpOptions = true)
public class CommitCommand implements Runnable{

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Option(names = {"-f","--force"}, description = "allows you to commit when there are pending items in the WorkingSet")
    private boolean force;

    @Override
    public void run() {
        HashMap<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> completedSetItems = parent.completedSet.getItems();
        if(completedSetItems.isEmpty()){
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Nothing to commit"));
        }
        List<Item> dbItems = parent.db.getItemsFromList(
                completedSetItems.keySet().stream().toList()).
                orElseThrow(
                        ()->new ParameterException(spec.commandLine(), Log.errorMsg("WorkingSet items non-existent in DB"))
                );
        if (!parent.workingSet.getItemIdSet().isEmpty() && !force){
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Items found in WorkingSet, use --force to override and commit"));
        }
        for (Item item : dbItems){
            CompletedSet.Pair<Item.Pool, LocalDate> poolLocalDatePair = completedSetItems.get(item.getItemId());
            Item.Pool pool = poolLocalDatePair.getPool();
            if (pool!=null){
                item.setItemPool(pool);
            }
            item.setLastRecall(poolLocalDatePair.getLast_recall());
            item.setTotalRecalls(item.getTotalRecalls()+1);
            parent.completedSet.removeItem(item.getItemId());
        }
//        System.out.println(dbItems);
        if (!parent.db.updateItemsBatch(dbItems)){
            Log.error("Commit Failed, rolling back");
            //rollback from Set items, not db, to preserve the null pools, that signify no change
            for (Map.Entry<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> item: completedSetItems.entrySet()) parent.completedSet.addItem(item.getKey(), item.getValue().getPool(), item.getValue().getLast_recall());
            if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
            return;
        }
        Log.info("Commit success: "+ dbItems.size()+" items updated");
        if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
    }
}
