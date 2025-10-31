package dev.sai.srs.cli;

import dev.sai.srs.data.Item;
import dev.sai.srs.printer.Printer;
import dev.sai.srs.service.CSVImporter;
import picocli.CommandLine.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Command(name = "import", description = """
        Import a csv into the database.
        The following format is mandatory.
        Header(Optional)
        (ITEM_ID, ITEM_NAME, ITEM_LINK, ITEM_POOL, ITEM_LAST_RECALL, totalRecalls)
        ITEM_ID: Integer > 0
        ITEM_NAME: Non-empty String
        ITEM_LINK: Non-empty String starting with 'https://'
        ITEM_POOL: Non-empty value belonging and limited to [H, M, L]
        ITEM_LAST_RECALL: Date string of format YYYY-MM-DD, or leave empty for today's date.
        ITEM_TOTAL_RECALLS: Integer >= 0
        """,
        mixinStandardHelpOptions = true)
public class ImportCommand implements Runnable {

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Option(names = "--path", paramLabel = "CSV_FILE_PATH", description = "Path to the csv file", required = true)
    private String filePath;

    @Option(names = "--preserve", paramLabel = "PRESERVE_SOURCE", description = "[csv/db] Pick between overwriting csv values in db or preserving db values in the even of duplicate ITEM_IDs.", required = true)
    private String overwrite;

    @Override
    public void run() {
        validate();
        try {
            System.out.println("INFO: Parsing CSV");
            CSVImporter csv = new CSVImporter(filePath);
            List<Item> items = csv.parse();
            if (items.isEmpty()) throw new ParameterException(spec.commandLine(), "ERROR: Import Failed: No Valid Rows");
            Set<Integer> dbItemsSet = parent.db.getAllItemsIds().orElse(new HashSet<>());
            boolean success = false;
            if (overwrite.equalsIgnoreCase("db")) {
                List<Item> duplicates = items.stream().filter(k -> dbItemsSet.contains(k.getItemId())).toList();
                List<Item> uniques = items.stream().filter(k -> !dbItemsSet.contains(k.getItemId())).toList();
                if (!uniques.isEmpty()) success = parent.db.insertItemsBatch(uniques);
                if (!duplicates.isEmpty()) System.err.println("WARNING: "+duplicates.size() + " duplicate item(s)");
                for (Item i : duplicates) System.err.println("WARNING: Duplicate " + i.toString());
                if (success) System.out.println("INFO: Import success: Added " + uniques.size() + " Unique Rows to database");
                else if (uniques.isEmpty()) System.err.println("ERROR: No Items to add to db");
                else System.err.println("ERROR: Import failed");
            } else {
                success = parent.db.upsertItemsBatch(items);
                if (success) System.out.println("INFO: Import success: Merged " + items.size() + " Valid Rows to database");
                else System.err.println("ERROR: Import failed");
            }
            if (parent.debug) Printer.statePrinter(parent.workingSet, parent.completedSet, parent.db);
        } catch (ParameterException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("ERROR: Import failed");
        }
    }

    private void validate() {
        if (filePath.isBlank()) throw new ParameterException(spec.commandLine(), "File path required");
        if (!filePath.trim().endsWith(".csv")) throw new ParameterException(spec.commandLine(), "CSV file expected");
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) throw new ParameterException(spec.commandLine(), "Existing File required");
        if (!Files.isRegularFile(path)) throw new ParameterException(spec.commandLine(), "Regular file required");
        if (!Files.isReadable(path)) throw new ParameterException(spec.commandLine(), "File is not readable");
        if (!overwrite.equalsIgnoreCase("csv") && !overwrite.equalsIgnoreCase("db"))
            throw new ParameterException(spec.commandLine(), "Pick either --overwrite=csv or --overwrite=db");
    }

}
