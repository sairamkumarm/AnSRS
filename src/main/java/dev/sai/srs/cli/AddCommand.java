package dev.sai.srs.cli;


import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine;
import java.time.LocalDate;

@CommandLine.Command(name = "add", description = "add new problems into the problem database or update an existing one")
public class AddCommand implements Runnable {

    @CommandLine.ParentCommand
    private SRSCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "ProblemID", description = "Problem Id")
    private int problemId;

    @CommandLine.Parameters(index = "1", paramLabel = "ProblemName", description = "Name of the problem, as in title, use Quotes")
    private String problemName;

    @CommandLine.Parameters(index = "2", paramLabel = "ProblemLink", description = "Link to the problem, optional quotes")
    private String problemLink;

    @CommandLine.Parameters(index = "3", paramLabel = "ProblemPool", description = "Pick from H M and L, where H is hard and needs to be practiced more and L is...low ig? it should've been E, but I'm lazy")
    private String problemPool;

    @CommandLine.Option(names = {"-u", "--update"}, description = "To be used while updating an existing problem", required = false)
    private boolean update;

    @CommandLine.Option(names = "--debug", description = "Prints cache and db state", required = false)
    private boolean debug;

    @Override
    public void run() {
        //Todo: split into validation and logic functions, use try-catch-finally for debug log printing
        if (problemId <= 0 || problemName == null || problemName.isEmpty() || problemLink == null || problemLink.isEmpty() || problemPool == null || problemPool.isEmpty()) {
            System.err.println("Invalid parameters, all parameters are required to proceed");
            return;
        }
        if (!problemLink.startsWith("https://")) {
            System.err.println("use https:// for links, to prevent accidental passage of wrong order parameters");
            return;
        }
        Problem.Pool poolEnum = null;
        try {
            poolEnum = Problem.Pool.valueOf(problemPool.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid pool value, pick between H, M and L");
            return;
        }
        Problem problem = new Problem(problemId, problemName, problemLink, poolEnum, LocalDate.now(), 1);
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
        if(debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);
    }
}
