package ansrs.util;

public class Log {
    public static void info(String msg) { System.out.println("\u001B[36mINFO:\u001B[0m " + msg); }
    public static void warn(String msg) { System.out.println("\u001B[33mWARNING:\u001B[0m " + msg); }
    public static void error(String msg) { System.err.println("\u001B[31mERROR:\u001B[0m " + msg); }
    public static String errorMsg(String msg) { return "\u001B[31mERROR:\u001B[0m " + msg; }
    public static String warnMsg(String msg) { return "\u001B[33mERROR:\u001B[0m " + msg; }

}
