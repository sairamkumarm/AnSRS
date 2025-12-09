package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.ItemRepository;
import ansrs.db.ArchiveRepository;
import ansrs.util.Log;
import ansrs.util.Printer;
import ansrs.util.VersionProvider;
import picocli.CommandLine.*;

import java.util.*;
import java.util.concurrent.Callable;

@Command(
        name = "archive",
        header = "Manage operations for items in archive",
        mixinStandardHelpOptions = true, versionProvider = VersionProvider.class
)
public class ArchiveCommand implements Callable<Integer> {

    @ParentCommand
    SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Option(names = {"--add"}, paramLabel = "ITEM_ID", description = "Move ITEM_ID from DB to archive")
    private Integer addId;

    @Option(names = {"--delete"}, paramLabel = "ITEM_ID", description = "Delete ITEM_ID from archive")
    private Integer deleteId;

    @Option(names = {"--restore"}, paramLabel = "ITEM_ID", description = "Restore ITEM_ID from archive to DB")
    private Integer restoreId;

    @Option(names = {"--id"}, paramLabel = "ITEM_ID", description = "Get archived ITEM_ID details")
    private Integer getId;

    @Option(names = {"--name"}, paramLabel = "ITEM_NAME_QUERY", description = "Search archived items by name")
    private String nameQuery;

    @Option(names = {"--list"}, description = "List all archived items")
    private boolean listAll;

    @Option(names = {"--sure"}, description = "Confirm destructive operation for --delete, --all, and --restore-all")
    private boolean sure;

    @Option(names = {"--all"}, description = "Archive all items from DB (excluding those in sets)")
    private boolean archiveAll;

    @Option(names = {"--restore-all"}, description = "Restore all items from archive to DB")
    private boolean restoreAll;

    @Override
    public Integer call() {
        validateSingleOperation();

        ArchiveRepository archiveRepository = parent.archiveRepository;
        ItemRepository db = parent.itemRepository;

        if (addId != null) return handleAdd(addId, db, archiveRepository);
        if (deleteId != null) return handleDelete(deleteId, archiveRepository);
        if (restoreId != null) return handleRestore(restoreId, db, archiveRepository);
        if (listAll) return handleList(archiveRepository);
        if (getId != null) return handleGet(getId, archiveRepository);
        if (nameQuery != null) return handleSearch(nameQuery, archiveRepository);
        if (archiveAll) return handleArchiveAll(db, archiveRepository);
        if (restoreAll) return handleRestoreAll(db, archiveRepository);

        throw new ParameterException(spec.commandLine(), Log.errorMsg("No valid operation specified"));
    }

