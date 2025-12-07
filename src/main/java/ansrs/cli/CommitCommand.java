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
import java.util.concurrent.Callable;

@Command(name = "commit",
        description = "Save completed items in WorkingSet to the database",
        mixinStandardHelpOptions = true)
public class CommitCommand implements Callable<Integer> {

    @Spec
    Model.CommandSpec spec;

    @ParentCommand
    SRSCommand parent;

    @Option(names = {"-f","--force"}, description = "allows you to commit when there are pending items in the WorkingSet")
    private boolean force;

    @Override
    public Integer call() {
        HashMap<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> completedSetItems = parent.completedSet.getItems();
        if(completedSetItems.isEmpty()){
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Nothing to commit"));
        }
        List<Item> dbItems = parent.itemRepository.getItemsFromList(
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
        if (!parent.itemRepository.updateItemsBatch(dbItems)){
            Log.error("Commit Failed, rolling back");
            //rollback from Set items, not db, to preserve the null pools, that signify no change
            for (Map.Entry<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> item: completedSetItems.entrySet()) parent.completedSet.addItem(item.getKey(), item.getValue().getPool(), item.getValue().getLast_recall());
            return 1;
        }
        Log.info("Commit success: "+ dbItems.size()+" items updated");
        Printer.printItemsList(dbItems);
        return 0;
    }
}
