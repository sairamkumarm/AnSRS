package dev.sai.srs.cache;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SessionCacheTests {
    private static final String baseDir = System.getenv("APPDATA");
    private static final Path srsDir = Path.of(baseDir, "SRS-TEST");
    private static SessionCache testSessionCache;
    private static Path sessionCacheTestPath;

    @BeforeEach
     void initTests(){
        try {
//            System.out.println(srsDir.toString());
            Files.createDirectories(srsDir);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        sessionCacheTestPath = srsDir.resolve("session_test.cache");
        testSessionCache = new SessionCache(sessionCacheTestPath);
    }

    @AfterEach
    void closeTests(){
        try{
            Files.deleteIfExists(sessionCacheTestPath);
            if (Files.exists(srsDir) && Files.list(srsDir).findAny().isEmpty()) {
                Files.delete(srsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void cacheInitTest(){
        //check date validity
        Assertions.assertNotNull(testSessionCache.getCacheDate());
        Assertions.assertEquals(LocalDate.now(), testSessionCache.getCacheDate());
        //check zero problems
        Assertions.assertEquals(0, testSessionCache.getProblemIdList().size());
        List<Integer> problems = new ArrayList<>();
        try{
            List<String> lines = Files.readAllLines(sessionCacheTestPath);
            if(lines.size() <2) throw new RuntimeException("Cache File Contents Malformed");
            Assertions.assertEquals(LocalDate.now(), LocalDate.parse(lines.getFirst()));
            for(int i=2; i< lines.size(); i++){
                String line = lines.get(i).trim();
                if(!line.isEmpty()) problems.add(Integer.parseInt(line));
            }
        } catch (IOException e){
            throw new RuntimeException("Error loading cache");
        }
        Assertions.assertEquals(0,problems.size());
    }

    boolean isCacheFileObjectEqual(){
        List<Integer> problems = new ArrayList<>();
        try{
            List<String> lines = Files.readAllLines(sessionCacheTestPath);
            if(!testSessionCache.getCacheDate().equals(LocalDate.parse(lines.getFirst()))) return false;
            if(lines.size() <2) return false;
            for(int i=2; i< lines.size(); i++){
                String line = lines.get(i).trim();
                if(!line.isEmpty()) problems.add(Integer.parseInt(line));
            }
            if(testSessionCache.getProblemIdList().size() != Integer.parseInt(lines.get(1).trim())) return false;
            if(!testSessionCache.getProblemIdList().equals(problems)) return false;
        } catch (IOException e){
            throw new RuntimeException("Error loading cache");
        }
        return true;
    }

    @Test
    void cacheReloadTest(){
        testSessionCache.fillCache(List.of(12,23,34));
        testSessionCache.reloadCache();
        Assertions.assertTrue(isCacheFileObjectEqual());
    }

    @Test
    void cacheOperationsTest(){
        testSessionCache.fillCache(List.of(12,23,34));
        testSessionCache.reloadCache();
        Assertions.assertTrue(isCacheFileObjectEqual());
        Assertions.assertTrue(testSessionCache.removeProblem((Integer) 12));
        Assertions.assertTrue(isCacheFileObjectEqual());
        Assertions.assertFalse(testSessionCache.removeProblem((Integer) 50));
        Assertions.assertTrue(isCacheFileObjectEqual());
    }


}
