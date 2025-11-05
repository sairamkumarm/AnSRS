package ansrs.cli;

import ansrs.db.DBManager;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private DBManager db;
    private SRSCommand parent;
    private DeleteCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = spy(new WorkingSet(tempDir.resolve("working.set")));
        completedSet = spy(new CompletedSet(tempDir.resolve("completed.set")));
        db = mock(DBManager.class);

        parent = new SRSCommand(workingSet, completedSet, db);
        cmd = new DeleteCommand();
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
    void testDeleteFromWorkingSet() {
        workingSet.addItem(1);
        assertEquals(0, cmdLine.execute("1", "--sure"));
        verify(workingSet).removeItem(1);
    }

    @Test
    void testDeleteFromCompletedSet() {
        completedSet.addItem(2, null);
        assertEquals(0, cmdLine.execute("2", "--completed", "--sure"));
        verify(completedSet).removeItem(2);
    }

    @Test
    void testDeleteFromDatabaseAlsoRemovesFromSets() {
        when(db.deleteItemsById(3)).thenReturn(true);
        workingSet.addItem(3);
        completedSet.addItem(3, null);
        assertEquals(0, cmdLine.execute("3", "--database", "--sure"));
        verify(db).deleteItemsById(3);
        verify(workingSet).removeItem(3);
        verify(completedSet).removeItem(3);
    }

    @Test
    void testParentListTriggersPrinter() {
        parent.list = true;
        workingSet.addItem(5);
        assertEquals(0, cmdLine.execute("5", "--sure"));
        verify(workingSet).removeItem(5);
    }

    @Test
    void testHardResetSuccess() {
        when(db.clearDatabase()).thenReturn(true);
        doReturn(true).when(completedSet).clearSet();
        doReturn(true).when(workingSet).clearSet();

        assertEquals(0, cmdLine.execute("--sure", "--hard-reset"));
        verify(db).clearDatabase();
        verify(workingSet).clearSet();
        verify(completedSet).clearSet();
    }

    @Test
    void testHardResetFailure() {
        when(db.clearDatabase()).thenReturn(false);
        assertEquals(1, cmdLine.execute("--sure", "--hard-reset"));
    }

    // --- VALIDATION FAILURES ---

    @Test
    void testMissingSureFlagThrows() {
        workingSet.addItem(7);
        assertEquals(2,
                cmdLine.execute("7"));
    }

    @Test
    void testNegativeItemId() {
        assertEquals(2,
                cmdLine.execute("-5", "--sure"));
    }

    @Test
    void testItemIdZeroWithoutResetThrows() {
        assertEquals(2,
                cmdLine.execute("0", "--sure"));
    }

    @Test
    void testCompletedSetDeletionOfNonExistentItemThrows() {
        assertEquals(2,
                cmdLine.execute("10", "--completed", "--sure"));
    }

    @Test
    void testWorkingSetDeletionOfNonExistentItemThrows() {
        assertEquals(2,
                cmdLine.execute("11", "--sure"));
    }

    @Test
    void testDatabaseDeletionFailureReturnsOne() {
        when(db.deleteItemsById(12)).thenReturn(false);
        assertEquals(1, cmdLine.execute("12", "--database", "--sure"));
    }

    @Test
    void testHardResetWithoutSureThrows() {
        assertEquals(2,
                cmdLine.execute("--hard-reset"));
    }

    // --- EDGE CASES ---

    @Test
    void testDatabaseDeletionWithMissingSets() {
        when(db.deleteItemsById(15)).thenReturn(true);
        doReturn(false).when(workingSet).removeItem(15);
        doReturn(false).when(completedSet).removeItem(15);

        assertEquals(0, cmdLine.execute("15", "--database", "--sure"));
        verify(db).deleteItemsById(15);
    }


