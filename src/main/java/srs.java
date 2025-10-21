import cache.Cache;
import cache.SessionCache;
import cache.UpdateCache;
import db.DuckDBManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class srs {
    public static void main(String[] args) {
        String baseDir = System.getenv("APPDATA");
        Path srsDir = Path.of(baseDir, "SRS");
        try {
            Files.createDirectories(srsDir);
            Path sessionCachePath = srsDir.resolve("session.cache");
            Path updateCachePath = srsDir.resolve("update.cache");
            Path databasePath = srsDir.resolve("srs.duckdb");
            SessionCache sessionCache = new SessionCache(sessionCachePath);
            Cache updateCache = new UpdateCache(updateCachePath);
            DuckDBManager db = new DuckDBManager(databasePath);

            db.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
