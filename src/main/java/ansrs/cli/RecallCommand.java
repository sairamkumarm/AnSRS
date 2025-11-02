package ansrs.cli;

import ansrs.data.Item;
import ansrs.util.Log;
import ansrs.util.Printer;
import ansrs.service.RecallService;
import picocli.CommandLine.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Command(name = "recall",
        description = "Loads items from database into WorkingSet for recall",
        mixinStandardHelpOptions = true)
public class RecallCommand implements Runnable {

    @ParentCommand
    private SRSCommand parent;

    @Spec
    private Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "RECALL_COUNT", description = "The amount of items to load into WorkingSet for recall")
    private int recallCount;

    @Option(names = {"-f", "--force"}, description = "use --force, to overwrite existing non-empty WorkingSet")
    private boolean force;

    @Option(names = {"-a", "--append"}, description = "use --append, to append to an existing non empty WorkingSet, only unique items are added")
    private boolean append;

    @Override
    public void run() {
        if (recallCount <= 0) throw new ParameterException(spec.commandLine(), Log.errorMsg("Recall count cannot be non-positive"));
        RecallService recallService = new RecallService(parent.db);
        Set<Integer> workingSetItems = parent.workingSet.getItemIdSet();
        if (!workingSetItems.isEmpty()) {
            if (!force)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("WorkingSet non-empty, use --force and/or --append to bypass"));
            if (!append) {
                workingSetItems.clear();
                Log.info("Overwriting existing WorkingSet");
            } else {
                Log.info("Appending items to non-empty WorkingSet");
            }
        }
        workingSetItems.addAll(recallService.recall(recallCount));
        parent.workingSet.fillSet(workingSetItems);
        Log.info(workingSetItems.size() + " items in WorkingSet");
        List<Item> list = parent.db.getItemsFromList(workingSetItems.stream().toList()).orElse(new ArrayList<>());
        Printer.printItemsGrid(list);
        if (parent.list) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
    }
}
