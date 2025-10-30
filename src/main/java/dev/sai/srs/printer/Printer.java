package dev.sai.srs.printer;

import dev.sai.srs.data.Item;
import dev.sai.srs.set.CompletedSet;
import dev.sai.srs.set.WorkingSet;
import dev.sai.srs.db.DuckDBManager;

import java.util.List;
import java.util.Optional;

public class Printer {

    // Box drawing characters
    private static final String TOP_LEFT = "╔";
    private static final String TOP_RIGHT = "╗";
    private static final String BOTTOM_LEFT = "╚";
    private static final String BOTTOM_RIGHT = "╝";
    private static final String HORIZONTAL = "═";
    private static final String VERTICAL = "║";
    private static final String T_DOWN = "╦";
    private static final String T_UP = "╩";
    private static final String T_RIGHT = "╠";
    private static final String T_LEFT = "╣";
    private static final String CROSS = "╬";

    private static final int ID_WIDTH = 6;
    private static final int NAME_WIDTH = 25;
    private static final int LINK_WIDTH = 60;
    private static final int POOL_WIDTH = 4;
    private static final int RECALL_WIDTH = 11;
    private static final int RECALLS_WIDTH = 7;

    public static void printItemsGrid(List<Item> items) {
        if (items == null || items.isEmpty()) {
            System.out.println("╭──────────────────────────╮");
            System.out.println("│  No items to display  │");
            System.out.println("╰──────────────────────────╯");
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
        System.out.print(TOP_LEFT);
        System.out.print(HORIZONTAL.repeat(ID_WIDTH + 2));
        System.out.print(T_DOWN);
        System.out.print(HORIZONTAL.repeat(NAME_WIDTH + 2));
        System.out.print(T_DOWN);
        System.out.print(HORIZONTAL.repeat(LINK_WIDTH + 2));
        System.out.print(T_DOWN);
        System.out.print(HORIZONTAL.repeat(POOL_WIDTH + 2));
        System.out.print(T_DOWN);
        System.out.print(HORIZONTAL.repeat(RECALL_WIDTH + 2));
        System.out.print(T_DOWN);
        System.out.print(HORIZONTAL.repeat(RECALLS_WIDTH + 2));
        System.out.println(TOP_RIGHT);
    }

    private static void printMiddleBorder() {
        System.out.print(T_RIGHT);
        System.out.print(HORIZONTAL.repeat(ID_WIDTH + 2));
        System.out.print(CROSS);
        System.out.print(HORIZONTAL.repeat(NAME_WIDTH + 2));
        System.out.print(CROSS);
        System.out.print(HORIZONTAL.repeat(LINK_WIDTH + 2));
        System.out.print(CROSS);
        System.out.print(HORIZONTAL.repeat(POOL_WIDTH + 2));
        System.out.print(CROSS);
        System.out.print(HORIZONTAL.repeat(RECALL_WIDTH + 2));
        System.out.print(CROSS);
        System.out.print(HORIZONTAL.repeat(RECALLS_WIDTH + 2));
        System.out.println(T_LEFT);
    }

    private static void printBottomBorder() {
        System.out.print(BOTTOM_LEFT);
        System.out.print(HORIZONTAL.repeat(ID_WIDTH + 2));
        System.out.print(T_UP);
        System.out.print(HORIZONTAL.repeat(NAME_WIDTH + 2));
        System.out.print(T_UP);
        System.out.print(HORIZONTAL.repeat(LINK_WIDTH + 2));
        System.out.print(T_UP);
        System.out.print(HORIZONTAL.repeat(POOL_WIDTH + 2));
        System.out.print(T_UP);
        System.out.print(HORIZONTAL.repeat(RECALL_WIDTH + 2));
        System.out.print(T_UP);
        System.out.print(HORIZONTAL.repeat(RECALLS_WIDTH + 2));
        System.out.println(BOTTOM_RIGHT);
    }

    private static void printHeader() {
        System.out.printf("%s %-6s %s %-25s %s %-60s %s %-4s %s %-11s %s %-7s %s%n",
                VERTICAL, "ID", VERTICAL, "Name", VERTICAL, "Link",
                VERTICAL, "Pool", VERTICAL, "Last Recall", VERTICAL, "Recalls", VERTICAL);
    }

    private static void printItemRow(Item p) {
        System.out.printf("%s %-6s %s %-25s %s %-60s %s %-4s %s %-11s %s %-7s %s%n",
                VERTICAL,
                p.getItemId(),
                VERTICAL,
                truncate(p.getItemName(), NAME_WIDTH),
                VERTICAL,
                truncate(p.getItemLink(), LINK_WIDTH),
                VERTICAL,
                p.getItemPool(),
                VERTICAL,
                p.getLastRecall(),
                VERTICAL,
                p.getTotalRecalls(),
                VERTICAL
        );
    }

    private static String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }

    public static void statePrinter(WorkingSet workingSet, CompletedSet completedSet, DuckDBManager duckDBManager) {
        System.out.println("╭────────────SYSTEM STATUS─────────────╮");

        System.out.println("WorkingSet: "+ workingSet.getItemIdSet());

        System.out.println("CompletedSet: "+ completedSet.getItems());

        Optional<List<Item>> itemsList = duckDBManager.getAllItems();
        if (itemsList.isPresent()) {
            printItemsGrid(itemsList.get());
        } else {
            System.out.println("╭───────────────────────╮");
            System.out.println("│  Database is empty   │");
            System.out.println("╰───────────────────────╯");
        }
    }
}