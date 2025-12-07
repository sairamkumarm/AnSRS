package ansrs.cli;

import ansrs.data.Item;
import ansrs.util.Log;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;

@Command(name = "add",
        description = """
                Add new items into the item database or update an existing one
                Parameter Order: ITEM_ID ITEM_NAME ITEM_LINK ITEM_POOL [--last-recall=ITEM_RECALL_DATE] [--total-recalls=ITEM_TOTAL_RECALLS]
                To update, use the flag versions or name, link, pool, last_recall and total_recalls
                """,
        mixinStandardHelpOptions = true)
public class AddCommand implements Callable<Integer> {

    @Spec
    Model.CommandSpec spec;

    @ParentCommand
    SRSCommand parent;

    // always required
    @Parameters(index = "0", paramLabel = "ITEM_ID", description = "Unique identifier of an item")
    private int itemId;

    // used for insert
    @Parameters(index = "1", paramLabel = "ITEM_NAME", description = "Name of the item, required for insert", arity = "0..1")
    private String itemName;

    @Parameters(index = "2", paramLabel = "ITEM_LINK", description = "Link to the item, required for insert", arity = "0..1")
    private String itemLink;

    @Parameters(index = "3", paramLabel = "ITEM_POOL", description = "Pick from H, M, and L, required for insert", arity = "0..1")
    private String itemPool;

    // used for update
    @Option(names = "--name", paramLabel = "ITEM_NAME", description = "Update item name")
    private String updateName;

    @Option(names = "--link", paramLabel = "ITEM_LINK", description = "Update item link (must start with https://)")
    private String updateLink;

    @Option(names = "--pool",  paramLabel = "ITEM_POOL", description = "Update item pool (H/M/L)")
    private String updatePool;

    @Option(names = {"-u", "--update"}, description = "Update an existing item", required = false)
    private boolean update;

    @Option(names = "--last-recall", paramLabel = "ITEM_LAST_RECALL", description = "Set custom last recall date (YYYY-MM-DD)")
    private String itemLastRecall;

    @Option(names = "--total-recalls", paramLabel = "ITEM_TOTAL_RECALLS", description = "Set custom total recall count")
    private Integer totalRecalls;

    @Override
    public Integer call() {
        try {
            if (update) {
                return handleUpdate();
            } else {
                return handleInsert();
            }
        } catch (ParameterException e) {
            throw e;
        } catch (Exception e) {
            Log.error(e.getMessage());
            return 1;
        }
    }

    private int handleInsert() {
        if (itemId <= 0 || itemName == null || itemLink == null || itemPool == null)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("All parameters (ID, NAME, LINK, POOL) are required for insert"));

        if (!itemLink.startsWith("https://"))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Links must start with https://"));

        Item.Pool poolEnum;
        try {
            poolEnum = Item.Pool.valueOf(itemPool.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Invalid pool value, pick between H, M, and L"));
        }

        LocalDate lastRecall = LocalDate.now();
        if (itemLastRecall != null && !itemLastRecall.isBlank())
            lastRecall = parseDate(itemLastRecall);

        int recalls = totalRecalls != null ? totalRecalls : 0;
        if (recalls < 0)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_TOTAL_RECALLS must be non-negative"));

        Item item = new Item(itemId, itemName, itemLink, poolEnum, lastRecall, recalls);
        if (!parent.itemRepository.insertItem(item)) {
            Log.error("Insert failed, check for duplicate ID: " + item);
            return 1;
        }

        Log.info("Successfully added: " + item);
        return 0;
    }

    private int handleUpdate() {
        if (itemId <= 0)
            throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_ID must be a positive integer"));

        if (updateName == null && updateLink == null && updatePool == null
                && itemLastRecall == null && totalRecalls == null) {
            throw new ParameterException(spec.commandLine(),
                    Log.errorMsg("No update fields specified. Use one or more of --name, --link, --pool, --last-recall, --total-recalls"));
        }

        Item existing = parent.itemRepository.getItemById(itemId).orElse(null);
        if (existing == null) {
            Log.error("No item found with ID: " + itemId);
            return 1;
        }

        if (updateName != null && !updateName.isBlank())
            existing.setItemName(updateName);

        if (updateLink != null && !updateLink.isBlank()) {
            if (!updateLink.startsWith("https://"))
                throw new ParameterException(spec.commandLine(), Log.errorMsg("Links must start with https://"));
            existing.setItemLink(updateLink);
        }

        if (updatePool != null && !updatePool.isBlank()) {
            try {
                existing.setItemPool(Item.Pool.valueOf(updatePool.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ParameterException(spec.commandLine(), Log.errorMsg("Invalid pool value, pick between H, M, and L"));
            }
        }

        if (itemLastRecall != null && !itemLastRecall.isBlank())
            existing.setLastRecall(parseDate(itemLastRecall));

        if (totalRecalls != null) {
            if (totalRecalls < 0)
                throw new ParameterException(spec.commandLine(), Log.errorMsg("ITEM_TOTAL_RECALLS must be non-negative"));
            existing.setTotalRecalls(totalRecalls);
        }

        if (!parent.itemRepository.updateItem(existing)) {
            Log.error("Update failed: " + existing);
            return 1;
        }

        Log.info("Successfully updated: " + existing);
        return 0;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            if (date.isAfter(LocalDate.now()))
                throw new ParameterException(spec.commandLine(), "ITEM_LAST_RECALL cannot be in the future.\nLeave blank to initialize with today's date.");
            return date;
        } catch (DateTimeParseException e) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Invalid ITEM_LAST_RECALL, use YYYY-MM-DD format."));
        }
    }
}
