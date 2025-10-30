package dev.sai.srs.set;

import dev.sai.srs.data.Item;
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

public class CompletedSetTests {

    private static final String baseDir = System.getenv("APPDATA");
    private static final Path srsDir = Path.of(baseDir, "SRS-COMPLETE-TEST");
    private static CompletedSet testCompletedSet;
    private static Path completedSetTestPath;

    @BeforeEach
    void initTests() {
        try {
            Files.createDirectories(srsDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        completedSetTestPath = srsDir.resolve("test_completed.set");
        testCompletedSet = new CompletedSet(completedSetTestPath);
    }

    @AfterEach
    void closeTests() {
        try {
            Files.deleteIfExists(completedSetTestPath);
            if (Files.exists(srsDir) && Files.list(srsDir).findAny().isEmpty()) {
                Files.delete(srsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void completedSetInitTest() {
        // date validity
        Assertions.assertNotNull(testCompletedSet.getSetDate());
        Assertions.assertEquals(LocalDate.now(), testCompletedSet.getSetDate());

        // empty item map
        Assertions.assertEquals(0, testCompletedSet.getItems().size());

        // check file structure
        try {
            List<String> lines = Files.readAllLines(completedSetTestPath);
            Assertions.assertTrue(lines.size() >= 2, "Set file should have at least 2 lines");
            Assertions.assertEquals(LocalDate.now(), LocalDate.parse(lines.getFirst()));
            Assertions.assertEquals("0", lines.get(1).trim());
        } catch (IOException e) {
            throw new RuntimeException("Error reading set file");
        }
    }

    private boolean isSetFileObjectEqual() {
        Map<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> fileItems = new HashMap<>();
        HashMap<Integer, CompletedSet.Pair<Item.Pool, LocalDate>> setItems = testCompletedSet.getItems();
        try {
            List<String> lines = Files.readAllLines(completedSetTestPath);
            if (lines.size() < 2) return false;
            if (!testCompletedSet.getSetDate().equals(LocalDate.parse(lines.getFirst()))) return false;

            for (int i = 2; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    String[] temp = line.split(" ");
                    if (temp.length != 3) return false;
                    int pid = Integer.parseInt(temp[0]);
                    Item.Pool pool = temp[1].equals("null") ? null : Item.Pool.valueOf(temp[1].toUpperCase());
                    LocalDate date = LocalDate.parse(temp[2]);
                    fileItems.put(pid, new CompletedSet.Pair<>(pool, date));
                    if (!fileItems.get(pid).equals(setItems.get(pid)));
                }
            }
            System.out.println(setItems);
            System.out.println(fileItems);
            if(testCompletedSet.getItems().size() != Integer.parseInt(lines.get(1).trim())) return false;
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Error validating set file");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Pool malformed");
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Date malformed");
        }
    }

    @Test
    void setReloadTest() {
//        HashMap<Integer, String> dummyItems = new HashMap<>();
//        dummyItems.put(10, "A");
//        dummyItems.put(20, "B");
//        dummyItems.put(30, "C");

        Assertions.assertTrue(testCompletedSet.addItem(10, Item.Pool.L));
        testCompletedSet.reloadItem();
        Assertions.assertTrue(isSetFileObjectEqual());
    }

    @Test
    void setAddItemTest() {
        // add first item
        Assertions.assertTrue(testCompletedSet.addItem(100, Item.Pool.L));
        Assertions.assertTrue(isSetFileObjectEqual());

        // duplicate add should fail
        Assertions.assertFalse(testCompletedSet.addItem(100, Item.Pool.L));
        Assertions.assertTrue(isSetFileObjectEqual());

        // add more entries
        Assertions.assertTrue(testCompletedSet.addItem(200, null));
        Assertions.assertTrue(testCompletedSet.addItem(300, Item.Pool.H));
        Assertions.assertTrue(isSetFileObjectEqual());
    }

    @Test
    void setDateChangeTest() {
        LocalDate newDate = LocalDate.now().minusDays(5);
        testCompletedSet.setSetDate(newDate);
        Assertions.assertEquals(newDate, testCompletedSet.getSetDate());
        Assertions.assertTrue(isSetFileObjectEqual());
    }
}
