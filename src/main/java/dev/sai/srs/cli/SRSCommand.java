package dev.sai.srs.cli;

import dev.sai.srs.cache.SessionCache;
import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.data.Problem;
import dev.sai.srs.db.DuckDBManager;
import dev.sai.srs.printer.Printer;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "srs",
        description = "Spaced Repetition System recall practice.",
        mixinStandardHelpOptions = true,
        subcommands = {CompleteCommand.class, AddCommand.class, DeleteCommand.class, CommitCommand.class, RecallCommand.class})
public class SRSCommand implements Runnable{
    public final SessionCache sessionCache;
    public final UpdateCache updateCache;
    public final DuckDBManager db;

    public SRSCommand(SessionCache sessionCache, UpdateCache updateCache, DuckDBManager duckDBManager) {
        this.sessionCache = sessionCache;
        this.updateCache = updateCache;
        this.db = duckDBManager;
    }

    @CommandLine.Option(names = {"-d", "--debug"}, description = "Prints cache and db state", required = false)
    private boolean debug;

    @Override
    public void run() {
        Optional<List<Problem>> op = db.getProblemsFromList(sessionCache.getProblemIdList());
        if (op.isPresent()){
            Printer.printProblemsGrid(op.get());
        } else{
            System.out.println("No problems found in cache");
        }
        //TODO: problems gen(with/without overwrite),
        // DONE: problem complete (with/without updating poll),
        // DONE: problem add to db, to introduce new problems
        // DONE: queue discard with flag,
        // DONE: commit (with/without flag) also delete session
        // TODO: add feature to complete whole session and clear db too
        //  DONE: think about completion dates vs updateCache date, cause sometimes updatecache is older than actual problems and could result in lastrecall going back in time
        if(debug) Printer.statePrinter(sessionCache,updateCache,db);
    }
}
