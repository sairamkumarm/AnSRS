package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.DBManager;
import ansrs.db.ArchiveManager;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

import java.util.*;
import java.util.concurrent.Callable;

@Command(
        name = "archive",
        description = "Manage archive operations",
        mixinStandardHelpOptions = true
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

        ArchiveManager archiveManager = parent.archiveManager;
        DBManager db = parent.itemDB;

        if (addId != null) return handleAdd(addId, db, archiveManager);
        if (deleteId != null) return handleDelete(deleteId, archiveManager);
        if (restoreId != null) return handleRestore(restoreId, db, archiveManager);
        if (listAll) return handleList(archiveManager);
        if (getId != null) return handleGet(getId, archiveManager);
        if (nameQuery != null) return handleSearch(nameQuery, archiveManager);
        if (archiveAll) return handleArchiveAll(db, archiveManager);
        if (restoreAll) return handleRestoreAll(db, archiveManager);

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

    private int handleAdd(int itemId, DBManager db, ArchiveManager archiveManager) {
        if (!db.contains(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] does not exist in database"));

        if (parent.workingSet.getItemIdSet().contains(itemId) || parent.completedSet.getItems().containsKey(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] is in WorkingSet or CompletedSet, cannot archive"));

        Item item = db.getItemById(itemId).orElse(null);
        if (item==null){
            Log.error("Failed to fetch ITEM_ID[" + itemId + "] from DB");
            return 1;
        }

        if (!archiveManager.insertItem(item)){
            Log.error("Archiving ITEM_ID[" + itemId + "] failed, aborted");
            return 1;
        }

        if (!db.deleteItemsById(itemId)) {
            archiveManager.deleteItemsById(itemId);
            Log.error("Rollback: Failed to remove ITEM_ID[" + itemId + "] from DB after archiving");
            return 1;
        }

        Log.info("Archived ITEM_ID[" + itemId + "] successfully");
        return 0;
    }

    private int handleDelete(int itemId, ArchiveManager archiveManager) {
        if (!sure)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag required to confirm deletion"));
        if (!archiveManager.contains(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] does not exist in archive"));

        if (archiveManager.deleteItemsById(itemId))
            Log.info("Deleted ITEM_ID[" + itemId + "] from archive");
        else{
            Log.error("Failed to delete ITEM_ID[" + itemId + "] from archive");
            return 1;
        }
        return 0;
    }

    private int handleRestore(int itemId, DBManager db, ArchiveManager archiveManager) {
        if (!archiveManager.contains(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] does not exist in archive"));
        if (db.contains(itemId))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID[" + itemId + "] already exists in DB"));

        Item item = archiveManager.getItemById(itemId).orElse(null);
        if (item==null){
            Log.error("Failed to fetch ITEM_ID[" + itemId + "] from archive");
            return 1;
        }

        if (!db.insertItem(item)){
            Log.error("Failed to restore ITEM_ID[" + itemId + "] to DB");
            return 1;
        }

        if (!archiveManager.deleteItemsById(itemId)) {
            db.deleteItemsById(itemId);
            Log.error("Rollback: Failed to remove ITEM_ID[" + itemId + "] from archive after restore");
            return 1;
        }

        Log.info("Restored ITEM_ID[" + itemId + "] successfully");
        return 0;
    }

    private int handleList(ArchiveManager archiveManager) {
        var list = archiveManager.getAllItems().orElse(Collections.emptyList());
        Printer.printItemsList(list);
        return 0;
    }

    private int handleGet(int itemId, ArchiveManager archiveManager) {
        Item item = archiveManager.getItemById(itemId)
                .orElse(null);
        Printer.printItem(item);
        return 0;
    }

    private int handleSearch(String query, ArchiveManager archiveManager) {
        System.out.println("Search: "+query.trim());
        var list = archiveManager.searchItemsByName(query.trim()).orElse(Collections.emptyList());
        Printer.printItemsList(list);
        return 0;
    }

    private int handleArchiveAll(DBManager db, ArchiveManager archiveManager) {
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

        if (!archiveManager.insertItemsBatch(toArchive)){
            Log.error("Failed to archive items");
            return 1;
        }

        for (Item item : toArchive) {
            if (!db.deleteItemsById(item.getItemId())) {
                archiveManager.deleteItemsById(item.getItemId());
                Log.error("Rollback: failed to delete ITEM_ID[" + item.getItemId() + "] from DB");
            }
        }

        Log.info("Archived " + toArchive.size() + " items successfully");
        return 0;
    }

    private int handleRestoreAll(DBManager db, ArchiveManager archiveManager) {
        if (!sure)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("--sure flag required to confirm mass restore"));

        var allItems = archiveManager.getAllItems().orElse(Collections.emptyList());
        if (allItems.isEmpty()) {
            Log.error("No items in archive to restore");
            return 1;
        }

        int restoredCount = 0;
        for (Item item : allItems) {
            if (db.contains(item.getItemId())) {
                Log.warn("Skipping ITEM_ID[" + item.getItemId() + "]: already exists in DB");
                continue;
            }
            if (!db.insertItem(item)) {
                Log.error("Failed to restore ITEM_ID[" + item.getItemId() + "] to DB");
                continue;
            }
            if (!archiveManager.deleteItemsById(item.getItemId())) {
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