//    @Test
//    void testDeletionStillWorksIfWorkingSetThrows() {
//        workingSet.addItem(20);
//        doThrow(new RuntimeException("fail")).when(workingSet).removeItem(20);
//        assertEquals(1,
//                cmdLine.execute("20", "--sure"));
//    }

    @Test
    void testMultipleDeletesSequentially() {
        workingSet.fillSet(Set.of(30, 31));
        assertEquals(0, cmdLine.execute("30", "--sure"));
        assertEquals(0, cmdLine.execute("31", "--sure"));
        verify(workingSet, times(2)).removeItem(anyInt());
    }

    @Test
    void testClearWorkingSetSuccess() {
        doReturn(true).when(workingSet).clearSet();
        assertEquals(0, cmdLine.execute("--working-all", "--sure"));
        verify(workingSet).clearSet();
    }

    @Test
    void testClearWorkingSetFailure() {
        doReturn(false).when(workingSet).clearSet();
        assertEquals(1, cmdLine.execute("--working-all", "--sure"));
    }

    @Test
    void testClearCompletedSetSuccess() {
        doReturn(true).when(completedSet).clearSet();
        assertEquals(0, cmdLine.execute("--completed-all", "--sure"));
        verify(completedSet).clearSet();
    }

    @Test
    void testClearCompletedSetFailure() {
        doReturn(false).when(completedSet).clearSet();
        assertEquals(1, cmdLine.execute("--completed-all", "--sure"));
    }

    @Test
    void testHardResetCannotBePairedWithWorkingAll() {
        assertEquals(2,
                cmdLine.execute("--hard-reset", "--working-all", "--sure"));
    }

    @Test
    void testHardResetCannotBePairedWithCompletedAll() {
        assertEquals(2,
                cmdLine.execute("--hard-reset", "--completed-all", "--sure"));
    }

    @Test
    void testItemIdSkippedWithoutResetThrows() {
        assertEquals(2,
                cmdLine.execute("--sure"));
    }

    @Test
    void testItemIdGivenWithResetThrows() {
        assertEquals(2,
                cmdLine.execute("42", "--hard-reset", "--sure"));
    }

    @Test
    void testItemIdGivenWithClearWorkingSetThrows() {
        assertEquals(2,
                cmdLine.execute("43", "--working-all", "--sure"));
    }

    @Test
    void testItemIdGivenWithClearCompletedSetThrows() {
        assertEquals(2,
                cmdLine.execute("44", "--completed-all", "--sure"));
    }

    @Test
    void testDatabaseDeletionLogsEvenIfSetsEmpty() {
        when(db.deleteItemsById(50)).thenReturn(true);
        doReturn(false).when(workingSet).removeItem(50);
        doReturn(false).when(completedSet).removeItem(50);
        assertEquals(0, cmdLine.execute("50", "--database", "--sure"));
    }

    @Test
    void testCompletedAllWithMissingSureFlagThrows() {
        assertEquals(2, cmdLine.execute("--completed-all"));
    }

    @Test
    void testWorkingAllWithMissingSureFlagThrows() {
        assertEquals(2, cmdLine.execute("--working-all"));
    }

    @Test
    void testResetWithMissingSureFlagThrows() {
        assertEquals(2, cmdLine.execute("--hard-reset"));
    }

    @Test
    void testResetPartialClearFailsReturnsOne() {
        when(db.clearDatabase()).thenReturn(true);
        doReturn(true).when(workingSet).clearSet();
        doReturn(false).when(completedSet).clearSet();
        assertEquals(1, cmdLine.execute("--sure", "--hard-reset"));
    }

    @Test
    void testCompletedSetDeleteWithInvalidItemIdThrows() {
        assertEquals(2,
                cmdLine.execute("-1", "--completed", "--sure"));
    }


    @Test
    void testClearWorkingAndCompletedSetTogetherSuccess() {
        doReturn(true).when(workingSet).clearSet();
        doReturn(true).when(completedSet).clearSet();
        assertEquals(0, cmdLine.execute("--working-all", "--completed-all", "--sure"));
        verify(workingSet).clearSet();
        verify(completedSet).clearSet();
    }

    @Test
    void testClearWorkingAndCompletedSetTogetherFailure() {
        doReturn(true).when(workingSet).clearSet();
        doReturn(false).when(completedSet).clearSet();
        assertEquals(1, cmdLine.execute("--working-all", "--completed-all", "--sure"));
        verify(workingSet).clearSet();
        verify(completedSet).clearSet();
    }

    @Test
    void testItemIdSkippedWithBothAllFlagsAllowed() {
        assertEquals(0, cmdLine.execute("--working-all", "--completed-all", "--sure"));
    }

    @Test
    void testItemIdGivenWithBothAllFlagsThrows() {
        assertEquals(2,
                cmdLine.execute("21", "--working-all", "--completed-all", "--sure"));
    }

    @Test
    void testHardResetWithBothAllFlagsThrows() {
        assertEquals(2,
                cmdLine.execute("--hard-reset", "--working-all", "--completed-all", "--sure"));
    }

    @Test
    void testBothAllFlagsWithoutSureThrows() {
        assertEquals(2,
                cmdLine.execute("--working-all", "--completed-all"));
    }

    @Test
    void testClearBothSetsDoesNotTouchDatabase() {
        doReturn(true).when(workingSet).clearSet();
        doReturn(true).when(completedSet).clearSet();
        assertEquals(0, cmdLine.execute("--working-all", "--completed-all", "--sure"));
        verifyNoInteractions(db);
    }


}
