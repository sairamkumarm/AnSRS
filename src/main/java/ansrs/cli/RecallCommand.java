package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.DBManager;
import ansrs.util.Log;
import ansrs.util.Printer;
import ansrs.service.RecallService;
import picocli.CommandLine.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "recall",
        description = "Loads items from database into WorkingSet for recall",
        mixinStandardHelpOptions = true)
public class RecallCommand implements Callable<Integer> {

    @ParentCommand
    SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "RECALL_COUNT", description = "The amount of items to load into WorkingSet for recall", defaultValue = "-12341234")
    private int recallCount;

    @Option(names = {"-f", "--force"}, description = "Overwrite existing non-empty WorkingSet")
    private boolean force;

    @Option(names = {"-a", "--append"}, description = "Append to an existing non empty WorkingSet, only unique items are added")
    private boolean append;

    @Option(names = {"-c","--custom"},
            paramLabel = "ITEM_ID", description = "Custom ITEM_ID(s) to recall, use space or comma separated values",
            split = ",", arity = "1..*")
    private ArrayList<Integer> customRecallIds = new ArrayList<>();

    private final ArrayList<Integer> validCustomRecallIds = new ArrayList<>();

    @Override
    public Integer call(){
        validate();
//        System.out.println(customRecallIds);
        Set<Integer> workingSetItems = parent.workingSet.getItemIdSet();
        if (!workingSetItems.isEmpty()) {
            if (!force)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("WorkingSet non-empty, use --force with or without --append to bypass"));
            if (!append) {
                workingSetItems.clear();
                Log.info("Overwriting existing WorkingSet");
            } else {
                Log.info("Appending items to non-empty WorkingSet");
            }
        }
        if (validCustomRecallIds.isEmpty()){
            RecallService recallService = createRecallService(parent.db);
            workingSetItems.addAll(recallService.recall(recallCount));
        } else {
            workingSetItems.addAll(validCustomRecallIds);
        }
        parent.workingSet.fillSet(workingSetItems);
        Log.info(workingSetItems.size() + " items in WorkingSet");
        List<Item> list = parent.db.getItemsFromList(workingSetItems.stream().toList()).orElse(new ArrayList<>());
        Printer.printItemsList(list);
        return 0;
    }

    protected RecallService createRecallService(DBManager db) {
        return new RecallService(db);
    }

    void validate(){
        if (customRecallIds.isEmpty()){
            if (recallCount==-12341234) throw new ParameterException(spec.commandLine(),Log.errorMsg("RECALL_COUNT is required for non --custom usage"));
            if (recallCount <= 0) throw new ParameterException(spec.commandLine(), Log.errorMsg("Recall count cannot be non-positive"));
        } else {
            if (customRecallIds.size()==1){
                for (int id: customRecallIds){
                    if (id<=0) throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID cannot be non-positive"));
                    else if (!parent.db.contains(id)){
                        throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID["+id+"] is non-existent in database"));
                    }
                     else {
                        validCustomRecallIds.add(id);
                    }
                }
            } else {
                for (int id : customRecallIds) {
                    if (id<=0) Log.warn("ITEM_ID cannot be non-positive, ignoring");
                    else if (!parent.db.contains(id)) {
                        Log.warn("ITEM_ID[" + id + "] non existent in database, ignoring");
                    } else {
                        validCustomRecallIds.add(id);
                    }
                }
                if (validCustomRecallIds.isEmpty()) throw new ParameterException(spec.commandLine(), Log.errorMsg("No valid ITEM_IDs to recall"));
            }
        }
    }
}
