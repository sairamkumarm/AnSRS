package ansrs.cli;


import ansrs.data.Item;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;

@Command(name = "add",
        description = "Add new items into the item database or update an existing one",
        mixinStandardHelpOptions = true)
public class AddCommand implements Runnable {

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Parameters(index = "0", paramLabel = "ITEM_ID", description = "Unique identifier of an item")
    private int itemId;

    @Parameters(index = "1", paramLabel = "ITEM_NAME", description = "Name of the item, as in title, use Quotes")
    private String itemName;

    @Parameters(index = "2", paramLabel = "ITEM_LINK", description = "Link to the item, optional quotes")
    private String itemLink;

    @Parameters(index = "3", paramLabel = "ITEM_POOL", description = "Pick from H, M, and L, (HIGH/MEDIUM/LOW)")
    private String itemPool;

    @Option(names = {"-u", "--update"}, description = "To be used while updating an existing item", required = false)
    private boolean update;

    @Override
    public void run() {
        validate();
        Item.Pool poolEnum = Item.Pool.valueOf(itemPool.toUpperCase());
        Item item = new Item(itemId, itemName, itemLink, poolEnum, LocalDate.now(), 0);
        try {
            if (!parent.db.insertItem(item)) {
                if (update) {
                    if (!parent.db.updateItem(item)) Log.error("Update Failed");
                } else {
                    Log.error("Insert failed");
                    return;
                }
            }
            Log.info("Successfully added item");
        } catch (Exception e) {
            Log.error(e.getMessage());
            return;
        }
        if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
    }

    private void validate() {
        if (itemId <= 0 || itemName == null || itemName.isEmpty() || itemLink == null || itemLink.isEmpty() || itemPool == null || itemPool.isEmpty()) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Invalid parameters, all parameters are required to proceed"));
        }
        if (!itemLink.startsWith("https://")) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("use https:// for links, to prevent accidental passage of wrong order parameters"));
        }
        try {
            Item.Pool.valueOf(itemPool.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Invalid pool value, pick between H, M and L"));
        }
    }
}
