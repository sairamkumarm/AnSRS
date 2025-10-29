package dev.sai.srs;

import dev.sai.srs.cache.SessionCache;
import dev.sai.srs.cache.UpdateCache;
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
            ColorScheme colorScheme = new ColorScheme.Builder()
                    .commands    (Style.bold, Style.underline)    // combine multiple styles
                    .options     (Style.fg_yellow)                // yellow foreground color
                    .parameters  (Style.fg_yellow)
                    .optionParams(Style.italic)
                    .errors      (Style.fg_red, Style.bold)
                    .stackTraces (Style.italic)
                    .build();
            SRSCommand root = new SRSCommand(sessionCache, updateCache, db);
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
