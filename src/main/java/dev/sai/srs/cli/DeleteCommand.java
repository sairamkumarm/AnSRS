package dev.sai.srs.cli;

import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

@Command(name = "delete",
        description = "delete lets you remove from session queue, completed queue and db, depending on the flags, by default it removes from session queue",
        mixinStandardHelpOptions = true)
public class DeleteCommand implements Runnable {

    @ParentCommand
    private SRSCommand parent;

    @Spec
    private Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "PROBLEM_ID", description = "Problem Id", defaultValue = "0")
    private int problemId;

    @Option(names = {"-c", "--completed"}, description = "Removes from problems that are completed but, are yet to be commited to the database.")
    private boolean deleteFromUpdateCache;

    @Option(names = {"-d", "--database"}, description = "Removes a problem from the database and caches")
    private boolean deleteFromDatabase;

    @Option(names = "--sure", description = "A defensive fallback to prevent accidental deletions", required = true)
    private boolean sure;

    @Option(names = {"--hard-reset"}, description = "Hard resets all the persistent data, cache included")
    private boolean reset;

    @Override
    public void run() {
        if (!sure) {
            throw new ParameterException(spec.commandLine(),"--sure flag is needed to perform deletions.");
        }
        if (problemId==0){
            if (reset) {
                if (
                parent.db.clearDatabase()&&
                parent.updateCache.clearCache()&&
                parent.sessionCache.clearCache()
                )
                    System.out.println("Reset successful");
                else System.err.println("Reset failed");
                return;
            }
            else throw new ParameterException(spec.commandLine(),"PROBLEM_ID can be skipped only during the --reset command");
        }

        if (problemId < 0) {
            throw new ParameterException(spec.commandLine(),"Problem Id ["+problemId+"] cannot be non-positive");
        }
        if (deleteFromDatabase) {
            if (!parent.db.deleteProblemById(problemId)) {
                System.err.println("Error in deleting problem ["+problemId+"] from DB, use --debug to confirm its existence");
                return;
            }
            System.out.println("Problem ["+problemId+"] deleted from database");
        }
        if (deleteFromUpdateCache) {
            if (!parent.updateCache.removeProblem(problemId)) {
                throw new ParameterException(spec.commandLine(),"Problem ["+problemId+"] non-existent in update cache");
            } else {
                System.out.println("Problem ["+problemId+"] deleted from update cache");
            }
        }
        if (!parent.sessionCache.removeProblem(problemId)) {
            throw new ParameterException(spec.commandLine(),"Problem ["+problemId+"] non-existent in session cache");
        } else {
            System.out.println("Problem ["+problemId+"] deleted from session");
        }

        if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);

    }

}
