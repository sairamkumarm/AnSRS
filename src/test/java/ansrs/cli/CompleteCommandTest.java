package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.DBManager;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CompleteCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private SRSCommand parent;
    private CompleteCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = new WorkingSet(tempDir.resolve("working.set"));
        completedSet = new CompletedSet(tempDir.resolve("completed.set"));
        db = mock(DBManager.class);

        parent = new SRSCommand(workingSet, completedSet, db);
        cmd = new CompleteCommand();
        cmdLine = new CommandLine(cmd);
        cmd.parent = parent;

        when(db.contains(anyInt())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {}
                });
    }

    // --- VALID CASES ---

    @Test
    void testSimpleCompletionFromWorkingSet() {
        workingSet.addItem(1);
        assertEquals(0, cmdLine.execute("1"));
        assertFalse(workingSet.getItemIdSet().contains(1));
        assertTrue(completedSet.containsItem(1));
    }

    @Test
    void testCompletionWithPoolUpdate() {
        workingSet.addItem(2);
        assertEquals(0, cmdLine.execute("2", "--update", "H"));
        assertFalse(workingSet.getItemIdSet().contains(2));
        assertEquals(Item.Pool.H, completedSet.getItems().get(2).getPool());
    }

    @Test
    void testCompletionWithCustomDate() {
        workingSet.addItem(3);
        String date = LocalDate.now().minusDays(3).toString();
        assertEquals(0, cmdLine.execute("3", "--date", date));
        assertEquals(LocalDate.parse(date), completedSet.getItems().get(3).getLast_recall());
    }

    @Test
    void testAllCompleteMovesEverything() {
        workingSet.fillSet(Set.of(10, 20, 30));
        assertEquals(0, cmdLine.execute("--all"));
        assertTrue(workingSet.getItemIdSet().isEmpty());
        assertTrue(completedSet.getItems().keySet().containsAll(Set.of(10, 20, 30)));
    }

    @Test
    void testForceCompletionWhenItemNotInWorkingSet() {
        when(db.contains(5)).thenReturn(true);
        Item mockItem = new Item(5, "Mock", "https://mock.com", Item.Pool.M, LocalDate.now(), 0);
        when(db.getItemById(5)).thenReturn(Optional.of(mockItem));

        assertEquals(0, cmdLine.execute("5", "--force"));
        assertTrue(completedSet.containsItem(5));
    }

    @Test
    void testForceCompletionWithCustomDate() {
        when(db.contains(6)).thenReturn(true);
        Item mockItem = new Item(6, "Mock", "https://mock.com", Item.Pool.L, LocalDate.now(), 0);
        when(db.getItemById(6)).thenReturn(Optional.of(mockItem));

        String date = LocalDate.now().minusDays(10).toString();
        assertEquals(0, cmdLine.execute("6", "--force", "--date", date));
        assertEquals(LocalDate.parse(date), completedSet.getItems().get(6).getLast_recall());
    }


    // --- VALIDATION FAILURES ---

    @Test
    void testNegativeItemId() {
        assertEquals(2, cmdLine.execute("-5"));
    }

    @Test
    void testItemIdZeroWithoutAllFlag() {
        assertEquals(2, cmdLine.execute("0"));
    }

    @Test
    void testAllFlagWithNonZeroItemId() {
        workingSet.addItem(11);
        assertEquals(2, cmdLine.execute("11", "--all"));
    }

    @Test
    void testNonExistentDatabaseItem() {
        when(db.contains(anyInt())).thenReturn(false);
        assertEquals(2, cmdLine.execute("12"));
    }

    @Test
    void testInvalidPoolValue() {
        workingSet.addItem(13);
        assertEquals(2, cmdLine.execute("13", "--update", "X"));
    }

    @Test
    void testInvalidDateFormat() {
        workingSet.addItem(14);
        assertEquals(2, cmdLine.execute("14", "--date", "12-12-2023"));
    }

    @Test
    void testForceFlagButItemNotInDatabase() {
        when(db.contains(15)).thenReturn(true);
        when(db.getItemById(15)).thenReturn(Optional.empty());
        assertEquals(2, cmdLine.execute("15", "--force"));
    }

    // --- EDGE & RUNTIME CASES ---

    @Test
    void testDuplicateCompletionIgnored() {
        workingSet.addItem(16);
        cmdLine.execute("16");
        int sizeBefore = completedSet.getItems().size();
        cmdLine.execute("16");
        assertEquals(sizeBefore, completedSet.getItems().size());
    }

    @Test
    void testAllCompleteWarnsOnPartialTransfer() {
        WorkingSet badWorkingSet = new WorkingSet(workingSet.getSetPath()) {
            @Override
            public boolean removeItem(Integer id) {
                // fail for one specific id to simulate partial transfer
                if (id == 60) return false;
                return super.removeItem(id);
            }
        };

        CompletedSet completedSpy = spy(completedSet);
        SRSCommand customParent = new SRSCommand(badWorkingSet, completedSpy, db);
        CompleteCommand customCmd = new CompleteCommand();
        CommandLine cl = new CommandLine(customCmd);
        customCmd.parent = customParent;

        badWorkingSet.fillSet(Set.of(50, 60));
        assertEquals(0, cl.execute("--all"));
        verify(completedSpy, atLeastOnce()).addItem(anyInt(), any());
    }
}
