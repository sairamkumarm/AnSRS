package ansrs.cli;

import ansrs.util.Log;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(name = "delete",
        description = "Remove from WorkingSet, CompletedSet and db, depending on the flags, by default it removes from WorkingSet",
        mixinStandardHelpOptions = true)
public class DeleteCommand implements Callable<Integer> {

    @ParentCommand
    SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "ITEM_ID", description = "Unique identifier of an item", defaultValue = "-12341234")
    private int itemId;

    @Option(names = {"-c", "--completed"}, description = "Removes from items that are completed but, are yet to be commited to the database.")
    private boolean deleteFromCompletedSet;

    @Option(names = {"-d", "--database"}, description = "Removes a item from the database and sets")
    private boolean deleteFromDatabase;

    @Option(names = "--sure", description = "A defensive fallback to prevent accidental deletions", required = true)
    private boolean sure;

    @Option(names = "--working-all", description = "Removes all items from WorkingSet")
    private boolean clearWorkingSet;

    @Option(names = "--completed-all", description = "Removes all items from CompletedSet")
    private boolean clearCompletedSet;

    @Option(names = {"--hard-reset"}, description = "Hard resets all the persistent data, sets included")
    private boolean reset;

    @Override
    public Integer call() {
        validate();
        if (reset) {
            if (
                    parent.itemRepository.clearItems() &&
                            parent.completedSet.clearSet() &&
                            parent.workingSet.clearSet()
            ){
                Log.info("Reset successful");
                return 0;
            }
            else Log.error("Complete Reset failed, use --list flag to check status");
            return 1;
        }

        if (clearCompletedSet && clearWorkingSet){
            if (parent.workingSet.clearSet() && parent.completedSet.clearSet()){
                Log.info("WorkingSet and CompletedSet cleared");
                return 0;
            } else {
                Log.error("Complete Deletion failed, use -ls flag to check status");
                return 1;
            }
        }
        if (clearWorkingSet){
            if (parent.workingSet.clearSet()) {
                Log.info("WorkingSet cleared");
                return 0;
            }
            else Log.error("WorkingSet clear failed, use --list --set or -ls flags to check status");
            return 1;
        }
        if (clearCompletedSet){
            if (parent.completedSet.clearSet()) {
                Log.info("CompletedSet cleared");
                return 0;
            }
            else Log.error("CompletedSet clear failed, use --list --set or -ls flags to check status");
            return 1;
        }


        if (deleteFromDatabase) {
            if (!parent.itemRepository.deleteItemsById(itemId)) {
                Log.error("Error in deleting ITEM_ID [" + itemId + "] from DB, use --list to confirm its existence");
                return 1;
            }
            boolean cs = parent.completedSet.removeItem(itemId);
            boolean ws = parent.workingSet.removeItem(itemId);
            Log.info("ITEM_ID [" + itemId + "] deleted from the database" + ((cs)?", and the CompletedSet.":"") + ((ws)?", and the WorkingSet.":"")+((!ws&&!cs)?".":""));
        } else if (deleteFromCompletedSet) {
            if (!parent.completedSet.removeItem(itemId)) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] non-existent in CompletedSet"));
            } else {
                Log.info("ITEM_ID [" + itemId + "] deleted from CompletedSet");
            }
        } else {
            if (!parent.workingSet.removeItem(itemId)) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] non-existent in WorkingSet"));
            } else {
                Log.info("ITEM_ID [" + itemId + "] deleted from WorkingSet");
            }
        }

        return 0;
    }

    void validate(){
        if (!sure) throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag is needed to perform deletions."));
        if (itemId==-12341234){
            if(!clearCompletedSet && !clearWorkingSet && !reset){
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID can be skipped only during the --hard-reset, --working-all, or --completed-all command"));
            }
            if (reset && (clearCompletedSet || clearWorkingSet)) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("The --hard-reset command cannot be paired with --working-all or --completed-all commands"));
            }
        }else {
            if (itemId<=0) throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID [" + itemId + "] cannot be non-positive"));
            if (reset || clearWorkingSet || clearCompletedSet) throw new ParameterException(spec.commandLine(), Log.errorMsg("The --hard-reset, --working-all, or --completed-all commands, can only be used when ITEM_ID is empty"));

        }

    }
}
