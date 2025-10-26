package dev.sai.srs.printer;

import dev.sai.srs.cache.SessionCache;
import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.data.Problem;
import dev.sai.srs.db.DuckDBManager;

import java.util.List;
import java.util.Optional;

public class Printer {
    public static void printProblemsGrid(List<Problem> problems) {
        if (problems == null || problems.isEmpty()) {
            System.out.println("No problems to display");
            return;
        }

        String format = "| %-10s | %-25s | %-60s | %-8s | %-12s | %-12s |%n";

        System.out.format(format, "ID", "Name", "Link", "Pool", "Last Recall", "Recalls");
        System.out.println(new String(new char[150]).replace("\0", "-"));

        for (Problem p : problems) {
            System.out.format(
                    format,
                    p.getProblemId(),
                    truncate(p.getProblemName(), 25),
                    p.getProblemLink(),  // no truncate here
                    p.getProblemPool(),
                    p.getLastRecall(),
                    p.getTotalRecalls()
            );
        }
    }

    private static String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }


    public static void statePrinter(SessionCache sessionCache, UpdateCache updateCache, DuckDBManager duckDBManager){
        System.out.println("---------STATUS----------");
        System.out.println("Session Cache state: "+sessionCache.getProblemIdSet());
        System.out.println("Update Cache state: "+updateCache.getProblems());
        System.out.println("Database state");
        Optional<List<Problem>> problemsList = duckDBManager.getAllProblems();
        if (problemsList.isPresent()){
            printProblemsGrid(problemsList.get());
        } else {
            System.out.println("Db empty");
        }
    }
}
