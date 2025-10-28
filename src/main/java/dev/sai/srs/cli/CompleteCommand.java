package dev.sai.srs.cli;

import dev.sai.srs.data.Problem;
import picocli.CommandLine;

import java.util.Optional;
import java.util.Set;

@CommandLine.Command(name = "complete",
        description = "marks problem as completed",
        mixinStandardHelpOptions = true)
public class CompleteCommand implements Runnable {

    @CommandLine.ParentCommand
    private SRSCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "PROBLEM_ID", description = "Problem ID")
    private int problemId;

    @CommandLine.Option(names = {"-u", "--update"}, paramLabel = "PROBLEM_POOL", description = "Pool value update for problem, choose from H M and L", required = false)
    private String poolString;

    @CommandLine.Option(names = {"-f", "--force"}, description = "forces completion of a problem non existent in session cache.")
    private boolean force;

    @Override
    public void run() {
//        System.out.println("recieved complete args "+problemId + " " + poolString + " " + force);
//        System.out.println(parent.sessionCache.problemIds);
        if (problemId <= 0) {
            System.err.println("Problem id required to be positive");
            return;
        }
        if (!parent.db.contains(problemId)) {
            System.err.println("Problem Id non-existent in database.");
            return;
        }
        Set<Integer> pids = parent.sessionCache.getProblemIdSet();
        if (!pids.contains(problemId) && !force) {
            System.err.println("Problem Id " + problemId + " doesn't exist in cache");
            return;
        }
        Problem.Pool pool = null;
        if (poolString != null) {
            try {
                pool = Problem.Pool.valueOf(poolString.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println(poolString + " is not a valid Pool value.");
                return;
            }
        }
        if (pids.contains((Integer) problemId)) {
            parent.sessionCache.removeProblem(problemId);
            if (poolString == null || poolString.isEmpty()) parent.updateCache.addProblem(problemId, null);
            else {
                assert pool != null;
                parent.updateCache.addProblem(problemId, pool);
            }
            System.out.println("done without force");
            return;
        }
        if (!pids.contains(problemId) && force) {
            Optional<Problem> problemById = parent.db.getProblemById(problemId);
            if (problemById.isPresent()) {
                Problem problem = problemById.get();
                parent.sessionCache.removeProblem(problemId);
                parent.updateCache.addProblem(problem.getProblemId(), problem.getProblemPool());
                System.out.println("done with force");
                return;
            } else {
                System.err.println("Problem Id " + problemId + " doesn't exist in database");
                return;
            }
        }
    }
}
