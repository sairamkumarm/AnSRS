package dev.sai.srs.cli;

import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

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

    @Parameters(index = "0", paramLabel = "PROBLEM_ID", description = "Problem ID")
    private int problemId;

    @Option(names = {"-u", "--update"}, paramLabel = "PROBLEM_POOL", description = "Pool value update for problem, choose from H M and L", required = false)
    private String poolString;

    @Option(names = {"-f", "--force"}, description = "forces completion of a problem non existent in session cache.")
    private boolean force;

    @Override
    public void run() {
            validate();

            Set<Integer> problemIdSet = parent.sessionCache.getProblemIdSet();
            Problem.Pool pool = (poolString!=null)?Problem.Pool.valueOf(poolString.toUpperCase()):null;

            if (!problemIdSet.contains(problemId)) {
                if (!force) throw new ParameterException(spec.commandLine(),"Problem Id " + problemId + " doesn't exist in cache");
                Optional<Problem> problemById = parent.db.getProblemById(problemId);
                if (problemById.isEmpty()) {
                    throw new ParameterException(spec.commandLine(),"Problem Id " + problemId + " doesn't exist in database");
                }
                Problem problem = problemById.get();
                parent.sessionCache.removeProblem(problemId);
                parent.updateCache.addProblem(problem.getProblemId(), problem.getProblemPool());
            }
            else {
                parent.sessionCache.removeProblem(problemId);
                if (poolString == null || poolString.isEmpty()) parent.updateCache.addProblem(problemId, null);
                else {
                    parent.updateCache.addProblem(problemId, pool);
                }
            }
            System.out.println("Completed problem ["+problemId+"]" + ((pool!=null)?(", and moved problem to "+pool.name()):""));
            if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);

    }


    private void validate(){
        if (problemId <= 0) {
            throw new ParameterException(spec.commandLine(),"Problem id  " + problemId + "  required to be positive");
        }
        if (!parent.db.contains(problemId)) {
            throw new ParameterException(spec.commandLine(),"Problem Id " + problemId + " non-existent in database.");
        }
        if (poolString != null) {
            try {
                Problem.Pool.valueOf(poolString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ParameterException(spec.commandLine(),poolString + " is not a valid Pool value.");
            }
        }
    }
}