    private void validateSingleOperation() {
        int ops = 0;
        if (addId != null) ops++;
        if (deleteId != null) ops++;
        if (restoreId != null) ops++;
        if (listAll) ops++;
        if (getId != null) ops++;
        if (nameQuery != null) ops++;
        if (archiveAll) ops++;
        if (restoreAll) ops++;
        if (ops == 0)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("No operation specified"));
        if (ops > 1)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Only one operation can be used at a time"));
    }

    private int handleAdd(int itemId, ItemRepository db, ArchiveRepository archiveRepository) {
        if (!db.exists(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] does not exist in database"));

        if (parent.workingSet.getItemIdSet().contains(itemId) || parent.completedSet.getItems().containsKey(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] is in WorkingSet or CompletedSet, cannot archive"));

        Item item = db.getItemById(itemId).orElse(null);
        if (item==null){
            Log.error("Failed to fetch ITEM_ID[" + itemId + "] from DB");
            return 1;
        }

        if (!archiveRepository.insertItem(item)){
            Log.error("Archiving ITEM_ID[" + itemId + "] failed, aborted");
            return 1;
        }

        if (!db.deleteItemsById(itemId)) {
            archiveRepository.deleteItemsById(itemId);
            Log.error("Rollback: Failed to remove ITEM_ID[" + itemId + "] from DB after archiving");
            return 1;
        }

        Log.info("Archived ITEM_ID[" + itemId + "] successfully");
        return 0;
    }

    private int handleDelete(int itemId, ArchiveRepository archiveRepository) {
        if (!sure)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag required to confirm deletion"));
        if (!archiveRepository.contains(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] does not exist in archive"));

        if (archiveRepository.deleteItemsById(itemId))
            Log.info("Deleted ITEM_ID[" + itemId + "] from archive");
        else{
            Log.error("Failed to delete ITEM_ID[" + itemId + "] from archive");
            return 1;
        }
        return 0;
    }

    private int handleRestore(int itemId, ItemRepository db, ArchiveRepository archiveRepository) {
        if (!archiveRepository.contains(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] does not exist in archive"));
        if (db.exists(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] already exists in DB"));

        Item item = archiveRepository.getItemById(itemId).orElse(null);
        if (item==null){
            Log.error("Failed to fetch ITEM_ID[" + itemId + "] from archive");
            return 1;
        }

        if (!db.insertItem(item)){
            Log.error("Failed to restore ITEM_ID[" + itemId + "] to DB");
            return 1;
        }

        if (!archiveRepository.deleteItemsById(itemId)) {
            db.deleteItemsById(itemId);
            Log.error("Rollback: Failed to remove ITEM_ID[" + itemId + "] from archive after restore");
            return 1;
        }

        Log.info("Restored ITEM_ID[" + itemId + "] successfully");
        return 0;
    }

    private int handleList(ArchiveRepository archiveRepository) {
        var list = archiveRepository.getAllItems().orElse(Collections.emptyList());
        Printer.printItemsList(list);
        return 0;
    }

    private int handleGet(int itemId, ArchiveRepository archiveRepository) {
        Item item = archiveRepository.getItemById(itemId)
                .orElse(null);
        Printer.printItem(item);
        return 0;
    }

    private int handleSearch(String query, ArchiveRepository archiveRepository) {
        System.out.println("Search: "+query.trim());
        var list = archiveRepository.searchItemsByName(query.trim()).orElse(Collections.emptyList());
        Printer.printItemsList(list);
        return 0;
    }

    private int handleArchiveAll(ItemRepository db, ArchiveRepository archiveRepository) {
        if (!sure)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag required to confirm mass archival"));
        var allItems = db.getAllItems().orElse(Collections.emptyList());
        if (allItems.isEmpty()) {
            Log.error("No items in DB to archive");
            return 1;
        }

        Set<Integer> workingIds = parent.workingSet.getItemIdSet();
        Set<Integer> completedIds = parent.completedSet.getItems().keySet();
        List<Item> toArchive = new ArrayList<>();

        for (Item item : allItems) {
            if (workingIds.contains(item.getItemId()) || completedIds.contains(item.getItemId())) {
                Log.warn("Skipping ITEM_ID[" + item.getItemId() + "]: exists in Working/Completed set, either complete and commit, or delete it");
                continue;
            }
            toArchive.add(item);
        }

        if (toArchive.isEmpty()) {
            Log.error("No eligible items to archive");
            return 1;
        }

        if (!archiveRepository.insertItemsBatch(toArchive)){
            Log.error("Failed to archive items");
            return 1;
        }

        for (Item item : toArchive) {
            if (!db.deleteItemsById(item.getItemId())) {
                archiveRepository.deleteItemsById(item.getItemId());
                Log.error("Rollback: failed to delete ITEM_ID[" + item.getItemId() + "] from DB");
            }
        }

        Log.info("Archived " + toArchive.size() + " items successfully");
        return 0;
    }

    private int handleRestoreAll(ItemRepository db, ArchiveRepository archiveRepository) {
        if (!sure)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag required to confirm mass restore"));

        var allItems = archiveRepository.getAllItems().orElse(Collections.emptyList());
        if (allItems.isEmpty()) {
            Log.error("No items in archive to restore");
            return 1;
        }

        int restoredCount = 0;
        for (Item item : allItems) {
            if (db.exists(item.getItemId())) {
                Log.warn("Skipping ITEM_ID[" + item.getItemId() + "]: already exists in DB");
                continue;
            }
            if (!db.insertItem(item)) {
                Log.error("Failed to restore ITEM_ID[" + item.getItemId() + "] to DB");
                continue;
            }
            if (!archiveRepository.deleteItemsById(item.getItemId())) {
                db.deleteItemsById(item.getItemId());
                Log.error("Rollback: failed to remove ITEM_ID[" + item.getItemId() + "] from archive after restore");
                continue;
            }
            restoredCount++;
        }

        if (restoredCount == 0) {
            Log.error("No items were restored");
            return 1;
        }

        Log.info("Restored " + restoredCount + " items successfully");
        return 0;
    }
}
