package ansrs.cli;

import ansrs.db.ArchiveRepository;
import ansrs.db.GroupRepository;
import ansrs.db.ItemRepository;
import ansrs.set.CompletedSet;
import ansrs.set.WorkingSet;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SRSCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private ItemRepository db;
    private ArchiveRepository am;
    private GroupRepository gr;
    private SRSCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = spy(new WorkingSet(tempDir.resolve("working.set")));
        completedSet = spy(new CompletedSet(tempDir.resolve("completed.set")));
        db = mock(ItemRepository.class);

        cmd = spy(new SRSCommand(workingSet, completedSet, db, am, gr));
        cmdLine = new CommandLine(cmd);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {}
                });
    }

    @Test
    void testListFlagPrintsFullState() {
        when(db.getAllItems()).thenReturn(Optional.empty());
        int exitCode = cmdLine.execute("--list");
        assertEquals(0, exitCode);
        verify(workingSet, atLeastOnce()).getItemIdSet();
        verify(completedSet, atLeastOnce()).getItems();
        verify(db, atLeastOnce()).getAllItems();
    }


    @Test
    void testListAndSetFlagPrintsSetState() {
        when(db.getItemsFromList(anyList())).thenReturn(Optional.of(new java.util.ArrayList<>()));
        int exitCode = cmdLine.execute("--list", "--set");
        assertEquals(0, exitCode);
        verify(db, atLeast(2)).getItemsFromList(anyList());
        verify(completedSet, atLeastOnce()).getItems();
    }

    @Test
    void testItemIdPrintsItem() {
        when(db.getItemById(3)).thenReturn(java.util.Optional.empty());
        int exitCode = cmdLine.execute("--id", "3");
        assertEquals(0, exitCode);
        verify(db, atLeastOnce()).getItemById(3);
    }

    @Test
    void testNegativeItemIdFailsValidation() {
        int exitCode = cmdLine.execute("--id", "-2");
        assertEquals(2, exitCode);
        verify(db, never()).getItemById(anyInt());
    }

    @Test
    void testSetFlagWithoutListFailsValidation() {
        int exitCode = cmdLine.execute("--set");
        assertEquals(2, exitCode);
        verifyNoInteractions(workingSet, completedSet);
    }

    @Test
    void testVersionAndHelpExitNormally() {
        assertEquals(0, cmdLine.execute("--version"));
        assertEquals(0, cmdLine.execute("--help"));
    }

    @Test
    void testNameFlagCallsSearchItemsByName() {
        when(db.searchItemsByName("widget")).thenReturn(Optional.of(new java.util.ArrayList<>()));
        int exitCode = cmdLine.execute("--name", "widget");
        assertEquals(0, exitCode);
        verify(db, times(1)).searchItemsByName("widget");
    }

    @Test
    void testNameFlagTrimsWhitespace() {
        when(db.searchItemsByName("widget")).thenReturn(Optional.of(new java.util.ArrayList<>()));
        int exitCode = cmdLine.execute("--name", "   widget   ");
        assertEquals(0, exitCode);
        verify(db, times(1)).searchItemsByName("widget");
    }

    @Test
    void testBlankNameFailsValidation() {
        int exitCode = cmdLine.execute("--name", "   ");
        assertEquals(2, exitCode);
        verify(db, never()).searchItemsByName(anyString());
    }

    @Test
    void testSingleCharacterNameFailsValidation() {
        int exitCode = cmdLine.execute("--name", "a");
        assertEquals(2, exitCode);
        verify(db, never()).searchItemsByName(anyString());
    }

    @Test
    void testNameFlagWithListAlsoPrintsState() {
        when(db.searchItemsByName("widget")).thenReturn(Optional.of(new java.util.ArrayList<>()));
        when(db.getAllItems()).thenReturn(Optional.of(new java.util.ArrayList<>()));
        int exitCode = cmdLine.execute("--name", "widget", "--list");
        assertEquals(0, exitCode);
        verify(db, atLeastOnce()).searchItemsByName("widget");
        verify(db, atLeastOnce()).getAllItems();
    }

}
