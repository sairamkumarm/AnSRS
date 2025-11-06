package ansrs.db;

import ansrs.data.Item;
import ansrs.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ArchiveManager implements AutoCloseable{
    private final Connection connection;

    public ArchiveManager(Path path){
        try {
            if (!Files.exists(path.getParent())){
                Files.createDirectories(path.getParent());
            }
            String url="jdbc:h2:file:" + path.toAbsolutePath();
            connection = DriverManager.getConnection(url, "sa", "");
            initTable();
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg("Failed to load database"));
        } catch (SQLException e) {
            throw new RuntimeException(Log.errorMsg("Failed to connect to database"), e);
        }
    }

    //test purpose only
    public ArchiveManager(String mockTestDBNameAndPath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:" + mockTestDBNameAndPath, "ta", "");
        initTable();
    }

    public void initTable() {
        try (PreparedStatement statement = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS archive(
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(255),
                    link VARCHAR(255),
                    pool CHARACTER,
                    last_recall VARCHAR(255),
                    total_recalls INTEGER
                )
                """)) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(Log.errorMsg("Error initialising archive table"),e);
        }
    }

    public boolean insertItem(Item item) {
        try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO archive
                    (id, name, link, pool, last_recall, total_recalls)
                    VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, item.getItemId());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getItemLink());
            ps.setString(4, item.getItemPool().name());
            ps.setString(5, item.getLastRecall().toString());
            ps.setInt(6, item.getTotalRecalls());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean insertItemsBatch(List<Item> items) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO archive
                (id, name, link, pool, last_recall, total_recalls)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            connection.setAutoCommit(false);
            for (Item item : items) {
                statement.setInt(1, item.getItemId());
                statement.setString(2, item.getItemName());
                statement.setString(3, item.getItemLink());
                statement.setString(4, item.getItemPool().name());
                statement.setString(5, item.getLastRecall().toString());
                statement.setInt(6, item.getTotalRecalls());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignore) {
            }
            if (Objects.equals(e.getSQLState(), "23505"))
                throw new RuntimeException(Log.errorMsg("ERROR: Duplicate ITEM_ID found."));
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignore) {
            }
        }
    }

    public boolean upsertItemsBatch(List<Item> items) {
        try (PreparedStatement statement = connection.prepareStatement("""
                MERGE INTO archive
                (id, name, link, pool, last_recall, total_recalls)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            connection.setAutoCommit(false);
            for (Item item : items) {
                statement.setInt(1, item.getItemId());
                statement.setString(2, item.getItemName());
                statement.setString(3, item.getItemLink());
                statement.setString(4, item.getItemPool().name());
                statement.setString(5, item.getLastRecall().toString());
                statement.setInt(6, item.getTotalRecalls());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignore) {
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignore) {
            }
        }
    }

    public boolean updateItem(Item item) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                            MERGE INTO archive
                            (id, name, link, pool, last_recall, total_recalls)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """
        )) {
            ps.setInt(1, item.getItemId());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getItemLink());
            ps.setString(4, item.getItemPool().name());
            ps.setString(5, item.getLastRecall().toString());
            ps.setInt(6, item.getTotalRecalls());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateItemsBatch(List<Item> items) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        UPDATE archive
                        SET name=?, link=?, pool=?, last_recall=?, total_recalls=?
                        WHERE id=?
                        """)
        ) {
            connection.setAutoCommit(false);
            for (Item item : items) {
                ps.setString(1, item.getItemName());
                ps.setString(2, item.getItemLink());
                ps.setString(3, item.getItemPool().name());
                ps.setString(4, item.getLastRecall().toString());
                ps.setInt(5, item.getTotalRecalls());
                ps.setInt(6, item.getItemId());
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignore) {
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignore) {
            }
        }
    }


    public Optional<Item> getItemById(int itemId) {
        try (
                ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM archive WHERE id=" + itemId);
        ) {
            if (rs.next()) {
                return Optional.of(new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Item.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                ));
            } else return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
    }

    public Optional<List<Item>> getAllItems() {
        List<Item> items = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM archive");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Item p = new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Item.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                );
                items.add(p);
            }
        } catch (SQLException e) {
            Log.error("Failed to fetch items from DB\n" + e);
            return Optional.empty();
        }
        return Optional.of(items);
    }

    public Optional<HashSet<Integer>> getAllItemsIds() {
        HashSet<Integer> items = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id FROM archive");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            Log.error("Failed to fetch ITEM_IDs from DB\n" + e);
            return Optional.empty();
        }
        return Optional.of(items);
    }

    public Optional<List<Item>> getItemsFromList(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Optional.empty();
        String inClause = ids.stream().map(k -> String.valueOf(k)).collect(Collectors.joining(",", "(", ")"));
        List<Item> res = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM archive WHERE id IN " + inClause);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Item p = new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Item.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                );
                res.add(p);
            }
            return Optional.of(res);
        } catch (SQLException e) {
            Log.error("Failed to fetch items from Database\n" + e);
            return Optional.empty();
        }
    }

    public boolean deleteItemsById(int itemId) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM archive WHERE id=?");) {
            statement.setInt(1, itemId);
            int rows = statement.executeUpdate();
            return rows == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean contains(int itemId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as count FROM archive WHERE id=?")) {
            statement.setInt(1, itemId);
            ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1) == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public Optional<List<Item>> searchItemsByName(String query) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM archive WHERE LOWER(name) LIKE LOWER(?)")) {
            List<Item> items = new ArrayList<>();
            statement.setString(1,"%"+query+"%");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Item item = new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Item.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                );
                items.add(item);
            }
            return Optional.of(items);
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return Optional.empty();
        }
    }

    public boolean clearDatabase() {
        try (PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE archive")) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }



    @Override
    public void close() throws Exception {
        connection.close();
    }
}
