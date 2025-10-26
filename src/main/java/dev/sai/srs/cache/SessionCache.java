package dev.sai.srs.cache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SessionCache implements Cache {
    private final Path cachePath;
    private LocalDate cacheDate;
    public Set<Integer> problemIds;

    public SessionCache(Path path) {
        this.cachePath = path;
        this.problemIds = new HashSet<>();
        if (!Files.exists(path)) {
            initCache(cachePath, LocalDate.now());
        } else {
            loadCache(cachePath);
        }
    }

    private void initCache(Path cachePath, LocalDate date) {
        try (BufferedWriter writer = Files.newBufferedWriter(cachePath)) {
            writer.write(date.toString());
            cacheDate = date;
            writer.newLine();
            writer.write(String.valueOf(0));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Error Creating Cache");
        }
    }

    private void loadCache(Path cachePath) {
        try {
            List<String> lines = Files.readAllLines(cachePath);
            if (lines.size() < 2) throw new RuntimeException("Cache File Contents Malformed");
            cacheDate = LocalDate.parse(lines.getFirst());
            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) problemIds.add(Integer.parseInt(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading cache");
        }
    }

    public void fillCache(List<Integer> problems){
        problemIds = new HashSet<>(problems);
    }

    public boolean removeProblem(Integer problemId) {
        if (problemIds.contains(problemId)) {
            boolean stat = problemIds.remove(problemId);
            reloadCache();
            return stat;
        }
        return false;
    }

    public void reloadCache() {
        try (BufferedWriter writer = Files.newBufferedWriter(cachePath)) {
            writer.write(cacheDate.toString());
            writer.newLine();
            writer.write(String.valueOf(problemIds.size()));
            writer.newLine();
            for (int i : problemIds) {
                writer.write(String.valueOf(i));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error Updating Cache");
        }
    }

    public Path getCachePath() {
        return cachePath;
    }

    public LocalDate getCacheDate() {
        return cacheDate;
    }

    public List<Integer> getProblemIdList() {
        return problemIds.stream().toList();
    }

    public Set<Integer> getProblemIdSet() {return problemIds;}

    public void setCacheDate(LocalDate cacheDate) {
        this.cacheDate = cacheDate;
        reloadCache();
    }

    @Override
    public String toString() {
        return "SessionCache{" +
                "cacheDate=" + cacheDate +
                ", problemIds=" + problemIds +
                '}';
    }
}
