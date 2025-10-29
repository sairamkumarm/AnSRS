package dev.sai.srs.cli;


import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;

@Command(name = "add",
        description = "add new problems into the problem database or update an existing one",
        mixinStandardHelpOptions = true)
public class AddCommand implements Runnable {

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Parameters(index = "0", paramLabel = "PROBLEM_ID", description = "Problem Id")
    private int problemId;

    @Parameters(index = "1", paramLabel = "PROBLEM_NAME", description = "Name of the problem, as in title, use Quotes")
    private String problemName;

    @Parameters(index = "2", paramLabel = "PROBLEM_LINK", description = "Link to the problem, optional quotes")
    private String problemLink;

    @Parameters(index = "3", paramLabel = "PROBLEM_POOL", description = "Pick from H, M, and L, (HIGH/MEDIUM/LOW)")
    private String problemPool;

    @Option(names = {"-u", "--update"}, description = "To be used while updating an existing problem", required = false)
    private boolean update;

    @Override
    public void run() {
        validate();
        Problem.Pool poolEnum = Problem.Pool.valueOf(problemPool.toUpperCase());
        Problem problem = new Problem(problemId, problemName, problemLink, poolEnum, LocalDate.now(), 0);
        try {
            if (!parent.db.insertProblem(problem)) {
                if (update) {
                    if (!parent.db.updateProblem(problem)) System.err.println("Update Failed");
                } else {
                    System.err.println("Insert failed");
                    return;
                }
            }
            System.out.println("Successfully added problem");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);
    }

    private void validate() {
        if (problemId <= 0 || problemName == null || problemName.isEmpty() || problemLink == null || problemLink.isEmpty() || problemPool == null || problemPool.isEmpty()) {
            throw new ParameterException(spec.commandLine(), "Invalid parameters, all parameters are required to proceed");
        }
        if (!problemLink.startsWith("https://")) {
            throw new ParameterException(spec.commandLine(), "use https:// for links, to prevent accidental passage of wrong order parameters");
        }
        try {
            Problem.Pool.valueOf(problemPool.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), "Invalid pool value, pick between H, M and L");
        }
    }
}
