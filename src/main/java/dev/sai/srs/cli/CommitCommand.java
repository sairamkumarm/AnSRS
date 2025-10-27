package dev.sai.srs.cli;

import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.data.Problem;
import picocli.CommandLine;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "commit", description = "commit - lets you commit completed problems in session to the database")
public class CommitCommand implements Runnable{

    @CommandLine.ParentCommand
    private SRSCommand parent;

    @CommandLine.Option(names = {"-f","--force"}, description = "allows you to commit when there are pending problems in the session")
    private boolean force;

    @Override
    public void run() {
        HashMap<Integer, UpdateCache.Pair<Problem.Pool, LocalDate>> updatedCacheProblems = parent.updateCache.getProblems();
        if(updatedCacheProblems.isEmpty()){
            System.err.println("Nothing to commit");
            return;
        }
        Optional<List<Problem>> dbProblemsOptional = parent.db.getProblemsFromList(updatedCacheProblems.keySet().stream().toList());
        assert dbProblemsOptional.isPresent();
        List<Problem> dbProblems = dbProblemsOptional.get();
        if (!parent.sessionCache.getProblemIdSet().isEmpty() && !force){
            System.err.println("Problems found in session, use --force to override and commit");
            return;
        }
        System.out.println(dbProblems);
        for (Problem problem: dbProblems){
            UpdateCache.Pair<Problem.Pool, LocalDate> poolLocalDatePair = updatedCacheProblems.get(problem.getProblemId());
            Problem.Pool pool = poolLocalDatePair.getFirst();
            if (pool!=null){
                problem.setProblemPool(pool);
            }
            problem.setLastRecall(poolLocalDatePair.getSecond());
            problem.setTotalRecalls(problem.getTotalRecalls()+1);
            parent.updateCache.removeProblem(problem.getProblemId());
        }
        System.out.println(dbProblems);
        if (!parent.db.updateProblemsBatch(dbProblems)){
            System.err.println("Commit Failed");
            return;
        }
        System.out.println("Commit success: "+dbProblems.size()+" problems updated");
    }
}
