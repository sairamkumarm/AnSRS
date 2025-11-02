package ansrs.util;

import ansrs.data.Item;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import ansrs.db.DBManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

public class Printer {
    public static final String bold = "\u001B[1m";
    public static final String dim = "\u001B[2m";
    public static final String reset = "\u001B[22m";
    public static void statePrinter(WorkingSet workingSet, CompletedSet completedSet, DBManager DBManager) {
        System.out.println("+------------SYSTEM STATUS-------------+");

        System.out.println("WorkingSet: "+ workingSet.getItemIdSet());

        System.out.println("CompletedSet: "+ completedSet.getItems());

        Optional<List<Item>> itemsList = DBManager.getAllItems();
        System.out.println("Database:");
        if (itemsList.isPresent()) {
            printItemsGrid(itemsList.get());
        } else {
            System.out.println("+-----------------------+");
            System.out.println("|   Database is empty   |");
            System.out.println("+-----------------------+");
        }
    }
    public static void printItemsGrid(List<Item> items) {
        if (items == null || items.isEmpty()) {
            System.out.println("No items to display");
            return;
        }

        for (Item item : items) {
            printItemRow(item);
        }
        System.out.println(dim + "---------------------------------------------------------------"+reset);
        System.out.println(dim+"Total: "+reset+bold+items.size()+reset+dim+" item(s)"+reset);
        System.out.println(dim + "---------------------------------------------------------------"+reset);

    }

    public static void printItemRow(Item item) {

        // Top separator
        System.out.println(bold+"---------------------------------------------------------------"+reset);

        // Line 1: Metadata (fixed, annotated)
        System.out.printf(dim+"ID: "+reset+"%4s"+reset+dim+"  POOL: "+reset+"%s"+reset+dim+"  RECALLS: "+reset+"%04d"+reset+dim+"  LAST_RECALL: "+reset+"%s"+"%n",
                item.getItemId(),
                item.getItemPool(),
                item.getTotalRecalls(),
                item.getLastRecall());

        // Line 2: Name
        System.out.println(bold + item.getItemName() + reset);

        // Line 3: Link
        System.out.println(item.getItemLink());

    }

}