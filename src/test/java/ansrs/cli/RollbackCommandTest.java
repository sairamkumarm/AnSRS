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
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RollbackCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private SRSCommand parent;
    private RollbackCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = spy(new WorkingSet(tempDir.resolve("working.set")));
        completedSet = spy(new CompletedSet(tempDir.resolve("completed.set")));
        db = mock(DBManager.class);

        parent = new SRSCommand(workingSet, completedSet, db);
        cmd = new RollbackCommand();
        cmdLine = new CommandLine(cmd);
        cmd.parent = parent;
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
    void testSingleItemRollbackSuccess() {
        completedSet.addItem(1, Item.Pool.M);
        assertEquals(0, cmdLine.execute("1"));
        assertTrue(workingSet.getItemIdSet().contains(1));
        assertFalse(completedSet.containsItem(1));
    }

    @Test
    void testSingleItemRollbackFailure() {
        completedSet.addItem(2, Item.Pool.L);
        doReturn(false).when(workingSet).addItem(2);
        assertEquals(1, cmdLine.execute("2"));
    }

    @Test
    void testAllRollbackSuccess() {
        completedSet.addItem(20, Item.Pool.L);
        completedSet.addItem(10, Item.Pool.H);
        assertEquals(0, cmdLine.execute("--all"));
        assertTrue(workingSet.getItemIdSet().containsAll(Set.of(10, 20)));
        assertTrue(completedSet.getItems().isEmpty());
    }

    @Test
    void testAllRollbackFailure() {
        completedSet.addItem(30, Item.Pool.M);
        doReturn(false).when(completedSet).clearSet();
        assertEquals(1, cmdLine.execute("--all"));
    }

    // --- VALIDATION FAILURES ---

    @Test
    void testNegativeItemId() {
        assertEquals(2, cmdLine.execute("-1"));
    }

    @Test
    void testItemIdZeroWithoutAllFlag() {
        assertEquals(2, cmdLine.execute("0"));
    }

    @Test
    void testAllFlagWithNonZeroItemId() {
        completedSet.addItem(11, Item.Pool.M);
        assertEquals(2, cmdLine.execute("11", "--all"));
    }

    @Test
    void testEmptyCompletedSet() {
        assertEquals(2, cmdLine.execute("--all"));
    }

    @Test
    void testNonExistentItemInCompletedSet() {
        completedSet.addItem(12, Item.Pool.H);
        assertEquals(2, cmdLine.execute("13"));
    }

    // --- EDGE CASES ---

    @Test
    void testPartialAllRollback() {
        CompletedSet badCompleted = new CompletedSet(completedSet.getSetPath()) {
            @Override
            public boolean clearSet() {
                return false; // simulate partial clear
            }
        };

        WorkingSet spyWorking = workingSet;
        SRSCommand customParent = new SRSCommand(spyWorking, badCompleted, db);
        RollbackCommand customCmd = new RollbackCommand();
        CommandLine cl = new CommandLine(customCmd);
        customCmd.parent = customParent;

        badCompleted.addItem(50, Item.Pool.M);
        badCompleted.addItem(60, Item.Pool.H);

        assertEquals(1, cl.execute("--all"));
    }
}
