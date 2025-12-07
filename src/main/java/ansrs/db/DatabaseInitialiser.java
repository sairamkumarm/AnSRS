package ansrs.db;

import ansrs.util.Log;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseInitialiser {

    public static Connection initEmbeddedDb(Path dbPath){
        try {
            if (!Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }
            String url = "jdbc:h2:file:" + dbPath.toAbsolutePath() + ";MODE=PostgreSQL";
            Connection connection = DriverManager.getConnection(url, "sa", "");
            runMigrations(url);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(Log.errorMsg("Failed to connect to database"), e);
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg("Failed to load database"), e);
        }
    }

    public static Connection initInMemoryDb(String name){
        try {
            String url = "jdbc:h2:mem:" + name + ";MODE=PostgreSQL";
            Connection connection = DriverManager.getConnection(url, "sa", "");
            runMigrations(url);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(Log.errorMsg("Failed to connect to database"), e);
        }
    }

    private static void runMigrations(String url){
        Logger.getLogger("org.flywaydb").setLevel(Level.SEVERE);
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").load();
        flyway.migrate();
    }
}
