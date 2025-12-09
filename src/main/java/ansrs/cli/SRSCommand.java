package ansrs.cli;

import ansrs.db.ArchiveRepository;
import ansrs.db.GroupRepository;
import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.db.ItemRepository;
import ansrs.util.Banner;
import ansrs.util.Log;
import ansrs.util.Printer;
import ansrs.util.VersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(name = "ansrs",
        description = "AnSRS (Pronounced \"Answers\") is a spaced repetition system.", versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true,
        subcommands = {AddCommand.class, CompleteCommand.class, DeleteCommand.class, CommitCommand.class,
                RecallCommand.class, RollbackCommand.class, ImportCommand.class, ArchiveCommand.class,
                GroupCommand.class})
public class SRSCommand implements Callable<Integer> {
    public final WorkingSet workingSet;
    public final CompletedSet completedSet;
    public final ItemRepository itemRepository;
    public final ArchiveRepository archiveRepository;
    public final GroupRepository groupRepository;

    public SRSCommand(WorkingSet workingSet, CompletedSet completedSet, ItemRepository ItemRepository, ArchiveRepository archiveRepository, GroupRepository groupRepository) {
        this.workingSet = workingSet;
        this.completedSet = completedSet;
        this.itemRepository = ItemRepository;
        this.archiveRepository = archiveRepository;
        this.groupRepository = groupRepository;
    }

    @Spec
    private Model.CommandSpec spec;

    @Option(names = {"-l", "--list"}, description = "Lists set and db state", required = false)
    private boolean list;

    @Option(names = {"-s", "--set"}, description = "Use this flag with --list to print only set", required = false)
    private boolean set;

    @Option(names = {"-i", "--id"}, paramLabel = "ITEM_ID", description = "Print a specific Item", required = false, defaultValue = "-12341234")
    private int itemId;

    @Option(names = {"-n", "--name"}, paramLabel = "ITEM_NAME_QUERY", description = "Find an Item by it's name, query must be longer than one character", required = false, defaultValue = "zyxwvutsrqp")
    private String itemName;

    @Override
    public Integer call() {
        startMessage();
        validate();
        if (itemId != -12341234) {
            System.out.println("Item:");
            Printer.printItem(itemRepository.getItemById(itemId).orElse(null));
        }
        if (!itemName.equals("zyxwvutsrqp")) {
            System.out.println("Search: " + itemName);
            Printer.printItemsList(itemRepository.searchItemsByName(itemName.trim()).orElse(new ArrayList<>()));
        }
        if (list) {
            if (set) {
                Printer.setStatePrinter(workingSet, completedSet, itemRepository);
            } else {
                Printer.statePrinter(workingSet, completedSet, itemRepository);
            }
        }
        return 0;
    }

    private void validate() {
        if (itemId != -12341234) {
            if (itemId <= 0)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID cannot be non-positive"));
        }
        if (!itemName.equals("zyxwvutsrqp")) {
            if (itemName.isBlank())
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_NAME_QUERY cannot be empty, to see all Items, use --list flag"));
            if (itemName.trim().length()==1)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_NAME_QUERY cannot be of size 1, too broad"));
        }
        if (set && !list)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--set is to be used along with --list, to display only Set statuses"));
    }

    private void startMessage(){
        if(!list && !set && itemId == -12341234 && itemName.equals("zyxwvutsrqp") && workingSet.getItemIdSet().isEmpty()){
            Banner.colorrizedBanner(VersionProvider.getVersionString());
            Banner.initMessage();
            System.out.println(spec.commandLine().getUsageMessage());
        } else {
            System.out.println("Current WorkingSet:");
            Printer.printItemsList(itemRepository.getItemsFromList(workingSet.getItemIdList()).orElse(new ArrayList<>()));
        }
    }
}
