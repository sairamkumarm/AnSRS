package ansrs;

import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.cli.SRSCommand;
import ansrs.db.DBManager;
import ansrs.util.Log;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ansrs {

    public static final Path srsDir;
    public static final Path workingSetPath;
    public static final Path completedSetPath;
    public static final Path databasePath;

    static {
        String baseDir = System.getenv("APPDATA");
        srsDir = Path.of(baseDir, "AnSRS");
        workingSetPath = srsDir.resolve("working.set");
        completedSetPath = srsDir.resolve("completed.set");
        databasePath = srsDir.resolve("ansrs.db");
    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(srsDir);

            WorkingSet workingSet = new WorkingSet(workingSetPath);
            CompletedSet completedSet = new CompletedSet(completedSetPath);
            DBManager db = new DBManager(databasePath);
            if(args.length==0){
            System.out.println("""
                    \u001B[32m
                     ██████╗         ████████╗███████╗ ████████╗
                    ██╔═══██╗        ██╔═════╝██╔═══██╗██╔═════╝
                    ████████║██████╗ ████████╗███████╔╝████████╗
                    ██╔═══██║██╔══██╗╚═════██║██╔═══██╗╚═════██║
                    ██║   ██║██║  ██║████████║██║   ██║████████║
                    ╚═╝   ╚═╝╚═╝  ╚═╝╚═══════╝╚═╝   ╚═╝╚═══════╝
                    ══════ANOTHER SPACED REPETITION SYSTEM══════
                    \u001B[0m\s
                    AnSRS Version 1.0.0
                    Author: Sairamkumar M
                    Email: sairamkumar.m@outlook.com
                    Github: https://github.com/sairamkumarm/AnSRS
                    
                    Usage: ansrs [-dhV] [COMMAND]
                    
                    AnSRS (Pronounced "Answers") is a spaced repetition system.
                    There are 3 Store of data here.
                    A WorkingSet, where Items set for recall during a session are stored.
                    A CompletedSet, where items recalled, are stored, waiting to be commited.
                    A Database, where items are persisted for further recollection.
                    
                      -d, --debug     Prints set and db state
                      -h, --help      Show this help message and exit.
                      -V, --version   Print version information and exit.
                    Commands:
                      add       Add new items into the item database or update an existing one
                      complete  Marks item as completed, and transfers them to the CompletedSet
                      delete    Remove from WorkingSet, CompletedSet and db, depending on the
                                  flags, by default it removes from WorkingSet
                      commit    Save completed items in WorkingSet to the database
                      recall    Loads items from database into WorkingSet for recall
                      rollback  Rolls back items from completed state to WorkingSet state
                      import    Import a csv into the database.

                    """);
            }
            SRSCommand root = new SRSCommand(workingSet, completedSet, db);
            int exitCode = new CommandLine(root).execute(args);
            db.close();
            System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg( "Error initializing environment"),e);
        } catch (Exception e) {
            throw new RuntimeException(Log.errorMsg( e.getMessage()));
        }
    }
}
