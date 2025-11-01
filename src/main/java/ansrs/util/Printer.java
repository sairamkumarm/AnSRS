package ansrs.util;

import ansrs.data.Item;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import ansrs.db.DBManager;

import java.util.List;
import java.util.Optional;

public class Printer {

    private static final int ID_WIDTH = 6;
    private static final int NAME_WIDTH = 25;
    private static final int LINK_WIDTH = 60;
    private static final int POOL_WIDTH = 4;
    private static final int RECALL_WIDTH = 11;
    private static final int RECALLS_WIDTH = 7;

    public static void printItemsGrid(List<Item> items) {
        if (items == null || items.isEmpty()) {
            System.out.println("+---------------------------+");
            System.out.println("|  No items to display      |");
            System.out.println("+---------------------------+");
            return;
        }

        printTopBorder();
        printHeader();
        printMiddleBorder();

        for (Item p : items) {
            printItemRow(p);
        }

        printBottomBorder();
        System.out.println("Total: " + items.size() + " item(s)");
    }

    private static void printTopBorder() {
        System.out.print("+");
        System.out.print("-".repeat(ID_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(NAME_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(LINK_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(POOL_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(RECALL_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(RECALLS_WIDTH + 2));
        System.out.println("+");
    }

    private static void printMiddleBorder() {
        System.out.print("+");
        System.out.print("-".repeat(ID_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(NAME_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(LINK_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(POOL_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(RECALL_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(RECALLS_WIDTH + 2));
        System.out.println("+");
    }

    private static void printBottomBorder() {
        System.out.print("+");
        System.out.print("-".repeat(ID_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(NAME_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(LINK_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(POOL_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(RECALL_WIDTH + 2));
        System.out.print("+");
        System.out.print("-".repeat(RECALLS_WIDTH + 2));
        System.out.println("+");
    }

    private static void printHeader() {
        System.out.printf("| %-6s | %-25s | %-60s | %-4s | %-11s | %-7s |%n",
                "ID", "Name", "Link", "Pool", "Last Recall", "Recalls");
    }

    private static void printItemRow(Item p) {
        System.out.printf("| %-6s | %-25s | %-60s | %-4s | %-11s | %-7s |%n",
                p.getItemId(),
                truncate(p.getItemName(), NAME_WIDTH),
                truncate(p.getItemLink(), LINK_WIDTH),
                p.getItemPool(),
                p.getLastRecall(),
                p.getTotalRecalls()
        );
    }

    private static String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }

    public static void statePrinter(WorkingSet workingSet, CompletedSet completedSet, DBManager DBManager) {
        System.out.println("+------------SYSTEM STATUS-------------+");

        System.out.println("WorkingSet: "+ workingSet.getItemIdSet());

        System.out.println("CompletedSet: "+ completedSet.getItems());

        Optional<List<Item>> itemsList = DBManager.getAllItems();
        if (itemsList.isPresent()) {
            printItemsGrid(itemsList.get());
        } else {
            System.out.println("+-----------------------+");
            System.out.println("|  Database is empty    |");
            System.out.println("+-----------------------+");
        }
    }
}