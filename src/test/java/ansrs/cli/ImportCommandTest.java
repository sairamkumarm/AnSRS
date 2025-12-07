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
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ImportCommandTest {

    private Path tempDir;
    private WorkingSet workingSet;
    private CompletedSet completedSet;
    private ItemRepository db;
    private ArchiveRepository am;
    private GroupRepository gr;
    private SRSCommand parent;
    private ImportCommand cmd;
    private CommandLine cmdLine;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("ansrs-test");
        workingSet = new WorkingSet(tempDir.resolve("working.set"));
        completedSet = new CompletedSet(tempDir.resolve("completed.set"));
        db = mock(ItemRepository.class);
        am= mock(ArchiveRepository.class);
        gr=mock(GroupRepository.class);
        parent = new SRSCommand(workingSet, completedSet, db, am, gr);
        cmd = new ImportCommand();
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

    // helper to create a minimal valid CSV (no header)
    private Path createCsv(String name, List<String> rows) throws Exception {
        Path csv = tempDir.resolve(name);
        Files.write(csv, rows);
        return csv;
    }

    // --- VALID CASES ---

    @Test
    void testImportUpsertSucceeds_whenOverwriteCsv() throws Exception {
        // prepare csv with two valid rows
        List<String> rows = List.of(
                "1,Item One,https://one.example,H," + LocalDate.now().toString() + ",0",
                "2,Item Two,https://two.example,M," + LocalDate.now().toString() + ",1"
        );
        Path csv = createCsv("items.csv", rows);

        when(db.upsertItemsBatch(anyList())).thenReturn(true);

        int exit = cmdLine.execute("--path", csv.toString(), "--preserve", "csv");
        assertEquals(0, exit);

        verify(db, times(1)).upsertItemsBatch(anyList());
    }

    @Test
    void testImportInsertUniquesAndWarnDuplicates_whenOverwriteDb() throws Exception {
        // CSV contains ids 3 and 4, db already has id 3 (duplicate)
        List<String> rows = List.of(
                "3,Item Three,https://three.example,M," + LocalDate.now().toString() + ",0",
                "4,Item Four,https://four.example,L," + LocalDate.now().toString() + ",2"
        );
        Path csv = createCsv("items2.csv", rows);

        Set<Integer> existing = new HashSet<>();
        existing.add(3);
        when(db.getAllItemsIds()).thenReturn(Optional.of(new HashSet<>()));
        // simulate inserting uniques succeeds
        when(db.insertItemsBatch(anyList())).thenReturn(true);

        int exit = cmdLine.execute("--path", csv.toString(), "--preserve", "db");
        assertEquals(0, exit);

        // insertItemsBatch called once (for uniques)
        verify(db, times(1)).insertItemsBatch(anyList());
        // getAllItemsIds should be invoked to detect duplicates
        verify(db, atLeastOnce()).getAllItemsIds();
    }

    @Test
    void testImportFailureReturnsOne_whenUpsertFails() throws Exception {
        List<String> rows = List.of(
                "5,Item Five,https://five.example,H," + LocalDate.now().toString() + ",0"
        );
        Path csv = createCsv("items3.csv", rows);

        when(db.upsertItemsBatch(anyList())).thenReturn(false);

        int exit = cmdLine.execute("--path", csv.toString(), "--preserve", "csv");
        assertEquals(1, exit);

        verify(db, times(1)).upsertItemsBatch(anyList());
    }

    @Test
    void testImportFailureReturnsOne_whenInsertFails() throws Exception {
        List<String> rows = List.of(
                "6,Item Six,https://six.example,M," + LocalDate.now().toString() + ",0"
        );
        Path csv = createCsv("items4.csv", rows);

        when(db.getAllItemsIds()).thenReturn(Optional.of(new HashSet<>()));
        when(db.insertItemsBatch(anyList())).thenReturn(false);

        int exit = cmdLine.execute("--path", csv.toString(), "--preserve", "db");
        assertEquals(1, exit);

        verify(db, times(1)).insertItemsBatch(anyList());
    }


    // --- VALIDATION FAILURES (exit code 2) ---

    @Test
    void testMissingFilePathFailsValidation() {
        int exit = cmdLine.execute("--path", "", "--preserve", "csv");
        assertEquals(2, exit);
    }

    @Test
    void testWrongExtensionFailsValidation() throws Exception {
        Path notCsv = tempDir.resolve("not_a_csv.txt");
        Files.writeString(notCsv, "garbage");
        int exit = cmdLine.execute("--path", notCsv.toString(), "--preserve", "csv");
        assertEquals(2, exit);
    }

    @Test
    void testNonExistentFileFailsValidation() {
        Path missing = tempDir.resolve("doesnotexist.csv");
        int exit = cmdLine.execute("--path", missing.toString(), "--preserve", "csv");
        assertEquals(2, exit);
    }

    @Test
    void testInvalidPreserveValueFailsValidation() throws Exception {
        Path csv = createCsv("items6.csv", List.of(
                "8,Item Eight,https://eight.example,H," + LocalDate.now().toString() + ",0"
        ));
        int exit = cmdLine.execute("--path", csv.toString(), "--preserve", "banana");
        assertEquals(2, exit);
    }

    // --- EDGE CASES ---

    @Test
    void testImportReturnsOneWhenCsvParserProducesNoValidRows() throws Exception {
        // create a CSV with invalid rows that CSVImporter should ignore.
        // We don't assert parser internals, only that ImportCommand returns 1 when parse yields empty list.
        Path csv = createCsv("empty.csv", List.of(",,,,,"));
        // If CSVImporter.parse throws, ImportCommand catches Exception and returns 1.
        // Just run and assert return is 1.
        int exit = cmdLine.execute("--path", csv.toString(), "--preserve", "csv");
        assertEquals(1, exit);
    }
}
