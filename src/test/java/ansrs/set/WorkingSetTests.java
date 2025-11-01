package ansrs.set;


import ansrs.util.Log;
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

public class WorkingSetTests {
    private static final String baseDir = System.getenv("APPDATA");
    private static final Path srsDir = Path.of(baseDir, "SRS-TEST");
    private static WorkingSet testWorkingSet;
    private static Path workingSetTestPath;

    @BeforeEach
     void initTests(){
        try {
//            System.out.println(srsDir.toString());
            Files.createDirectories(srsDir);
        } catch (IOException e){
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
        workingSetTestPath = srsDir.resolve("test_working.set");
        testWorkingSet = new WorkingSet(workingSetTestPath);
    }

    @AfterEach
    void closeTests(){
        try{
            Files.deleteIfExists(workingSetTestPath);
            if (Files.exists(srsDir) && Files.list(srsDir).findAny().isEmpty()) {
                Files.delete(srsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(Log.errorMsg(e.getMessage()));
        }
    }

    @Test
    void workingSetInitTest(){
        //check date validity
        Assertions.assertNotNull(testWorkingSet.getSetDate());
        Assertions.assertEquals(LocalDate.now(), testWorkingSet.getSetDate());
        //check zero items
        Assertions.assertEquals(0, testWorkingSet.getItemIdList().size());
        List<Integer> items = new ArrayList<>();
        try{
            List<String> lines = Files.readAllLines(workingSetTestPath);
            if(lines.size() <2) throw new RuntimeException(Log.errorMsg("Set File Contents Malformed"));
            Assertions.assertEquals(LocalDate.now(), LocalDate.parse(lines.getFirst()));
            for(int i=2; i< lines.size(); i++){
                String line = lines.get(i).trim();
                if(!line.isEmpty()) items.add(Integer.parseInt(line));
            }
        } catch (IOException e){
            throw new RuntimeException(Log.errorMsg("Error loading set"));
        }
        Assertions.assertEquals(0,items.size());
    }

    boolean isSetFileObjectEqual(){
        List<Integer> items = new ArrayList<>();
        try{
            List<String> lines = Files.readAllLines(workingSetTestPath);
            if(!testWorkingSet.getSetDate().equals(LocalDate.parse(lines.getFirst()))) return false;
            if(lines.size() <2) return false;
            for(int i=2; i< lines.size(); i++){
                String line = lines.get(i).trim();
                if(!line.isEmpty()) items.add(Integer.parseInt(line));
            }
            if(testWorkingSet.getItemIdList().size() != Integer.parseInt(lines.get(1).trim())) return false;
            if(!testWorkingSet.getItemIdList().equals(items)) return false;
        } catch (IOException e){
            throw new RuntimeException(Log.errorMsg("Error loading set"));
        }
        return true;
    }

    @Test
    void setReloadTest(){
        testWorkingSet.fillSet(List.of(12,23,34));
        testWorkingSet.reloadSet();
        Assertions.assertTrue(isSetFileObjectEqual());
    }

    @Test
    void setOperationsTest(){
        testWorkingSet.fillSet(List.of(12,23,34));
        testWorkingSet.reloadSet();
        Assertions.assertTrue(isSetFileObjectEqual());
        Assertions.assertTrue(testWorkingSet.removeItem((Integer) 12));
        Assertions.assertTrue(isSetFileObjectEqual());
        Assertions.assertFalse(testWorkingSet.removeItem((Integer) 50));
        Assertions.assertTrue(isSetFileObjectEqual());
    }


}
