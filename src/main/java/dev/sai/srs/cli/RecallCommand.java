package dev.sai.srs.cli;

import dev.sai.srs.data.Problem;
import dev.sai.srs.printer.Printer;
import dev.sai.srs.service.RecallService;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@CommandLine.Command(name = "recall",
        description = "Loads problems from database into cache for recall",
        mixinStandardHelpOptions = true)
public class RecallCommand implements Runnable {

    @CommandLine.ParentCommand
    private SRSCommand parent;

    @CommandLine.Parameters(index = "0", paramLabel = "RECALL_COUNT", description = "The amount of problems to load into cache for recall")
    private int recallCount;

    @CommandLine.Option(names = {"-f", "--force"}, description = "use --force, to overwrite existing non empty session cache")
    private boolean force;

    @CommandLine.Option(names = {"-a", "--append"}, description = "use --append, to append to an existing non empty session cache, only unique problems are added")
    private boolean append;


    @Override
    public void run() {
        try {
            if (recallCount <= 0) throw new RuntimeException("Recall count cannot be non-positive");
            RecallService recallService = new RecallService(parent.db);
            Set<Integer> cacheProblems = parent.sessionCache.getProblemIdSet();
            if (!cacheProblems.isEmpty()) {
                if (!force)
                    throw new RuntimeException("Session cache non-empty, use --force and/or --append to bypass");
                if (!append) {
                    cacheProblems.clear();
                    System.out.println("Overwriting existing cache");
                } else {
                    System.out.println("Appending problems to non-empty session");
                }
            }
            cacheProblems.addAll(recallService.recall(recallCount));
            parent.sessionCache.fillCache(cacheProblems);
            System.out.println(cacheProblems.size() + " problems in cache");
            Optional<List<Problem>> listOptional = parent.db.getProblemsFromList(cacheProblems.stream().toList());
            if (listOptional.isPresent()) Printer.printProblemsGrid(listOptional.get());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
