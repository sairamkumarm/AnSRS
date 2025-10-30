package dev.sai.srs.db;

import dev.sai.srs.data.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DuckDBManager implements AutoCloseable {
    private final Connection connection;

    public DuckDBManager(Path dbPath) {
        try {
            if (!Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }
            String url = "jdbc:duckdb:" + dbPath.toAbsolutePath();
            connection = DriverManager.getConnection(url);
            initTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initTable() {
        try (PreparedStatement statement = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS items(
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    link TEXT,
                    pool TEXT,
                    last_recall TEXT,
                    total_recalls INTEGER,
                )
                """)) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean insertItem(Item item) {
        try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO items
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


    public boolean updateItem(Item item) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                            INSERT OR REPLACE INTO items
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
                        UPDATE items
                        SET name=?, link=?, pool=?, last_recall=?, total_recalls=?
                        WHERE id=?
                        """)
        ) {
            connection.setAutoCommit(false);
            for (Item item : items){
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
            try{ connection.rollback();} catch (SQLException ignore) {}
            return false;
        } finally {
            try { connection.setAutoCommit(true);} catch (SQLException ignore){}
        }
    }


    public Optional<Item> getItemById(int itemId) {
        try (
                ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM items WHERE id=" + itemId);
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
            throw new RuntimeException(e);
        }
    }

    public Optional<List<Item>> getAllItems() {
        List<Item> items = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM items");
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
            System.err.println("Failed to fetch items from DuckDB\n" + e);
            return Optional.empty();
        }
        return Optional.of(items);
    }

    public Optional<List<Item>> getItemsFromList(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Optional.empty();
        String inClause = ids.stream().map(k -> String.valueOf(k)).collect(Collectors.joining(",", "(", ")"));
        List<Item> res = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM items WHERE id IN " + inClause);
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
        } catch (SQLException e) {
            System.err.println("Failed to fetch items from DuckDB\n" + e);
            return Optional.empty();
        }
        return Optional.of(res);
    }

    public boolean deleteItemsById(int itemId) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE id=?");) {
            statement.setInt(1, itemId);
            int rows = statement.executeUpdate();
            return rows == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean contains(int itemId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as count FROM items WHERE id=?")) {
            statement.setInt(1, itemId);
            ResultSet rs = statement.executeQuery();
            rs.next();
            if (rs.getInt(1) == 1) return true;
            else return false;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean clearDatabase(){
        try(PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE items")) {
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
