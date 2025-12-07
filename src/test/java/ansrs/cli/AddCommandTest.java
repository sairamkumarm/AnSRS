package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.ArchiveRepository;
import ansrs.db.GroupRepository;
import ansrs.db.ItemRepository;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private ItemRepository db;
    private ArchiveRepository am;
    private GroupRepository gr;
    private AddCommand cmd;
    private SRSCommand parent;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        Path workingSetPath = tempDir.resolve("working.set");
        Path completedSetPath = tempDir.resolve("completed.set");
        Path dbPath = tempDir.resolve("ansrs.db");

        workingSet = spy(new WorkingSet(workingSetPath));
        completedSet = spy(new CompletedSet(completedSetPath));
        db = mock(ItemRepository.class);
        am= mock(ArchiveRepository.class);
        parent = new SRSCommand(workingSet, completedSet, db, am, gr);
        cmd = new AddCommand();
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

    // --- VALID INSERTS ---

    @Test
    void testValidInsert() {
        when(db.insertItem(any(Item.class))).thenReturn(true);
        int exit = cmdLine.execute("1", "Alpha", "https://alpha.com", "H");
        assertEquals(0, exit);
        verify(db).insertItem(any(Item.class));
        verify(workingSet, never()).addItem(any());
    }

    @Test
    void testValidInsertWithRecallFields() {
        when(db.insertItem(any(Item.class))).thenReturn(true);
        String date = LocalDate.now().minusDays(2).toString();
        int exit = cmdLine.execute("2", "Beta", "https://beta.com", "M",
                "--last-recall", date, "--total-recalls", "5");
        assertEquals(0, exit);
        verify(db).insertItem(any(Item.class));
    }

    // --- INSERT VALIDATION FAILURES ---

    @Test
    void testMissingInsertParameters() {
        int exit = cmdLine.execute("3", "Gamma", "https://gamma.com");
        assertEquals(2, exit);
    }

    @Test
    void testInvalidUrlScheme() {
        int exit = cmdLine.execute("4", "Delta", "http://delta.com", "H");
        assertEquals(2, exit);
    }

    @Test
    void testInvalidPoolValue() {
        int exit = cmdLine.execute("5", "Epsilon", "https://epsi.com", "X");
        assertEquals(2, exit);
    }

    @Test
    void testFutureRecallDateFails() {
        String futureDate = LocalDate.now().plusDays(5).toString();
        int exit = cmdLine.execute("6", "Zeta", "https://zeta.com", "M",
                "--last-recall", futureDate);
        assertEquals(2, exit);
    }

    @Test
    void testInvalidRecallDateFormatFails() {
        int exit = cmdLine.execute("7", "Eta", "https://eta.com", "L",
                "--last-recall", "2025/01/02");
        assertEquals(2, exit);
    }

    @Test
    void testNegativeTotalRecallsFails() {
        int exit = cmdLine.execute("8", "Theta", "https://theta.com", "H",
                "--total-recalls", "-3");
        assertEquals(2, exit);
    }

    // --- INSERT DB BEHAVIOR ---

    @Test
    void testInsertFailsWhenDuplicate() {
        when(db.insertItem(any())).thenReturn(false);
        int exit = cmdLine.execute("9", "Dup", "https://dup.com", "L");
        assertEquals(1, exit);
        verify(db).insertItem(any());
    }

    @Test
    void testInsertThrowsException() {
        doThrow(new RuntimeException("DB down")).when(db).insertItem(any(Item.class));
        int exit = cmdLine.execute("10", "Err", "https://err.com", "H");
        assertEquals(1, exit);
        verify(db).insertItem(any(Item.class));
    }

    // --- VALID UPDATES ---

    @Test
    void testValidUpdateSingleField() {
        Item existing = new Item(100, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(db.getItemById(100)).thenReturn(Optional.of(existing));
        when(db.updateItem(any())).thenReturn(true);

        int exit = cmdLine.execute("--update", "100", "--link", "https://new.com");
        assertEquals(0, exit);
        verify(db).updateItem(any(Item.class));
    }

    @Test
    void testValidUpdateMultipleFields() {
        Item existing = new Item(101, "Old", "https://old.com", Item.Pool.M, LocalDate.now(), 2);
        when(db.getItemById(101)).thenReturn(Optional.of(existing));
        when(db.updateItem(any())).thenReturn(true);

        int exit = cmdLine.execute("--update", "101",
                "--name", "NewName",
                "--pool", "L",
                "--total-recalls", "5");
        assertEquals(0, exit);
        verify(db).updateItem(any(Item.class));
    }

    // --- UPDATE VALIDATION FAILURES ---

    @Test
    void testUpdateWithoutAnyFieldFails() {
        Item existing = new Item(102, "A", "https://a.com", Item.Pool.L, LocalDate.now(), 1);
        when(db.getItemById(102)).thenReturn(Optional.of(existing));

        int exit = cmdLine.execute("--update", "102");
        assertEquals(2, exit);
    }

    @Test
    void testUpdateNonExistentItemFails() {
        when(db.getItemById(103)).thenReturn(Optional.empty());
        int exit = cmdLine.execute("--update", "103", "--name", "Ghost");
        assertEquals(1, exit);
    }

    @Test
    void testUpdateInvalidUrlFails() {
        Item existing = new Item(104, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(db.getItemById(104)).thenReturn(Optional.of(existing));
        int exit = cmdLine.execute("--update", "104", "--link", "http://wrong.com");
        assertEquals(2, exit);
    }

    @Test
    void testUpdateInvalidPoolFails() {
        Item existing = new Item(105, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(db.getItemById(105)).thenReturn(Optional.of(existing));
        int exit = cmdLine.execute("--update", "105", "--pool", "Z");
        assertEquals(2, exit);
    }

    @Test
    void testUpdateNegativeRecallsFails() {
        Item existing = new Item(106, "Old", "https://old.com", Item.Pool.M, LocalDate.now(), 1);
        when(db.getItemById(106)).thenReturn(Optional.of(existing));
        int exit = cmdLine.execute("--update", "106", "--total-recalls", "-2");
        assertEquals(2, exit);
    }

    // --- UPDATE DB FAILURES ---

    @Test
    void testUpdateFailsInDb() {
        Item existing = new Item(107, "Old", "https://old.com", Item.Pool.H, LocalDate.now(), 1);
        when(db.getItemById(107)).thenReturn(Optional.of(existing));
        when(db.updateItem(any())).thenReturn(false);

        int exit = cmdLine.execute("--update", "107", "--name", "New");
        assertEquals(1, exit);
    }

    @Test
    void testUpdateDbThrowsExceptionCaught() {
        Item existing = new Item(108, "Old", "https://old.com", Item.Pool.L, LocalDate.now(), 2);
        when(db.getItemById(108)).thenReturn(Optional.of(existing));
        doThrow(new RuntimeException("DB crash")).when(db).updateItem(any(Item.class));

        int exit = cmdLine.execute("--update", "108", "--name", "Boom");
        assertEquals(1, exit);
    }
}
