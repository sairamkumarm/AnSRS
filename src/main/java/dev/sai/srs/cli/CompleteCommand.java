package dev.sai.srs.cli;

import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;

@Command(name = "complete",
        description = "marks problem as completed",
        mixinStandardHelpOptions = true)
public class CompleteCommand implements Runnable {

    @ParentCommand
    private SRSCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "PROBLEM_ID", description = "The PROBLEM_ID that identifies the problem", defaultValue = "0")
    private int problemId;

    @Option(names = {"--update"}, paramLabel = "PROBLEM_POOL", description = "Optional PROBLEM_POOL value updated for problem, choose from H M and L", required = false)
    private String poolString;

    @Option(names = "--date", paramLabel = "PROBLEM_LAST_RECALL", description = "Optional PROBLEM_LAST_RECALL specification, use YYYY-MM-DD format", required = false)
    private String date;

    @Option(names = {"-f", "--force"}, description = "Forces completion of a problem non existent in session.")
    private boolean force;

    @Option(names = {"-a", "--all"}, description = "Completes all problems in the session, but you lose the ability to update problem pools")
    private boolean allComplete;

    @Override
    public void run() {
        validate();

        Set<Integer> sessionCacheProblemIdSet = parent.sessionCache.getProblemIdSet();
        if (allComplete && problemId == 0) {
            for (int id : sessionCacheProblemIdSet) {
                parent.updateCache.addProblem(id, null);
                parent.sessionCache.removeProblem(id);
            }
            System.out.println("All problems in session completed.");
            return;
        }

        Problem.Pool pool = (poolString != null) ? Problem.Pool.valueOf(poolString.toUpperCase()) : null;
        LocalDate lastRecall = (date!=null) ? LocalDate.parse(date):LocalDate.now();

        if (!sessionCacheProblemIdSet.contains(problemId)) {
            if (!force) throw new ParameterException(spec.commandLine(), "PROBLEM_ID [" + problemId + "] doesn't exist in cache");
            Problem problem = parent.db.getProblemById(problemId).
                    orElseThrow(
                            () -> new ParameterException(
                                    spec.commandLine(),
                                    "PROBLEM_ID [" + problemId + "] doesn't exist in database")
                    );
            parent.updateCache.addProblem(problem.getProblemId(), problem.getProblemPool(), lastRecall);
        } else {
            parent.sessionCache.removeProblem(problemId);
            if (poolString == null || poolString.isEmpty()) parent.updateCache.addProblem(problemId, null, lastRecall);
            else {
                parent.updateCache.addProblem(problemId, pool, lastRecall);
            }
        }
        System.out.println("Completed PROBLEM_ID [" + problemId + "]" + ((pool != null) ? (", and moved problem to PROBLEM_POOL[" + pool.name() + "]") : "") + ((!lastRecall.equals(LocalDate.now()))?", with PROBLEM_LAST_RECALL["+lastRecall.toString()+"]":""));
        if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);

    }


    private void validate() {
        if (problemId < 0) {
            throw new ParameterException(spec.commandLine(), "PROBLEM_ID [" + problemId + "] required to be positive");
        }
        if (problemId == 0 && !allComplete) {
            throw new ParameterException(spec.commandLine(), "PROBLEM_ID can be [" + problemId + "] only when --all flag is used to completed all the problems, otherwise, it is required to be positive");
        }
        if (problemId != 0) {
            if (allComplete) {
                throw new ParameterException(spec.commandLine(), "--all / -a flag can only be used when PROBLEM_ID is not specified");
            }
            if (!parent.db.contains(problemId)) {
                throw new ParameterException(spec.commandLine(), "PROBLEM_ID [" + problemId + "] non-existent in database.");
            }
            if (poolString != null) {
                try {
                    Problem.Pool.valueOf(poolString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ParameterException(spec.commandLine(), poolString + " is not a valid PROBLEM_POOL value.");
                }
            }
            if (date != null){
                try {
                    LocalDate.parse(date);
                }catch (DateTimeParseException e){
                    throw new ParameterException(spec.commandLine(), date + " is not a valid PROBLEM_LAST_RECALL value. use YYYY-MM-DD format.");
                }
            }
        }


    }
}
