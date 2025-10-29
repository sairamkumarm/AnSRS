package dev.sai.srs.cli;

import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Command(name = "commit",
        description = "commit - lets you commit completed problems in session to the database",
        mixinStandardHelpOptions = true)
public class CommitCommand implements Runnable{

    @Spec
    private Model.CommandSpec spec;

    @ParentCommand
    private SRSCommand parent;

    @Option(names = {"-f","--force"}, description = "allows you to commit when there are pending problems in the session")
    private boolean force;

    @Override
    public void run() {
        HashMap<Integer, UpdateCache.Pair<Problem.Pool, LocalDate>> updatedCacheProblems = parent.updateCache.getProblems();
        if(updatedCacheProblems.isEmpty()){
            throw new ParameterException(spec.commandLine(),"Nothing to commit");
        }
        List<Problem> dbProblems = parent.db.getProblemsFromList(
                updatedCacheProblems.keySet().stream().toList()).
                orElseThrow(
                        ()->new ParameterException(spec.commandLine(), "Session problems non-existent in DB")
                );
        if (!parent.sessionCache.getProblemIdSet().isEmpty() && !force){
            throw new ParameterException(spec.commandLine(),"Problems found in session, use --force to override and commit");
        }
        for (Problem problem: dbProblems){
            UpdateCache.Pair<Problem.Pool, LocalDate> poolLocalDatePair = updatedCacheProblems.get(problem.getProblemId());
            Problem.Pool pool = poolLocalDatePair.getPool();
            if (pool!=null){
                problem.setProblemPool(pool);
            }
            problem.setLastRecall(poolLocalDatePair.getLast_recall());
            problem.setTotalRecalls(problem.getTotalRecalls()+1);
            parent.updateCache.removeProblem(problem.getProblemId());
        }
        System.out.println(dbProblems);
        if (!parent.db.updateProblemsBatch(dbProblems)){
            System.err.println("Commit Failed, rolling back");
            //rollback from cache problems, not db, to preserve the null pools, that signify no change
            for (Map.Entry<Integer, UpdateCache.Pair<Problem.Pool, LocalDate>> problem: updatedCacheProblems.entrySet()) parent.updateCache.addProblem(problem.getKey(), problem.getValue().getPool(), problem.getValue().getLast_recall());
            return;
        }
        System.out.println("Commit success: "+dbProblems.size()+" problems updated");
        if (parent.debug) Printer.statePrinter(parent.sessionCache, parent.updateCache, parent.db);
    }
}
