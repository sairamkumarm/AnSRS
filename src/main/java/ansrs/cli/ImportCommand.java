package ansrs.cli;

import ansrs.data.Item;
import ansrs.util.Log;
import ansrs.service.CSVImporter;
import picocli.CommandLine.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "import", description = """
        Import a csv into the database.
        The following format is mandatory.
        Header(Optional)
        (ITEM_ID, ITEM_NAME, ITEM_LINK, ITEM_POOL, ITEM_LAST_RECALL, totalRecalls)
        ITEM_ID: Integer > 0
        ITEM_NAME: Non-empty String
        ITEM_LINK: Non-empty String starting with 'https://'
        ITEM_POOL: Non-empty value belonging and limited to [H, M, L]
        ITEM_LAST_RECALL: Date string of format YYYY-MM-DD, or cannot be in the future, leave empty for today's date.
        ITEM_TOTAL_RECALLS: Integer >= 0
        """,
        mixinStandardHelpOptions = true)
public class ImportCommand implements Callable<Integer> {

    @Spec
    Model.CommandSpec spec;

    @ParentCommand
    SRSCommand parent;

    @Option(names = "--path", paramLabel = "CSV_FILE_PATH", description = "Path to the csv file", required = true)
    private String filePath;

    @Option(names = "--preserve", paramLabel = "PRESERVE_SOURCE", description = "[csv/db] Pick between overwriting csv values in db or preserving db values in the even of duplicate ITEM_IDs.", required = true)
    private String overwrite;

    @Override
    public Integer call() {
        validate();
        try {
            Log.info("Parsing CSV");
            CSVImporter csv = new CSVImporter(filePath);
            List<Item> items = csv.parse();
            if (items.isEmpty())
                throw new ParameterException(spec.commandLine(), Log.errorMsg("Import Failed: No Valid Rows"));
            Set<Integer> dbItemsSet = parent.itemDB.getAllItemsIds().orElse(new HashSet<>());
            boolean success = false;
            if (overwrite.equalsIgnoreCase("db")) {
                List<Item> duplicates = items.stream().filter(k -> dbItemsSet.contains(k.getItemId())).toList();
                List<Item> uniques = items.stream().filter(k -> !dbItemsSet.contains(k.getItemId())).toList();
                if (!uniques.isEmpty()) success = parent.itemDB.insertItemsBatch(uniques);
                if (!duplicates.isEmpty()) Log.warn(duplicates.size() + " duplicate item(s)");
                for (Item i : duplicates) Log.warn("Duplicate " + i.toString());
                if (success) {
                    Log.info("Import success: Added " + uniques.size() + " Unique Rows to database");
                    return 0;
                } else if (uniques.isEmpty()) Log.error("No Items to add to db");
                else Log.error("Import failed");
            } else {
                success = parent.itemDB.upsertItemsBatch(items);
                if (success) {
                    Log.info("Import success: Merged " + items.size() + " Valid Rows to database");
                    return 0;
                } else Log.error("Import failed");
            }
            return 1;
        } catch (ParameterException e) {
            throw e;
        } catch (Exception e) {
            Log.error("Import failed");
            return 1;
        }
    }

    private void validate() {
        if (filePath.isBlank())
            throw new ParameterException(spec.commandLine(), Log.errorMsg("File path is required for import"));
        if (!filePath.trim().endsWith(".csv"))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("CSV file expected"));
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Existing File required"));
        if (!Files.isRegularFile(path))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Regular file required"));
        if (!Files.isReadable(path))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("File is not readable"));
        if (!overwrite.equalsIgnoreCase("csv") && !overwrite.equalsIgnoreCase("db"))
            throw new ParameterException(spec.commandLine(), Log.errorMsg("Pick either --overwrite=csv or --overwrite=db"));
    }

}
