package ansrs.cli;

import ansrs.data.Group;
import ansrs.db.GroupRepository;
import ansrs.data.Item;
import ansrs.set.WorkingSet;
import ansrs.util.Log;
import ansrs.util.Printer;
import ansrs.util.VersionProvider;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "group",
        header = "Manage groups, items in groups, and recall from group",
        description = "Manage items groups, for quick loading into WorkingSet for recall \nNOTE: destructive actions are not carried onto the items themselves",
        mixinStandardHelpOptions = true, versionProvider = VersionProvider.class
)
public class GroupCommand implements Callable<Integer> {

    @ParentCommand
    SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    // ===== CORE IDENTIFIER =====
    @Option(names = "--id", paramLabel = "GROUP_ID", description = "Target group id")
    private Integer groupId;

    // ===== CREATE =====
    @Option(names = "--create", description = "Create a new group")
    private boolean create;

    @Option(names = "--name", paramLabel = "GROUP_NAME", description = "Group name (required for create)")
    private String name;

    @Option(names = "--link", paramLabel = "GROUP_LINK", description = "Optional group link (https only)")
    private String link;

    // ===== UPDATE =====
    @Option(names = "--update", description = "Update group name or link")
    private boolean update;

    // ===== DELETE =====
    @Option(names = "--delete", description = "Delete a group (does not affect items in them)")
    private boolean delete;

    // ===== READ =====
    @Option(names = "--show", description = "Show a group's metadata")
    private boolean show;

    @Option(names = "--show-all", description = "Show all groups in collection")
    private boolean showAll;

    @Option(names = "--show-items", description = "Show items in a specific group")
    private boolean showItems;

    // ===== ITEM OPERATIONS =====
    @Option(names = "--add-item", paramLabel = "ITEM_ID", description = "Add an item into a group, requires --id=GROUPS_ID")
    private Integer addItemId;

    @Option(
            names = "--add-batch",
            paramLabel = "ITEM_IDS", description = "Add items in space separated or comma separated fashion",
            split = ",", arity = "1..*"
    )
    private List<Integer> batchItemIds = new ArrayList<>();

    @Option(names = "--remove-item", paramLabel = "ITEM_ID", description = "Remove an item from a specified group (does not affect the item itself)")
    private Integer removeItemId;

    @Option(names = "--recall", paramLabel = "RECALL_MODE", description = "Add group items to WorkingSet with mode \"overwrite\" | \"append\"")
    private String recall;

