package ansrs.db;

import ansrs.data.Group;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GroupRepositoryTest {

    static GroupRepository groupRepository;

    @BeforeAll
    static void setup() {
        Connection conn = DatabaseInitialiser.initInMemoryDb("testdb");
        groupRepository = new GroupRepository(conn);
    }

    @AfterEach
    void clear() {
        // brute clear since we don't yet have a truncate method on lists
        groupRepository.deleteGroupById(1);
        groupRepository.deleteGroupById(2);
        groupRepository.deleteGroupById(3);
    }

    @AfterAll
    static void teardown() throws Exception {
        groupRepository.close();
    }

    @Test
    void testCreateAndFindById() {
        assertTrue(groupRepository.createGroup(1, "List One", "https://list.one"));

        Optional<Group> fetched = groupRepository.findById(1);
        assertTrue(fetched.isPresent());
        assertEquals("List One", fetched.get().name);
        assertEquals("https://list.one", fetched.get().link);
    }

    @Test
    void testExists() {
        assertFalse(groupRepository.exists(1));
        groupRepository.createGroup(1, "List One", "https://list.one");
        assertTrue(groupRepository.exists(1));
    }

    @Test
    void testUpdateGroup() {
        groupRepository.createGroup(1, "Old Name", "https://old.link");

        assertTrue(groupRepository.updateGroup(1, "New Name", "https://new.link"));

        assertTrue(groupRepository.findById(1).isPresent());
        Group updated = groupRepository.findById(1).get();
        assertEquals("New Name", updated.name);
        assertEquals("https://new.link", updated.link);
    }

    @Test
    void testDeleteGroupById() {
        groupRepository.createGroup(1, "Temp", "https://tmp");

        assertTrue(groupRepository.exists(1));
        assertTrue(groupRepository.deleteGroupById(1));
        assertFalse(groupRepository.exists(1));
    }

    @Test
    void testFindAll() {
        groupRepository.createGroup(1, "List A", "https://a");
        groupRepository.createGroup(2, "List B", "https://b");

        Optional<List<Group>> lists = groupRepository.findAll();
        assertTrue(lists.isPresent());
        assertEquals(2, lists.get().size());

        assertTrue(
                lists.get().stream().anyMatch(l -> l.id == 1 && l.name.equals("List A"))
        );
        assertTrue(
                lists.get().stream().anyMatch(l -> l.id == 2 && l.name.equals("List B"))
        );
    }

    @Test
    void testDuplicatePrimaryKeyIsRejected() {
        assertTrue(groupRepository.createGroup(2, "List A", "https://a"));
        assertFalse(groupRepository.createGroup(2, "List A Duplicate", "https://a2"));
    }

    @Test
    void testFindByIdReturnsEmptyForMissing() {
        Optional<Group> fetched = groupRepository.findById(999);
        assertTrue(fetched.isEmpty());
    }
}
