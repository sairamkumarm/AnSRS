package db;

import data.Problem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DuckDBManager implements AutoCloseable{
    private final Connection connection;

    public DuckDBManager(Path dbPath) {
        try{
            if (!Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }
            String url = "jdbc:duckdb:"+dbPath.toAbsolutePath();
            connection = DriverManager.getConnection(url);
            initTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database",e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void initTable(){
        try(Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS problems(
                        id INTEGER PRIMARY KEY,
                        name TEXT,
                        link TEXT,
                        pool TEXT,
                        last_recall TEXT,
                        total_recalls INTEGER,
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsertProblem(Problem problem) {
        try (PreparedStatement ps = connection.prepareStatement("""
        INSERT OR REPLACE INTO problems
        (id, name, link, pool, last_recall, total_recalls)
        VALUES (?, ?, ?, ?, ?, ?)
    """)) {
            ps.setInt(1, problem.getProblemId());
            ps.setString(2, problem.getProblemName());
            ps.setString(3, problem.getProblemLink());
            ps.setString(4, problem.getProblemPool().name());
            ps.setString(5, problem.getLastRecall().toString());
            ps.setInt(6, problem.getTotalRecalls());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to upsert problem into DuckDB", e);
        }
    }

    public List<Problem> getAllProblems() {
        List<Problem> problems = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM problems")) {
            while (rs.next()) {
                Problem p = new Problem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Problem.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                );
                problems.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch problems from DuckDB", e);
        }
        return problems;
    }


    @Override
    public void close() throws Exception {
        connection.close();
    }
}