    @Override
    public Integer call() {
        validate();

        GroupRepository groupRepository = parent.groupRepository;
        WorkingSet workingSet = parent.workingSet;
        // ===== CREATE =====
        if (create) {
            boolean ok = groupRepository.createGroup(groupId, name, link);
            if (!ok) {
                Log.error("Failed to create GROUP["+ groupId +"]");
                return 1;
            }
            Log.info("Created group [" + groupId + "]");
            return 0;
        }

        // ===== UPDATE =====
        if (update) {
            boolean ok = groupRepository.updateGroup(groupId, name, link);
            if (!ok) {
                Log.error("Failed to update GROUP[" + groupId + "]");
                return 1;
            }
            Log.info("Updated group [" + groupId + "]");
            return 0;
        }

        // ===== DELETE =====
        if (delete) {
            boolean ok = groupRepository.deleteGroupById(groupId);
            if (!ok) {
                Log.error("Failed to delete group [" + groupId + "]");
                return 1;
            }
            Log.info("Deleted group [" + groupId + "]");
            return 0;
        }

        // ===== SHOW GROUP METADATA =====
        if (show) {
            Optional<Group> rowOpt = groupRepository.findById(groupId);
            if (rowOpt.isEmpty()) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("GROUP_ID[" + groupId + "] does not exist")
                );
            }
            Group row = rowOpt.get();
            Printer.printGroup(row);
            return 0;
        }

        // ===== SHOW ALL GROUPS =====

        if (showAll){
            List<Group> groups = groupRepository.findAll().orElse(new ArrayList<>());
            Printer.printGroupsList(groups);
            return 0;
        }

        // ===== SHOW GROUP ITEMS =====
        if (showItems) {
            List<Integer> itemIds = groupRepository.getItemIdsForGroup(groupId);
            if (itemIds.isEmpty()) {
                Log.info("No items in GROUP[" + groupId + "]");
                return 0;
            }
            List<Item> items = parent.itemRepository.getItemsFromList(itemIds).orElse(new ArrayList<>());
            Printer.printItemsList(items);
            return 0;
        }

        // ===== ADD SINGLE ITEM =====
        if (addItemId != null) {
            boolean ok = groupRepository.addItemToGroup(groupId, addItemId);
            if (!ok) {
                Log.error("Failed to add ITEM_ID[" + addItemId + "] to GROUP[" + groupId + "]");
                return 1;
            }
            Log.info("Added ITEM_ID[" + addItemId + "] to GROUP[" + groupId + "]");
            return 0;
        }

        // ===== ADD BATCH ITEMS =====
        if (!batchItemIds.isEmpty()) {
            List<Integer> toAdd = new ArrayList<>();

            for (Integer id : batchItemIds) {
                if (parent.groupRepository.itemExistsInGroup(groupId, id)) {
                    Log.warn("ITEM_ID[" + id + "] already exists in GROUP[" + groupId + "], skipping.");
                } else {
                    toAdd.add(id);
                }
            }

            if (toAdd.isEmpty()) {
                Log.warn("No new items to add to GROUP[" + groupId + "]");
                return 0;
            }

            boolean ok = groupRepository.addItemsToGroupBatch(groupId, toAdd);
            if (!ok) {
                Log.error("Batch add failed for GROUP[" + groupId + "]");
                return 1;
            }

            Log.info("Batch added " + toAdd.size() + " items to GROUP[" + groupId + "]");
            return 0;
        }

        // ===== REMOVE SINGLE ITEM =====
        if (removeItemId != null) {
            boolean ok = groupRepository.removeItemFromGroup(groupId, removeItemId);
            if (!ok) {
                Log.error("Failed to remove ITEM_ID[" + removeItemId + "] from GROUP[" + groupId + "]");
                return 1;
            }
            Log.info("Removed ITEM_ID[" + removeItemId + "] from GROUP[" + groupId + "]");
            return 0;
        }

        // ===== RECALL GROUP =====
        if (recall != null && !recall.isBlank()) {
            List<Integer> itemIdsForGroup = groupRepository.getItemIdsForGroup(groupId);
            if (itemIdsForGroup.isEmpty()){
                Log.warn("GROUP["+groupId+"] empty, nothing to recall");
                return 0;
            }
            if (recall.equals("overwrite")){
                if (!workingSet.clearSet()){
                    Log.error("WorkingSet overwrite failed");
                    return 1;
                } else {
                    Log.info("WorkingSet overwritten");
                }
            } else if(recall.equals("append")){
                Log.info("WorkingSet appended");
            }
            workingSet.fillSet(itemIdsForGroup);
            Log.info("GROUP["+groupId+"] items added to WorkingSet");
            return 0;
        }

        throw new ParameterException(spec.commandLine(), Log.errorMsg("No operation specified"));
    }

    // ===== VALIDATION =====
    private void validate() {

        int opCount = 0;
        if (create) opCount++;
        if (update) opCount++;
        if (delete) opCount++;
        if (show) opCount++;
        if (showItems) opCount++;
        if (showAll) opCount++;
        if (recall != null) opCount++;
        if (addItemId != null) opCount++;
        if (!batchItemIds.isEmpty()) opCount++;
        if (removeItemId != null) opCount++;

        if (opCount != 1) {
            throw new ParameterException(
                    spec.commandLine(),
                    Log.errorMsg("Exactly one operation flag must be specified")
            );
        }

        if (showAll) return;

        // ===== CREATE RULES =====
        if (create) {
            if (groupId == null || groupId <= 0)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("Valid --id required for create"));

            if (name == null || name.isBlank())
                throw new ParameterException(spec.commandLine(), Log.errorMsg("--name is required for create"));

            if (link!=null && !link.startsWith("https://")){
                throw new ParameterException(spec.commandLine(), Log.errorMsg("link must start with https://"));
            }

            if (parent.groupRepository.exists(groupId))
                throw new ParameterException(spec.commandLine(), Log.errorMsg("GROUP_ID[" + groupId + "] already exists"));

            return;
        }

        // ===== ALL NON-CREATE REQUIRE ID =====
        if (groupId == null || groupId <= 0) {
            throw new ParameterException(
                    spec.commandLine(),
                    Log.errorMsg("--id is required for this operation")
            );
        }

        if (!parent.groupRepository.exists(groupId)) {
            throw new ParameterException(
                    spec.commandLine(),
                    Log.errorMsg("GROUP_ID[" + groupId + "] does not exist")
            );
        }

        // ===== UPDATE VALIDATION =====
        if (update) {
            if ((name == null || name.isBlank()) && (link == null || link.isBlank())) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("At least one of --name or --link is required for update")
                );
            }
            // ===== LINK VALIDATION =====
            if (link!=null && !link.startsWith("https://")){
                throw new ParameterException(spec.commandLine(), Log.errorMsg("link must start with https://"));
            }
            return;
        }


        // ===== ITEM VALIDATION =====
        if (addItemId != null) {
            if (!parent.itemRepository.exists(addItemId)) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("ITEM_ID[" + addItemId + "] does not exist")
                );
            }
            if (parent.groupRepository.itemExistsInGroup(groupId, addItemId)) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("ITEM_ID[" + addItemId + "] already exists in GROUP[" + groupId + "]")
                );
            }
        }

        if (removeItemId != null) {
            if (!parent.itemRepository.exists(removeItemId)) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("ITEM_ID[" + removeItemId + "] does not exist")
                );
            }
            if (!parent.groupRepository.itemExistsInGroup(groupId, removeItemId)) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("ITEM_ID[" + removeItemId + "] does not exist in GROUP[" + groupId + "]")
                );
            }
        }


        for (Integer id : batchItemIds) {
            if (id == null || id <= 0 || !parent.itemRepository.exists(id)) {
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("Invalid ITEM_ID in batch: " + id)
                );
            }
        }

        if (recall!=null){
            if (recall.isBlank() || (!recall.equals("overwrite") && !recall.equals("append"))){
                throw new ParameterException(
                        spec.commandLine(),
                        Log.errorMsg("Invalid RECALL_MODE, it must be either \"overwrite\" | \"append\"")
                );
            }
        }
    }
}
