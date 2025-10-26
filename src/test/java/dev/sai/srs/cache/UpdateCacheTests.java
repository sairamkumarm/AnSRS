package dev.sai.srs.cache;

import dev.sai.srs.data.Problem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateCacheTests {

    private static final String baseDir = System.getenv("APPDATA");
    private static final Path srsDir = Path.of(baseDir, "SRS-UPDATE-TEST");
    private static UpdateCache testUpdateCache;
    private static Path updateCacheTestPath;

    @BeforeEach
    void initTests() {
        try {
            Files.createDirectories(srsDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateCacheTestPath = srsDir.resolve("update_test.cache");
        testUpdateCache = new UpdateCache(updateCacheTestPath);
    }

    @AfterEach
    void closeTests() {
        try {
            Files.deleteIfExists(updateCacheTestPath);
            if (Files.exists(srsDir) && Files.list(srsDir).findAny().isEmpty()) {
                Files.delete(srsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void cacheInitTest() {
        // date validity
        Assertions.assertNotNull(testUpdateCache.getCacheDate());
        Assertions.assertEquals(LocalDate.now(), testUpdateCache.getCacheDate());

        // empty problem map
        Assertions.assertEquals(0, testUpdateCache.getProblems().size());

        // check file structure
        try {
            List<String> lines = Files.readAllLines(updateCacheTestPath);
            Assertions.assertTrue(lines.size() >= 2, "Cache file should have at least 2 lines");
            Assertions.assertEquals(LocalDate.now(), LocalDate.parse(lines.getFirst()));
            Assertions.assertEquals("0", lines.get(1).trim());
        } catch (IOException e) {
            throw new RuntimeException("Error reading cache file");
        }
    }

    private boolean isCacheFileObjectEqual() {
        Map<Integer, UpdateCache.Pair<Problem.Pool, LocalDate>> fileProblems = new HashMap<>();
        HashMap<Integer, UpdateCache.Pair<Problem.Pool, LocalDate>> cacheProblems = testUpdateCache.getProblems();
        try {
            List<String> lines = Files.readAllLines(updateCacheTestPath);
            if (lines.size() < 2) return false;
            if (!testUpdateCache.getCacheDate().equals(LocalDate.parse(lines.getFirst()))) return false;

            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    String[] temp = line.split(" ");
                    if (temp.length != 3) return false;
                    int pid = Integer.parseInt(temp[0]);
                    Problem.Pool pool = temp[1].equals("null") ? null : Problem.Pool.valueOf(temp[1].toUpperCase());
                    LocalDate date = LocalDate.parse(temp[2]);
                    fileProblems.put(pid, new UpdateCache.Pair<>(pool, date));
                    if (!fileProblems.get(pid).equals(cacheProblems.get(pid)));
                }
            }
            System.out.println(cacheProblems);
            System.out.println(fileProblems);
            if(testUpdateCache.getProblems().size() != Integer.parseInt(lines.get(1).trim())) return false;
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Error validating cache file");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Pool malformed");
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Date malformed");
        }
    }

    @Test
    void cacheReloadTest() {
//        HashMap<Integer, String> dummyProblems = new HashMap<>();
//        dummyProblems.put(10, "A");
//        dummyProblems.put(20, "B");
//        dummyProblems.put(30, "C");

        Assertions.assertTrue(testUpdateCache.addProblem(10, Problem.Pool.L));
        testUpdateCache.reloadCache();
        Assertions.assertTrue(isCacheFileObjectEqual());
    }

    @Test
    void cacheAddProblemTest() {
        // add first problem
        Assertions.assertTrue(testUpdateCache.addProblem(100, Problem.Pool.L));
        Assertions.assertTrue(isCacheFileObjectEqual());

        // duplicate add should fail
        Assertions.assertFalse(testUpdateCache.addProblem(100, Problem.Pool.L));
        Assertions.assertTrue(isCacheFileObjectEqual());

        // add more entries
        Assertions.assertTrue(testUpdateCache.addProblem(200, null));
        Assertions.assertTrue(testUpdateCache.addProblem(300, Problem.Pool.H));
        Assertions.assertTrue(isCacheFileObjectEqual());
    }

    @Test
    void cacheDateChangeTest() {
        LocalDate newDate = LocalDate.now().minusDays(5);
        testUpdateCache.setCacheDate(newDate);
        Assertions.assertEquals(newDate, testUpdateCache.getCacheDate());
        Assertions.assertTrue(isCacheFileObjectEqual());
    }
}
