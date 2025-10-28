package dev.sai.srs.db;

import dev.sai.srs.data.Problem;

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
                CREATE TABLE IF NOT EXISTS problems(
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

    public boolean insertProblem(Problem problem) {
        try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO problems
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
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


    public boolean updateProblem(Problem problem) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                            INSERT OR REPLACE INTO problems
                            (id, name, link, pool, last_recall, total_recalls)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """
        )) {
            ps.setInt(1, problem.getProblemId());
            ps.setString(2, problem.getProblemName());
            ps.setString(3, problem.getProblemLink());
            ps.setString(4, problem.getProblemPool().name());
            ps.setString(5, problem.getLastRecall().toString());
            ps.setInt(6, problem.getTotalRecalls());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateProblemsBatch(List<Problem> problems) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        UPDATE problems
                        SET name=?, link=?, pool=?, last_recall=?, total_recalls=?
                        WHERE id=?
                        """)
        ) {
            connection.setAutoCommit(false);
            for (Problem problem: problems){
                ps.setString(1, problem.getProblemName());
                ps.setString(2, problem.getProblemLink());
                ps.setString(3,problem.getProblemPool().name());
                ps.setString(4,problem.getLastRecall().toString());
                ps.setInt(5,problem.getTotalRecalls());
                ps.setInt(6,problem.getProblemId());
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


    public Optional<Problem> getProblemById(int pid) {
        try (
                ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM problems WHERE id=" + pid);
        ) {
            if (rs.next()) {
                return Optional.of(new Problem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Problem.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                ));
            } else return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<List<Problem>> getAllProblems() {
        List<Problem> problems = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM problems");
             ResultSet rs = stmt.executeQuery()) {
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
            System.err.println("Failed to fetch problems from DuckDB\n" + e);
            return Optional.empty();
        }
        return Optional.of(problems);
    }

    public Optional<List<Problem>> getProblemsFromList(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Optional.empty();
        String inClause = ids.stream().map(k -> String.valueOf(k)).collect(Collectors.joining(",", "(", ")"));
        List<Problem> res = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM problems WHERE id IN " + inClause);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Problem p = new Problem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("link"),
                        Problem.Pool.valueOf(rs.getString("pool")),
                        rs.getDate("last_recall").toLocalDate(),
                        rs.getInt("total_recalls")
                );
                res.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch problems from DuckDB\n" + e);
            return Optional.empty();
        }
        return Optional.of(res);
    }

    public boolean deleteProblemById(int problemId) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM problems WHERE id=?");) {
            statement.setInt(1, problemId);
            int rows = statement.executeUpdate();
            return rows == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean contains(int problemId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as count FROM problems WHERE id=?")) {
            statement.setInt(1, problemId);
            ResultSet rs = statement.executeQuery();
            rs.next();
            if (rs.getInt(1) == 1) return true;
            else return false;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean clearDatabase(){
        try(PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE problems")) {
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
