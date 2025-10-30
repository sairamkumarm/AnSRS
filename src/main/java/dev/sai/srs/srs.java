package dev.sai.srs;

import dev.sai.srs.set.WorkingSet;
import dev.sai.srs.set.CompletedSet;
import dev.sai.srs.cli.SRSCommand;
import dev.sai.srs.db.DBManager;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class srs {

    public static final Path srsDir;
    public static final Path workingSetPath;
    public static final Path completedSetPath;
    public static final Path databasePath;

    static {
        String baseDir = System.getenv("APPDATA");
        srsDir = Path.of(baseDir, "SRS");
        workingSetPath = srsDir.resolve("working.set");
        completedSetPath = srsDir.resolve("completed.set");
        databasePath = srsDir.resolve("srs.db");

    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(srsDir);

            WorkingSet workingSet = new WorkingSet(workingSetPath);
            CompletedSet completedSet = new CompletedSet(completedSetPath);
            DBManager db = new DBManager(databasePath);
            SRSCommand root = new SRSCommand(workingSet, completedSet, db);
            int exitCode = new CommandLine(root).execute(args);
            db.close();
            System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException( "Error initializing environment",e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
