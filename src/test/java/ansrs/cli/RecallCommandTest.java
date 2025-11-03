package ansrs.cli;

import ansrs.data.Item;
import ansrs.db.DBManager;
import ansrs.service.RecallService;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecallCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private SRSCommand parent;
    private RecallCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = spy(new WorkingSet(tempDir.resolve("working.set")));
        completedSet = spy(new CompletedSet(tempDir.resolve("completed.set")));
        db = mock(DBManager.class);

        parent = new SRSCommand(workingSet, completedSet, db);
        cmd = spy(new RecallCommand());
        cmdLine = new CommandLine(cmd);
        cmd.parent = parent;
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {}
                });
    }

    // --- VALID CASES ---

    @Test
    void testSimpleRecallIntoEmptyWorkingSet() {
        RecallService mockService = mock(RecallService.class);
        doReturn(List.of(1, 2, 3)).when(mockService).recall(3);
        doReturn(mockService).when(cmd).createRecallService(any());

        assertEquals(0, cmdLine.execute("3"));
        verify(workingSet, atLeastOnce()).fillSet(anySet());
    }

    @Test
    void testRecallAppendsWhenAppendFlagUsed() {
        workingSet.fillSet(Set.of(1, 2));
        RecallService mockService = mock(RecallService.class);
        doReturn(List.of(3, 4)).when(mockService).recall(2);
        doReturn(mockService).when(cmd).createRecallService(any());

        assertEquals(0, cmdLine.execute("2", "--force", "--append"));
        verify(workingSet, atLeastOnce()).fillSet(anySet());
        assertTrue(workingSet.getItemIdSet().containsAll(Set.of(1, 2, 3, 4)));
    }

    @Test
    void testRecallOverwritesWhenForceUsedWithoutAppend() {
        workingSet.fillSet(Set.of(10, 20));
        RecallService mockService = mock(RecallService.class);
        doReturn(List.of(30, 40)).when(mockService).recall(2);
        doReturn(mockService).when(cmd).createRecallService(any());

        assertEquals(0, cmdLine.execute("2", "--force"));
        verify(workingSet, atLeastOnce()).fillSet(anySet());
        assertTrue(workingSet.getItemIdSet().containsAll(Set.of(30, 40)));
        assertFalse(workingSet.getItemIdSet().contains(10));
    }

    @Test
    void testParentListTriggersPrinter() {
        parent.list = true;
        RecallService mockService = mock(RecallService.class);
        doReturn(List.of(1, 2)).when(mockService).recall(2);
        doReturn(mockService).when(cmd).createRecallService(any());

        assertEquals(0, cmdLine.execute("2"));
        verify(workingSet, atLeastOnce()).fillSet(anySet());
    }

    // --- VALIDATION FAILURES ---

    @Test
    void testNegativeRecallCount() {
        assertEquals(2, cmdLine.execute("-3"));
    }

    @Test
    void testZeroRecallCount() {
        assertEquals(2, cmdLine.execute("0"));
    }

    @Test
    void testNonEmptyWorkingSetWithoutForceFlag() {
        workingSet.fillSet(Set.of(1, 2));
        when(db.getAllItems()).thenReturn(Optional.of(List.of(new Item())));
        assertEquals(2, cmdLine.execute("2"));
    }

    // --- EDGE CASES ---

    @Test
    void testAppendFlagWithoutForceStillFails() {
        workingSet.fillSet(Set.of(1));
        when(db.getAllItems()).thenReturn(Optional.of(List.of(new Item())));
        assertEquals(2, cmdLine.execute("2", "--append"));
    }

    @Test
    void testForceAndAppendBothAppliedWorks() {
        workingSet.fillSet(Set.of(5));
        RecallService mockService = mock(RecallService.class);
        doReturn(List.of(6, 7)).when(mockService).recall(2);
        doReturn(mockService).when(cmd).createRecallService(any());

        assertEquals(0, cmdLine.execute("2", "--force", "--append"));
        assertTrue(workingSet.getItemIdSet().containsAll(Set.of(5, 6, 7)));
    }
}
