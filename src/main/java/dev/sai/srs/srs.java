package dev.sai.srs;

import dev.sai.srs.cache.SessionCache;
import dev.sai.srs.cache.UpdateCache;
import dev.sai.srs.cli.SRSCommand;
import dev.sai.srs.db.DuckDBManager;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class srs {

    public static final Path srsDir;
    public static final Path sessionCachePath;
    public static final Path updateCachePath;
    public static final Path databasePath;

    static {
        String baseDir = System.getenv("APPDATA");
        srsDir = Path.of(baseDir, "SRS");
        sessionCachePath = srsDir.resolve("session.cache");
        updateCachePath = srsDir.resolve("update.cache");
        databasePath = srsDir.resolve("srs.duckdb");


    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(srsDir);

            SessionCache sessionCache = new SessionCache(sessionCachePath);
            UpdateCache updateCache = new UpdateCache(updateCachePath);
            DuckDBManager db = new DuckDBManager(databasePath);

//            System.out.println(dev.sai.srs.db.getProblemsFromList(List.of(1,11)));
//            dev.sai.srs.db.insertProblem(new Problem(1,"234", "sdfgsdf", Problem.Pool.L, LocalDate.now(), 1));
//            sessionCache.fillCache(List.of(1,11,13));
            SRSCommand root = new SRSCommand(sessionCache, updateCache, db);
            int exitCode = new CommandLine(root).execute(args);
            //TODO: implement proper dev.sai.srs.exception handling, and graceful failing
            db.close();
            System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException( "Error initializing environment",e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
