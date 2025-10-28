package dev.sai.srs.cache;

import dev.sai.srs.data.Problem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UpdateCache implements Cache {
    private final Path cachePath;
    private LocalDate cacheDate;
    private HashMap<Integer, Pair<Problem.Pool, LocalDate>> problems;

    public UpdateCache(Path path) {
        this.cachePath = path;
        this.problems = new HashMap<>();
        if (!Files.exists(path)) {
            initCache(cachePath, LocalDate.now());
        } else {
            loadCache(cachePath);
        }
    }

    private void initCache(Path cachePath, LocalDate date) {
        try (BufferedWriter writer = Files.newBufferedWriter(cachePath)) {
            writer.write(date.toString());
            writer.newLine();
            cacheDate = date;
            writer.write(String.valueOf(0));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error: UpdateCache Creation Failed");
        }
    }

    private void loadCache(Path cachePath) {
        try {
            List<String> lines = Files.readAllLines(cachePath);
            if (lines.size() < 2) throw new RuntimeException("UpdateCache Malformed: Too few lines");
            cacheDate = LocalDate.parse(lines.getFirst());
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    String[] problemHolder = line.split(" ");
                    if (problemHolder.length != 3) throw new RuntimeException("UpdateCache Problem Malformed: Not enough data");
                    try{
                       problems.put(Integer.parseInt(problemHolder[0]),
                               new Pair<Problem.Pool, LocalDate>(
                                       problemHolder[1].equals("null")?null:Problem.Pool.valueOf(problemHolder[1].toUpperCase()),
                                       LocalDate.parse(problemHolder[2])));
                    } catch (IllegalArgumentException e){
                        throw new RuntimeException("UpdateCache Problem Malformed: Pool Value Invalid");
                    } catch (DateTimeParseException e){
                        throw new RuntimeException("UpdateCache Problem Malformed: Date unparsable");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading cache");
        }
    }

    public void reloadCache() {
        try (BufferedWriter writer = Files.newBufferedWriter(cachePath)) {
            writer.write(cacheDate.toString());
            writer.newLine();
            writer.write(String.valueOf(problems.size()));
            writer.newLine();
            for (Map.Entry<Integer, Pair<Problem.Pool, LocalDate>> e : problems.entrySet()) {
                if (e.getValue()==null || e.getValue().getLast_recall()==null) throw new RuntimeException("UpdateCache Object Malformed");
                String pid = String.valueOf(e.getKey());
                String pool = (e.getValue().getPool()==null) ? "null" :e.getValue().getPool().name();
                String date = e.getValue().getLast_recall().toString();
                writer.write(pid + " " + pool + " " + date);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addProblem(Integer problemId, Problem.Pool pool) {
        if (problems.containsKey(problemId)) {
            return false;
        } else {
            problems.put(problemId, new Pair<>(pool, LocalDate.now()));
            reloadCache();
            return true;
        }
    }


    public boolean removeProblem(Integer problemID) {
        if (!problems.containsKey(problemID)) return false;
        problems.remove(problemID);
        reloadCache();
        return true;
    }

    public Path getCachePath() {
        return cachePath;
    }

    public LocalDate getCacheDate() {
        return cacheDate;
    }

    public HashMap<Integer, Pair<Problem.Pool, LocalDate>> getProblems() {
        return problems;
    }

    public void setCacheDate(LocalDate cacheDate) {
        this.cacheDate = cacheDate;
        reloadCache();
    }

    @Override
    public String toString() {
        return "SessionCache{" +
                "cacheDate=" + cacheDate +
                ", problemIds=" + problems +
                '}';
    }

    public static class Pair<T,V>{
        private final T pool;
        private final V last_recall;

        public Pair(T pool, V last_recall) {
            this.pool = pool;
            this.last_recall = last_recall;
        }

        public T getPool() {
            return pool;
        }

        public V getLast_recall() {
            return last_recall;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "pool=" + pool +
                    ", last_recall=" + last_recall +
                    '}';
        }
    }
}
