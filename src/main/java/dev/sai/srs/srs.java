package dev.sai.srs;

import dev.sai.srs.set.WorkingSet;
import dev.sai.srs.set.CompletedSet;
import dev.sai.srs.cli.SRSCommand;
import dev.sai.srs.db.DuckDBManager;
import picocli.CommandLine;
import picocli.CommandLine.Help.*;
import picocli.CommandLine.Help.Ansi.*;
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
        databasePath = srsDir.resolve("srs.duckdb");


    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(srsDir);

            WorkingSet workingSet = new WorkingSet(workingSetPath);
            CompletedSet completedSet = new CompletedSet(completedSetPath);
            DuckDBManager db = new DuckDBManager(databasePath);
            ColorScheme colorScheme = new ColorScheme.Builder()
                    .commands    (Style.bold, Style.underline)    // combine multiple styles
                    .options     (Style.fg_yellow)                // yellow foreground color
                    .parameters  (Style.fg_yellow)
                    .optionParams(Style.italic)
                    .errors      (Style.fg_red, Style.bold)
                    .stackTraces (Style.italic)
                    .build();
            SRSCommand root = new SRSCommand(workingSet, completedSet, db);
            int exitCode = new CommandLine(root).setColorScheme(colorScheme).execute(args);
            db.close();
            System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException( "Error initializing environment",e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
