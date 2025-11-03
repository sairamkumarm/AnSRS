package ansrs.util;

import ansrs.data.Item;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import ansrs.db.DBManager;

import java.time.LocalDate;
import java.util.ArrayList;
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
            printItemsList(itemsList.get());
        } else {
            System.out.println(dim+"+-----------------------+");
            System.out.println("|   "+reset+"Database is empty"+dim+"   |");
            System.out.println("+-----------------------+"+reset);
        }

    }
    public static void setStatePrinter(WorkingSet workingSet, CompletedSet completedSet, DBManager db) {
        System.out.println("+------------SYSTEM STATUS-------------+");
        System.out.println("WorkingSet: ");
        List<Item> workingSetItems = db.getItemsFromList(workingSet.getItemIdList()).orElse(new ArrayList<>());
        printItemsList(workingSetItems);
        System.out.println("CompletedSet: ");
        List<Item> completedSetChangeList = db.getItemsFromList((completedSet.getItems().keySet().stream().toList())).orElse(new ArrayList<>());
        for (Item i: completedSetChangeList){
            CompletedSet.Pair<Item.Pool, LocalDate> poolLocalDatePair = completedSet.getItems().get(i.getItemId());
            if (poolLocalDatePair.getPool() != null) i.setItemPool(poolLocalDatePair.getPool());
            i.setLastRecall(poolLocalDatePair.getLast_recall());
            i.setTotalRecalls(i.getTotalRecalls()+1);
        }
        printItemsList(completedSetChangeList);

    }

    public static void printItem(Item item){
        if (item==null){
            System.out.println(dim+"+-----------------------+");
            System.out.println("|   "+reset+"Item non-existent"+dim+"   |");
            System.out.println("+-----------------------+"+reset);
        } else {
            printItemRow(item);
            System.out.println(dim + "---------------------------------------------------------------"+reset);
        }
    }

    public static void printItemsList(List<Item> items) {
        if (items == null || items.isEmpty()) {
            System.out.println(dim+"+-----------------------+");
            System.out.println("|  "+reset+"No Items to display"+dim+"  |");
            System.out.println("+-----------------------+"+reset);
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
        System.out.println(dim+"---------------------------------------------------------------"+reset);

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