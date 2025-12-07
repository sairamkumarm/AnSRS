package ansrs.util;

public class Banner {

    public static void colorrizedBanner(String version){
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
                        ANOTHER SPACED REPETITION SYSTEM                  \s
                        AnSRS Version %s                \s
                       """.formatted(version);

        String coloredBanner = colorizeBanner(banner);
        System.out.println(coloredBanner);
    }

    public static void initHelp(){
        System.out.println("""
                            Author: Sairamkumar M
                            Email: sairamkumar.m@outlook.com
                            Github: https://github.com/sairamkumarm/AnSRS
                            
                            AnSRS (Pronounced "Answers") is a spaced repetition system.
                            ansrs is a command-line spaced repetition system designed for quick item tracking and recall
                            scheduling. It uses a lightweight local database to manage three sets: working, completed,
                            and recall. The system supports a jump-start feature that allows new users to begin recall
                            sessions immediately using predefined items, bypassing the usual initialization delay.
                            
                            There are 3 Store of data here.
                            A WorkingSet, where Items set for recall during a session are stored.
                            A CompletedSet, where items recalled, are stored, waiting to be commited.
                            A Database, where items are persisted in normal storage and archived for further recollection.
                            
                            Usage: ansrs [-hlsV] [-i=ITEM_ID] [-n=ITEM_NAME_QUERY] [COMMAND]

                              -h, --help         Show this help message and exit.
                              -i, --id=ITEM_ID   Print a specific Item
                              -l, --list         Lists set and db state
                              -n, --name=ITEM_NAME_QUERY
                                                 Find an Item by it's name, query must be longer than one
                                                   character
                              -s, --set          Use this flag with --list to print only set
                              -V, --version      Print version information and exit.
                            Commands:
                              add       Add new items into the item database or update an existing one
                              complete  Marks item as completed, and transfers them to the CompletedSet
                              delete    Remove from WorkingSet, CompletedSet and db, depending on the
                                          flags, by default it removes from WorkingSet
                              commit    Save completed items in WorkingSet to the database
                              recall    Loads items from database into WorkingSet for recall
                              rollback  Rolls back items from completed state to WorkingSet state
                              import    Import a csv into the database.
                              archive   Manage archive operations
                            
                            """);
    }


    private static String colorizeBanner(String banner) {
        String fg = "\u001B[97m";     // bright white
        String wbg = "\u001B[107m";     // bright white background
        String bg = "\u001B[40m";     // black background
        String accent = "\u001B[92m"; // green
        String reset = "\u001B[0m";

        String[] lines = banner.split("\n", -1);

        int maxWidth = 0;
        for (String line : lines) {
            int visibleLength = stripAnsi(line).length();
            if (visibleLength > maxWidth) {
                maxWidth = visibleLength;
            }
        }

        StringBuilder out = new StringBuilder();

        for (String line : lines) {
            String clean = stripAnsi(line);
            int pad = maxWidth - clean.length();
            String paddedLine = line + " ".repeat(Math.max(0, pad));

            paddedLine = paddedLine
                    .replace("$", fg + wbg + "$" + reset + bg)
                    .replace("\\", accent + bg + "\\" + reset + bg)
                    .replace("/", accent + bg + "/" + reset + bg)
                    .replace("_", accent + bg + "_" + reset + bg)
                    .replace("|", accent + bg + "|" + reset + bg)
                    .replace("<", accent + bg + "<" + reset + bg)
                    .replace("=", accent + bg + "=" + reset + bg);

            out.append(bg).append(paddedLine).append(reset).append("\n");
        }

        return out.toString();
    }

    private static String stripAnsi(String s) {
        return s.replaceAll("\\u001B\\[[;\\d]*m", "");
    }
}
