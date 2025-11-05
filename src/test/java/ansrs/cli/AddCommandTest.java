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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private AddCommand cmd;
    private SRSCommand parent;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        // Create an isolated temp workspace for each test
        tempDir = Files.createTempDirectory("ansrs-test");

        Path workingSetPath = tempDir.resolve("working.set");
        Path completedSetPath = tempDir.resolve("completed.set");
        Path dbPath = tempDir.resolve("ansrs.db");

        // Use real sets but mock DB
        workingSet = new WorkingSet(workingSetPath);
        completedSet = new CompletedSet(completedSetPath);
        db = mock(DBManager.class);

        parent = new SRSCommand(workingSet, completedSet, db);
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

    // --- VALID CASES ---

    @Test
    void testValidInsert() throws Exception {
        when(db.insertItem(any(Item.class))).thenReturn(true);
        new CommandLine(cmd).execute("1", "Alpha", "https://aphpa", "H");
        verify(db).insertItem(any(Item.class));
    }

    @Test
    void testValidUpdateFlow() throws Exception {
        when(db.insertItem(any())).thenReturn(false);
        when(db.updateItem(any())).thenReturn(true);
        assertEquals(0,
                cmdLine.execute("2", "Beta", "https://beta.com", "M", "-u"));
        verify(db).updateItem(any());
    }

    @Test
    void testValidInsertWithLastRecallAndTotalRecalls() throws Exception {
        when(db.insertItem(any())).thenReturn(true);
        String date = LocalDate.now().minusDays(5).toString();
        assertEquals(0,
                cmdLine.execute("3", "Gamma", "https://gamma.com", "L",
                        "--last-recall", date,
                        "--total-recalls", "2"));
        verify(db).insertItem(any(Item.class));
    }

    // --- VALIDATION FAILURES ---

    @Test
    void testMissingParameters() {
        assertEquals(2,cmdLine.execute("0", "", "https://ok.com", "H"));
    }

    @Test
    void testInvalidUrlScheme() {
        assertEquals(2,cmdLine.execute("1", "Invalid", "http://oops.com", "M"));
    }

    @Test
    void testInvalidPoolValue() {
        assertEquals(2,
                cmdLine.execute("1", "WrongPool", "https://pool.com", "X"));
    }

    @Test
    void testFutureRecallDate() {
        String date = LocalDate.now().plusDays(2).toString();
        assertEquals(2,
                cmdLine.execute("1", "Future", "https://future.com", "H",
                        "--last-recall", date));
    }

    @Test
    void testInvalidRecallDateFormat() {
        assertEquals(2,
                cmdLine.execute("1", "BadDate", "https://date.com", "M",
                        "--last-recall", "13-2024-01"));
    }

    @Test
    void testNegativeTotalRecalls() {
        assertEquals(2,
                cmdLine.execute("1", "Neg", "https://neg.com", "L",
                        "--total-recalls", "-1"));
    }

    // --- RUNTIME BEHAVIOR ---

    @Test
    void testInsertFailsWithoutUpdate() throws Exception {
        when(db.insertItem(any())).thenReturn(false);
        assertEquals(1,
                cmdLine.execute("10", "Dup", "https://dup.com", "H"));
        verify(db).insertItem(any());
    }

    @Test
    void testInsertFailsAndUpdateFails() throws Exception {
        when(db.insertItem(any())).thenReturn(false);
        when(db.updateItem(any())).thenReturn(false);
        assertEquals(0,
                cmdLine.execute("11", "UpdateFail", "https://fail.com", "M", "-u"));
    }


    @Test
    void testExceptionInDbInsertCaught() throws Exception {
        doThrow(new RuntimeException("DB down")).when(db).insertItem(any(Item.class));
        assertEquals(1,
                cmdLine.execute("13", "Throw", "https://throw.com", "H"));
        verify(db).insertItem(any(Item.class));
    }
}
