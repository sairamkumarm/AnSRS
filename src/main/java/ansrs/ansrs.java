package ansrs;

import ansrs.set.WorkingSet;
import ansrs.set.CompletedSet;
import ansrs.cli.SRSCommand;
import ansrs.db.DBManager;
import ansrs.util.Log;
import ansrs.util.Printer;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

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
            if (args.length == 0) {
                String banner = """
                                                                          \s
                         $$$$$$\\             $$$$$$\\  $$$$$$$\\   $$$$$$\\  \s
                        $$  __$$\\           $$  __$$\\ $$  __$$\\ $$  __$$\\ \s
                        $$ |  $$ |$$$$$$$\\  $$ |  \\__|$$ |  $$ |$$ |  \\__|\s
                        $$$$$$$$ |$$  __$$\\ \\$$$$$$\\  $$$$$$$  |\\$$$$$$\\  \s
                        $$  __$$ |$$ |  $$ | \\____$$\\ $$  __$$<  \\____$$\\ \s
                        $$ |  $$ |$$ |  $$ |$$\\   $$ |$$ |  $$ |$$\\   $$ |\s
                        $$ |  $$ |$$ |  $$ |\\$$$$$$  |$$ |  $$ |\\$$$$$$  |\s
                        \\__|  \\__|\\__|  \\__| \\______/ \\__|  \\__| \\______/ \s
                                                                          \s
                                 ANOTHER SPACED REPETITION SYSTEM         \s
                                       AnSRS Version 1.0.0                \s
                                                                          \s
                       """;

                String coloredBanner = colorizeBanner(banner);
                System.out.println(coloredBanner);
                if (workingSet.getItemIdSet().isEmpty()) {
                    System.out.println("""
                            Author: Sairamkumar M
                            Email: sairamkumar.m@outlook.com
                            Github: https://github.com/sairamkumarm/AnSRS
                            
                            Usage: ansrs [-dhV] [COMMAND]
                            
                            AnSRS (Pronounced "Answers") is a spaced repetition system.
                            There are 3 Store of data here.
                            A WorkingSet, where Items set for recall during a session are stored.
                            A CompletedSet, where items recalled, are stored, waiting to be commited.
                            A Database, where items are persisted for further recollection.
                            
                              -l, --list      Prints set and db state
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
                } else {
                    System.out.println("Current WorkingSet:");
                    Printer.printItemsGrid(db.getItemsFromList(workingSet.getItemIdList()).orElse(new ArrayList<>()));
                }
            }
            SRSCommand root = new SRSCommand(workingSet, completedSet, db);
            int exitCode = new CommandLine(root).execute(args);
            db.close();
            System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg("Error initializing environment"), e);
        } catch (Exception e) {
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
    }

    public static String colorizeBanner(String banner) {
        String foreground = "\u001B[97m";
        String background = "\u001B[107m";
        String reset = "\u001B[0m";
        String accent = "\u001B[92m";
        String blackBg = "\u001B[40m";
        banner = banner.replace("$", foreground + background + "$" + reset)
                .replace("\\", accent + blackBg + "\\" + reset)
                .replace("/", accent + blackBg + "/" + reset)
                .replace("_", accent + blackBg + "_" + reset)
                .replace("|", accent + blackBg + "|" + reset)
                .replace("<", accent + blackBg + "<" + reset)
                .replace(" ", accent + blackBg + " ")
                .replace("\n", reset + "\n" + accent + blackBg)
                .replace("=", accent + blackBg + "=");

        banner = banner+reset;
        return banner;
    }
}
