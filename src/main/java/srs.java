import cache.Cache;
import cache.SessionCache;
import cache.UpdateCache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class srs {
    public static void main(String[] args) {
        String baseDir = System.getenv("APPDATA");
        Path srsDir = Path.of(baseDir, "SRS");
        try{
            Files.createDirectories(srsDir);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        Path sessionCachePath = srsDir.resolve("session.cache");
        Path updateCachePath = srsDir.resolve("update.cache");
//        LocalDate date = LocalDate.now();
//        List<Integer> ls = List.of(101,102,103,104,105);
        SessionCache sessionCache = new SessionCache(sessionCachePath);
        System.out.println(sessionCache);
        Cache updateCache = new UpdateCache(updateCachePath);
        System.out.println(updateCache);
//        sessionCache.problemIds = new ArrayList<>(List.of(12,23,34,45));
//        System.out.println(sessionCache);
//        sessionCache.removeProblem((Integer) 12);
//        System.out.println(sessionCache);
    }
}
