package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.ItemRepository;
import ansrs.util.Log;
import ansrs.util.Printer;
import ansrs.service.RecallService;
import ansrs.util.VersionProvider;
import picocli.CommandLine.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "recall",
        header = "Loads items from database into WorkingSet for recall",
        mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class RecallCommand implements Callable<Integer> {

    @ParentCommand
    SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "RECALL_COUNT", description = "The amount of items to load into WorkingSet for recall", defaultValue = "-12341234")
    private int recallCount;

    @Option(names = {"-o", "--overwrite"}, description = "Overwrite existing non-empty WorkingSet")
    private boolean overwrite;

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
            if (!overwrite && !append)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("WorkingSet non-empty, use --overwrite or --append to bypass"));
            if (overwrite) {
                workingSetItems.clear();
                Log.info("Overwriting existing WorkingSet");
            } else {
                Log.info("Appending items to non-empty WorkingSet");
            }
        }
        if (validCustomRecallIds.isEmpty()){
            RecallService recallService = createRecallService(parent.itemRepository);
            workingSetItems.addAll(recallService.recall(recallCount));
        } else {
            workingSetItems.addAll(validCustomRecallIds);
        }
        parent.workingSet.fillSet(workingSetItems);
        Log.info(workingSetItems.size() + " items in WorkingSet");
        List<Item> list = parent.itemRepository.getItemsFromList(workingSetItems.stream().toList()).orElse(new ArrayList<>());
        Printer.printItemsList(list);
        return 0;
    }

    protected RecallService createRecallService(ItemRepository db) {
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
                    else if (!parent.itemRepository.exists(id)){
                        throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID["+id+"] is non-existent in database"));
                    }
                     else {
                        validCustomRecallIds.add(id);
                    }
                }
            } else {
                for (int id : customRecallIds) {
                    if (id<=0) Log.warn("ITEM_ID cannot be non-positive, ignoring");
                    else if (!parent.itemRepository.exists(id)) {
                        Log.warn("ITEM_ID[" + id + "] non existent in database, ignoring");
                    } else {
                        validCustomRecallIds.add(id);
                    }
                }
                if (validCustomRecallIds.isEmpty()) throw new ParameterException(spec.commandLine(), Log.errorMsg("No valid ITEM_IDs to recall"));
            }
        }
        if (overwrite && append){
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Cannot use --overwrite and --append at once."));
        }
    }
}
