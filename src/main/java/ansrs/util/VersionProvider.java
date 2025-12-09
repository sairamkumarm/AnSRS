package ansrs.util;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
        String version = ansrs.ansrs.class.getPackage().getImplementationVersion();
        return new String[]{"Ansrs version "+ (version!=null?version:"not found")};
    }
    public static String getVersionString(){
        String version = ansrs.ansrs.class.getPackage().getImplementationVersion();
        return (version!=null?version:"not found");
    }
}
