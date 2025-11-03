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
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommitCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private SRSCommand parent;
    private CommitCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = spy(new WorkingSet(tempDir.resolve("working.set")));
        completedSet = spy(new CompletedSet(tempDir.resolve("completed.set")));
        db = mock(DBManager.class);

        parent = new SRSCommand(workingSet, completedSet, db);
        cmd = new CommitCommand();
        cmdLine = new CommandLine(cmd);
        cmd.parent = parent;
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {}
                });
    }

    // --- VALID CASES ---

    @Test
    void testSuccessfulCommit() {
        Item item = new Item(1, "Mock", "https://mock.com", Item.Pool.L, LocalDate.now(), 0);
        completedSet.addItem(1, Item.Pool.H, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(item)));
        when(db.updateItemsBatch(anyList())).thenReturn(true);

        assertEquals(0, cmdLine.execute());
        verify(db, times(1)).updateItemsBatch(anyList());
        verify(completedSet, atLeastOnce()).removeItem(1);
    }

    @Test
    void testSuccessfulCommitWithListFlag() {
        parent.list = true;
        Item item = new Item(2, "Mock2", "https://mock2.com", Item.Pool.M, LocalDate.now(), 1);
        completedSet.addItem(2, Item.Pool.L, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(item)));
        when(db.updateItemsBatch(anyList())).thenReturn(true);

        assertEquals(0, cmdLine.execute());
        verify(db).updateItemsBatch(anyList());
        verify(completedSet).removeItem(2);
    }

    @Test
    void testCommitWithForceFlagAndNonEmptyWorkingSet() {
        workingSet.addItem(10);
        completedSet.addItem(20, Item.Pool.H, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(new Item(20, "Mock", "url", Item.Pool.M, LocalDate.now(), 0))));
        when(db.updateItemsBatch(anyList())).thenReturn(true);

        assertEquals(0, cmdLine.execute("--force"));
        verify(completedSet).removeItem(20);
    }

    // --- VALIDATION FAILURES ---

    @Test
    void testEmptyCompletedSet() {
        assertEquals(2, cmdLine.execute());
    }

    @Test
    void testWorkingSetNonEmptyWithoutForce() {
        workingSet.addItem(11);
        completedSet.addItem(12, Item.Pool.H, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(new Item(12, "Mock", "url", Item.Pool.L, LocalDate.now(), 0))));

        assertEquals(2, cmdLine.execute());
    }

    @Test
    void testNonExistentDBItems() {
        completedSet.addItem(30, Item.Pool.M, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.empty());

        assertEquals(2, cmdLine.execute());
    }

    // --- EDGE & RUNTIME CASES ---

    @Test
    void testCommitRollbackOnDBFailure() {
        Item item = new Item(40, "Mock", "url", Item.Pool.M, LocalDate.now(), 0);
        completedSet.addItem(40, Item.Pool.L, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(item)));
        when(db.updateItemsBatch(anyList())).thenReturn(false);

        assertEquals(1, cmdLine.execute());
        verify(completedSet, atLeastOnce()).addItem(eq(40), any(), any());
    }

    @Test
    void testRollbackWithListFlag() {
        parent.list = true;
        Item item = new Item(50, "Mock", "url", Item.Pool.L, LocalDate.now(), 1);
        completedSet.addItem(50, Item.Pool.H, LocalDate.now());
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(item)));
        when(db.updateItemsBatch(anyList())).thenReturn(false);

        assertEquals(1, cmdLine.execute());
        verify(completedSet, atLeastOnce()).addItem(eq(50), any(), any());
    }
}
