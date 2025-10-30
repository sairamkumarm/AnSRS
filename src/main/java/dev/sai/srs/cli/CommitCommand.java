package dev.sai.srs.cli;

import dev.sai.srs.data.Item;
import dev.sai.srs.set.CompletedSet;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(name = "commit",
        description = "commit - lets you commit completed items in WorkingSet to the database",
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
            throw new ParameterException(spec.commandLine(),"Nothing to commit");
        }
        List<Item> dbItems = parent.db.getItemsFromList(
                completedSetItems.keySet().stream().toList()).
                orElseThrow(
                        ()->new ParameterException(spec.commandLine(), "WorkingSet items non-existent in DB")
                );
        if (!parent.workingSet.getItemIdSet().isEmpty() && !force){
            throw new ParameterException(spec.commandLine(),"Items found in WorkingSet, use --force to override and commit");
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
        System.out.println(dbItems);
        if (!parent.db.updateItemsBatch(dbItems)){
            System.err.println("Commit Failed, rolling back");
            //rollback from Set items, not db, to preserve the null pools, that signify no change
            for (Map.Entry<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> item: completedSetItems.entrySet()) parent.completedSet.addItem(item.getKey(), item.getValue().getPool(), item.getValue().getLast_recall());
            return;
        }
        System.out.println("Commit success: "+ dbItems.size()+" items updated");
        if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
    }
}
