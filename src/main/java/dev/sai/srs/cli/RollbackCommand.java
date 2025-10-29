package dev.sai.srs.cli;


import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.util.HashMap;

@Command(name = "rollback", description = "Rolls back problems from completed state to session state", mixinStandardHelpOptions = true)
public class RollbackCommand implements Runnable {

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Parameters(paramLabel = "PROBLEM_ID", description = "Unique identifier of a problem", defaultValue = "0")
    private int problemId;

    @Option(names = {"-a", "--all"}, description = "Rollback all problems from session", required = false)
    private boolean all;

    @Override
    public void run() {
        validate();
        if (all) {
            HashMap<Integer, UpdateCache.Pair<Problem.Pool, LocalDate>> updateCacheProblems = parent.updateCache.getProblems();
            if (parent.sessionCache.fillCache(updateCacheProblems.keySet()) &&
                            parent.updateCache.clearCache()) System.out.println("Full rollback complete.");
            else System.err.println("Rollback failed.");
        } else {
            if (parent.sessionCache.addProblem(problemId) &&
                            parent.updateCache.removeProblem(problemId))
                System.out.println("Problem [" + problemId + "] rollback complete.");
            else System.err.println("Problem [" + problemId + "] rollback failed.");
        }
        if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);
    }

    private void validate() {
        if (problemId < 0)
            throw new ParameterException(spec.commandLine(), "PROBLEM_ID [" + problemId + "] cannot be non-positive.");
        if ((problemId == 0 && !all) || (problemId != 0 && all))
            throw new ParameterException(spec.commandLine(), "Either PROBLEM_ID must be specified, or --all must be used, they are mutually exclusive.");
        if (parent.updateCache.getProblems().isEmpty())
            throw new ParameterException(spec.commandLine(), "Nothing to rollback.");
        if (problemId!=0 && !parent.updateCache.containsProblem(problemId))
            throw new ParameterException(spec.commandLine(), "PROBLEM_ID [" + problemId + "] non-existent.");
    }

}
