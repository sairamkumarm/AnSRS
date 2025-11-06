package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.ArchiveManager;
import ansrs.db.DBManager;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArchiveCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private ArchiveManager am;
    private SRSCommand parent;
    private ArchiveCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-archive-test");
        workingSet = spy(new WorkingSet(tempDir.resolve("working.set")));
        completedSet = spy(new CompletedSet(tempDir.resolve("completed.set")));
        db = mock(DBManager.class);
        am = mock(ArchiveManager.class);
        parent = new SRSCommand(workingSet, completedSet, db, am);
        cmd = new ArchiveCommand();
        cmd.parent = parent;
        cmdLine = new CommandLine(cmd);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                });
    }

    // --- VALID CASES ---

    @Test
    void testAddSuccess() {
        when(db.contains(1)).thenReturn(true);
        when(db.getItemById(1)).thenReturn(Optional.of(new Item(1, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)));
        when(am.insertItem(any())).thenReturn(true);
        when(db.deleteItemsById(1)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--add", "1"));
        verify(am).insertItem(any());
        verify(db).deleteItemsById(1);
    }

    @Test
    void testDeleteSuccess() {
        when(am.contains(10)).thenReturn(true);
        when(am.deleteItemsById(10)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--delete", "10", "--sure"));
        verify(am).deleteItemsById(10);
    }

    @Test
    void testRestoreSuccess() {
        Item item = new Item(20, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(am.contains(20)).thenReturn(true);
        when(db.contains(20)).thenReturn(false);
        when(am.getItemById(20)).thenReturn(Optional.of(item));
        when(db.insertItem(item)).thenReturn(true);
        when(am.deleteItemsById(20)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--restore", "20"));
        verify(db).insertItem(item);
        verify(am).deleteItemsById(20);
    }

    @Test
    void testListAll() {
        when(am.getAllItems()).thenReturn(Optional.of(List.of(
                new Item(1, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1),
                new Item(2, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)
        )));
        assertEquals(0, cmdLine.execute("--list"));
        verify(am).getAllItems();
    }

    @Test
    void testGetById() {
        when(am.getItemById(1)).thenReturn(Optional.of(new Item(1, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)));
        assertEquals(0, cmdLine.execute("--id", "1"));
        verify(am).getItemById(1);
    }

    @Test
    void testSearchByName() {
        when(am.searchItemsByName("test")).thenReturn(Optional.of(List.of(
                new Item(1, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)
        )));
        assertEquals(0, cmdLine.execute("--name", "test"));
        verify(am).searchItemsByName("test");
    }

    @Test
    void testArchiveAllSuccess() {
        when(db.getAllItems()).thenReturn(Optional.of(List.of(
                new Item(1,"Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1),
                new Item(2, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)
        )));
        when(am.insertItemsBatch(any())).thenReturn(true);
        when(db.deleteItemsById(anyInt())).thenReturn(true);

        assertEquals(0, cmdLine.execute("--all", "--sure"));
        verify(am).insertItemsBatch(any());
        verify(db, atLeastOnce()).deleteItemsById(anyInt());
    }

    // --- FAILURE CASES ---

    @Test
    void testAddFailsWhenNotInDb() {
        when(db.contains(99)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--add", "99"));
    }

    @Test
    void testAddFailsWhenItemInSet() {
        workingSet.addItem(5);
        when(db.contains(5)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--add", "5"));
    }

    @Test
    void testAddFailsOnInsertError() {
        when(db.contains(10)).thenReturn(true);
        when(db.getItemById(10)).thenReturn(Optional.of(new Item(10, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)));
        when(am.insertItem(any())).thenReturn(false);
        assertEquals(1, cmdLine.execute("--add", "10"));
    }

    @Test
    void testDeleteFailsWithoutSure() {
        when(am.contains(11)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--delete", "11"));
    }

    @Test
    void testDeleteFailsIfItemMissing() {
        when(am.contains(11)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--delete", "11", "--sure"));
    }

    @Test
    void testDeleteFailsOnArchiveError() {
        when(am.contains(12)).thenReturn(true);
        when(am.deleteItemsById(12)).thenReturn(false);
        assertEquals(1, cmdLine.execute("--delete", "12", "--sure"));
    }

    @Test
    void testRestoreFailsIfMissingInArchive() {
        when(am.contains(21)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--restore", "21"));
    }

    @Test
    void testRestoreFailsIfAlreadyInDb() {
        when(am.contains(22)).thenReturn(true);
        when(db.contains(22)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--restore", "22"));
    }

    @Test
    void testRestoreFailsIfFetchFails() {
        when(am.contains(23)).thenReturn(true);
        when(db.contains(23)).thenReturn(false);
        when(am.getItemById(23)).thenReturn(Optional.empty());
        assertEquals(1, cmdLine.execute("--restore", "23"));
    }

    @Test
    void testArchiveAllFailsWithoutSure() {
        assertEquals(2, cmdLine.execute("--all"));
    }

    @Test
    void testArchiveAllFailsWhenDbEmpty() {
        when(db.getAllItems()).thenReturn(Optional.of(Collections.emptyList()));
        assertEquals(1, cmdLine.execute("--all", "--sure"));
    }

    @Test
    void testArchiveAllFailsOnInsertBatchError() {
        when(db.getAllItems()).thenReturn(Optional.of(List.of(new Item(1, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1))));
        when(am.insertItemsBatch(any())).thenReturn(false);
        assertEquals(1, cmdLine.execute("--all", "--sure"));
    }

    @Test
    void testMultipleFlagsThrows() {
        assertEquals(2, cmdLine.execute("--list", "--add", "1"));
    }

    @Test
    void testNoOperationThrows() {
        assertEquals(2, cmdLine.execute());
    }

    // --- EDGE CASES ---

    @Test
    void testArchiveAllSkipsItemsInSets() {
        workingSet.addItem(1);
        completedSet.addItem(2, null);
        when(db.getAllItems()).thenReturn(Optional.of(List.of(
                new Item(1, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1),
                new Item(2, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1),
                new Item(3, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)
        )));
        when(am.insertItemsBatch(any())).thenReturn(true);
        when(db.deleteItemsById(anyInt())).thenReturn(true);

        assertEquals(0, cmdLine.execute("--all", "--sure"));
        verify(am).insertItemsBatch(argThat(list -> list.size() == 1));
    }

    // --- RESTORE-ALL TESTS ---

    @Test
    void testRestoreAllSuccess() {
        List<Item> items = List.of(
                new Item(100, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1),
                new Item(101, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1)
        );
        when(am.getAllItems()).thenReturn(Optional.of(items));
        when(db.contains(anyInt())).thenReturn(false);
        when(db.insertItem(any())).thenReturn(true);
        when(am.deleteItemsById(anyInt())).thenReturn(true);

        assertEquals(0, cmdLine.execute("--restore-all", "--sure"));
        verify(db, times(2)).insertItem(any());
        verify(am, times(2)).deleteItemsById(anyInt());
    }

    @Test
    void testRestoreAllFailsWithoutSure() {
        assertEquals(2, cmdLine.execute("--restore-all"));
    }

    @Test
    void testRestoreAllSkipsItemsAlreadyInDb() {
        Item a = new Item(200, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        Item b = new Item(201, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(am.getAllItems()).thenReturn(Optional.of(List.of(a, b)));
        when(db.contains(200)).thenReturn(true);
        when(db.contains(201)).thenReturn(false);
        when(db.insertItem(b)).thenReturn(true);
        when(am.deleteItemsById(201)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--restore-all", "--sure"));
        verify(db, times(1)).insertItem(b);
        verify(am, times(1)).deleteItemsById(201);
    }

    @Test
    void testRestoreAllFailsIfArchiveEmpty() {
        when(am.getAllItems()).thenReturn(Optional.of(Collections.emptyList()));
        assertEquals(1, cmdLine.execute("--restore-all", "--sure"));
    }

    @Test
    void testRestoreAllHandlesPartialFailure() {
        Item a = new Item(300, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        Item b = new Item(301, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(am.getAllItems()).thenReturn(Optional.of(List.of(a, b)));
        when(db.contains(anyInt())).thenReturn(false);
        when(db.insertItem(a)).thenReturn(false); // fail first insert
        when(db.insertItem(b)).thenReturn(true);
        when(am.deleteItemsById(b.getItemId())).thenReturn(true);

        assertEquals(0, cmdLine.execute("--restore-all", "--sure"));
        verify(db).insertItem(a);
        verify(db).insertItem(b);
        verify(am).deleteItemsById(b.getItemId());
    }

}
