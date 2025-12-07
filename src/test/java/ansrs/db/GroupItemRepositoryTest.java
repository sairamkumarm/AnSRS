package ansrs.db;

import ansrs.data.Item;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupItemRepositoryTest {

    static ItemRepository itemRepository;
    static GroupRepository groupRepository;
    static GroupItemRepository groupItemRepository;

    static Item baseItem1;
    static Item baseItem2;

    @BeforeAll
    static void setup() {
        Connection conn = DatabaseInitialiser.initInMemoryDb("testdb");

        itemRepository = new ItemRepository(conn);
        groupRepository = new GroupRepository(conn);
        groupItemRepository = new GroupItemRepository(conn);

        baseItem1 = new Item(1, "Item1", "https://a.com", Item.Pool.H, LocalDate.now(), 2);
        baseItem2 = new Item(2, "Item2", "https://b.com", Item.Pool.M, LocalDate.now(), 1);

        itemRepository.insertItemsBatch(List.of(baseItem1, baseItem2));

        groupRepository.createGroup(100, "ListA", "https://list.a");
        groupRepository.createGroup(200, "ListB", "https://list.b");
    }

    @AfterEach
    void clear() {
        groupItemRepository.removeAllItemsFromGroup(100);
        groupItemRepository.removeAllItemsFromGroup(200);
    }

    @AfterAll
    static void teardown() throws Exception {
        groupItemRepository.close();
        groupRepository.close();
        itemRepository.close();
    }

    @Test
    void testAddItemToGroupAndExists() {
        assertTrue(groupItemRepository.addItemToGroup(100, 1));
        assertTrue(groupItemRepository.exists(100, 1));
        assertFalse(groupItemRepository.exists(100, 2));
    }

    @Test
    void testAddItemsToGroupBatch() {
        assertTrue(groupItemRepository.addItemsToGroupBatch(100, List.of(1, 2)));

        List<Integer> ids = groupItemRepository.getItemIdsForGroup(100);
        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(List.of(1, 2)));
    }

    @Test
    void testRemoveItemFromGroup() {
        groupItemRepository.addItemsToGroupBatch(100, List.of(1, 2));

        assertTrue(groupItemRepository.removeItemFromGroup(100, 1));

        List<Integer> remaining = groupItemRepository.getItemIdsForGroup(100);
        assertEquals(1, remaining.size());
        assertTrue(remaining.contains(2));
    }

    @Test
    void testRemoveAllItemsFromGroup() {
        groupItemRepository.addItemsToGroupBatch(100, List.of(1, 2));

        assertTrue(groupItemRepository.removeAllItemsFromGroup(100));

        List<Integer> remaining = groupItemRepository.getItemIdsForGroup(100);
        assertTrue(remaining.isEmpty());
    }

    @Test
    void testGetItemIdsForGroup() {
        groupItemRepository.addItemToGroup(100, 1);
        groupItemRepository.addItemToGroup(100, 2);

        List<Integer> ids = groupItemRepository.getItemIdsForGroup(100);
        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(List.of(1, 2)));
    }

    @Test
    void testGetGroupIdsForItem() {
        groupItemRepository.addItemToGroup(100, 1);
        groupItemRepository.addItemToGroup(200, 1);

        List<Integer> lists = groupItemRepository.getGroupIdsForItem(1);
        assertEquals(2, lists.size());
        assertTrue(lists.containsAll(List.of(100, 200)));
    }

    @Test
    void testDuplicateInsertIsRejectedByPrimaryKey() {
        assertTrue(groupItemRepository.addItemToGroup(100, 1));
        assertFalse(groupItemRepository.addItemToGroup(100, 1));
    }
}
