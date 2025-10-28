package dev.sai.srs.cli;

import dev.sai.srs.printer.Printer;
import picocli.CommandLine;

@CommandLine.Command(name = "delete",
        description = "delete lets you remove from session queue, completed queue and db, depending on the flags, by default it removes from session queue",
        mixinStandardHelpOptions = true)
public class DeleteCommand implements Runnable {

    @CommandLine.ParentCommand
    private SRSCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "PROBLEM_ID", description = "Problem Id")
    private int problemId;

    @CommandLine.Option(names = {"-c", "--completed"}, description = "Removes from problems that are completed but, are yet to be commited to the database.")
    private boolean deleteFromUpdateCache;

    @CommandLine.Option(names = {"-d", "--database"}, description = "Removes a problem from the database and caches")
    private boolean deleteFromDatabase;

    @CommandLine.Option(names = "--sure", description = "A defensive fallback to prevent accidental deletions", required = true)
    private boolean sure;

    @CommandLine.Option(names = {"-r", "--reset"}, description = "Hard resets all the persistent data, cache included")
    private boolean reset;

    @Override
    public void run() {
        if (!sure) {
            System.err.println("--sure flag is needed to perform deletions.");
            return;
        }
        if (problemId <= 0) {
            System.err.println("Problem Id cannot be non-positive");
            return;
        }
        if (deleteFromDatabase) {
            if (!parent.db.deleteProblemById(problemId)) {
                System.err.println("Error in deleting problem from DB");
                return;
            }
            System.out.println("Problem deleted from database");
        }
        if (deleteFromUpdateCache) {
            if (!parent.updateCache.removeProblem(problemId)) {
                System.err.println("Problem non-existent in update cache");
                return;
            } else {
                System.out.println("Problem deleted from update cache");
            }
        }
        if (!parent.sessionCache.removeProblem(problemId)) {
            System.err.println("Problem non-existent in session cache");
        } else {
            System.out.println("Problem deleted from session");
        }

        if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);

    }

}
