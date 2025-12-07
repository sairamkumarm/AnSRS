package ansrs.util;

import ansrs.data.Group;
import ansrs.data.Item;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import ansrs.db.ItemRepository;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Printer {
    public static final String bold = "\u001B[1m";
    public static final String dim = "\u001B[2m";
    public static final String reset = "\u001B[22m";
    public static void statePrinter(WorkingSet workingSet, CompletedSet completedSet, ItemRepository ItemRepository) {
        System.out.println("+------------SYSTEM STATUS-------------+");

        System.out.println("WorkingSet: "+ workingSet.getItemIdSet());

        System.out.println("CompletedSet: "+ completedSet.getItems());

        Optional<List<Item>> itemsList = ItemRepository.getAllItems();
        System.out.println("Database:");
        if (itemsList.isPresent()) {
            printItemsList(itemsList.get());
        } else {
            System.out.println(dim+"+-----------------------+");
            System.out.println("|   "+reset+"Database is empty"+dim+"   |");
            System.out.println("+-----------------------+"+reset);
        }

    }
    public static void setStatePrinter(WorkingSet workingSet, CompletedSet completedSet, ItemRepository db) {
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

    public static void printGroupRow(Group group) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        String createdAt = group.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter);
        String updatedAt = group.updatedAt.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter);
        // Top separator
        System.out.println(dim+"-------------------------------------------------------------"+reset);

        // Line 1: Metadata (fixed, annotated)
        System.out.printf(dim+"ID: "+reset+"%3s"+reset+dim+"   CREATED: "+reset+"%s"+reset+dim+"  LAST_UPDATED: "+reset+"%s"+"%n",
                group.id, createdAt, updatedAt);

        // Line 2: Name
        System.out.println(dim+"NAME: "+reset+bold + group.name + reset);

        // Line 3: Link
        if(group.link!=null && !group.link.isBlank()) System.out.println(group.link);

    }

    public static void printGroup(Group group) {
        if (group == null) {
            System.out.println(dim+"+------------------------+");
            System.out.println("|   "+reset+"Group Non-existent"+dim+"   |");
            System.out.println("+------------------------+"+reset);
            return;
        }
        printGroupRow(group);
        System.out.println(dim+"-------------------------------------------------------------"+reset);
    }

    public static void printGroupsList(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            System.out.println(dim+"+------------------------+");
            System.out.println("|  "+reset+"No Groups to display"+dim+"  |");
            System.out.println("+------------------------+"+reset);
            return;
        }

        for (Group group: groups) {
            printGroupRow(group);
        }
        System.out.println(dim + "-------------------------------------------------------------"+reset);
        System.out.println(dim+"Total: "+reset+bold+groups.size()+reset+dim+" group(s)"+reset);
        System.out.println(dim + "-------------------------------------------------------------"+reset);

    }
}