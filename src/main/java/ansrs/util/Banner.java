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

    public static void initMessage(){
        System.out.println("""
                            Author: Sairamkumar M
                            Email: sairamkumar.m@outlook.com
                            Github: https://github.com/sairamkumarm/AnSRS
                            
                            AnSRS (Pronounced "Ans-er-es") is a spaced repetition system, designed for quick item
                            tracking and recall scheduling. It uses a lightweight local database and cache files to manage
                            items. The system supports batch importing items, and grouping for quick recall sessions.
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
