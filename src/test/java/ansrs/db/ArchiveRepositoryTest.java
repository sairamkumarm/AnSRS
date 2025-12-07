package ansrs.db;

import ansrs.data.Item;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveRepositoryTest {

    static ArchiveRepository am;
    static Item baseItem;

    @BeforeAll
    static void setup() {
        Connection connection = DatabaseInitialiser.initInMemoryDb("testdb");
        am = new ArchiveRepository(connection);
        baseItem = new Item(1, "Item1", "https://a.com", Item.Pool.H, LocalDate.now(), 2);
    }

    @AfterEach
    void clear() {
        am.clearDatabase();
    }

    @AfterAll
    static void teardown() throws Exception {
        am.close();
    }

    @Test
    void testInsertAndGetItemById() {
        assertTrue(am.insertItem(baseItem));
        Optional<Item> fetched = am.getItemById(1);
        assertTrue(fetched.isPresent());
        assertEquals("Item1", fetched.get().getItemName());
    }

    @Test
    void testInsertItemsBatch() {
        List<Item> items = List.of(
                baseItem,
                new Item(2, "Two", "https://b.com", Item.Pool.M, LocalDate.now(), 3)
        );
        assertTrue(am.insertItemsBatch(items));
        List<Item> amItems = am.getAllItems().orElse(Collections.emptyList());
        assertEquals(2, amItems.size());
    }

    @Test
    void testUpsertItemsBatch() {
        List<Item> items = List.of(baseItem);
        am.insertItemsBatch(items);

        List<Item> updated = List.of(
                new Item(1, "Updated", "https://a.com", Item.Pool.H, LocalDate.now(), 4)
        );
        assertTrue(am.upsertItemsBatch(updated));

        Item fetched = am.getItemById(1).get();
        assertEquals("Updated", fetched.getItemName());
        assertEquals(4, fetched.getTotalRecalls());
    }

    @Test
    void testUpdateItemsBatch() {
        am.insertItem(baseItem);
        List<Item> updates = List.of(
                new Item(1, "Mod", "https://a.com", Item.Pool.H, LocalDate.now(), 9)
        );
        assertTrue(am.updateItemsBatch(updates));

        Item fetched = am.getItemById(1).get();
        assertEquals("Mod", fetched.getItemName());
        assertEquals(9, fetched.getTotalRecalls());
    }

    @Test
    void testDeleteAndContains() {
        am.insertItem(baseItem);
        assertTrue(am.contains(1));
        assertTrue(am.deleteItemsById(1));
        assertFalse(am.contains(1));
    }

    @Test
    void testGetAllItemsIds() {
        am.insertItem(baseItem);
        am.insertItem(new Item(2, "Second", "https://b.com", Item.Pool.L, LocalDate.now(), 0));
        Optional<HashSet<Integer>> ids = am.getAllItemsIds();
        assertTrue(ids.isPresent());
        assertTrue(ids.get().containsAll(List.of(1, 2)));
    }

    @Test
    void testGetItemsFromList() {
        am.insertItem(baseItem);
        am.insertItem(new Item(2, "Other", "https://b.com", Item.Pool.M, LocalDate.now(), 1));
        Optional<List<Item>> list = am.getItemsFromList(List.of(1, 2));
        assertTrue(list.isPresent());
        assertEquals(2, list.get().size());
    }

    @Test
    void testSearchItemsByName() {
        Item i1 = new Item(1, "AlphaWidget", "https://a.com", Item.Pool.H, LocalDate.now(), 2);
        Item i2 = new Item(2, "BetaTool", "https://b.com", Item.Pool.M, LocalDate.now(), 1);
        Item i3 = new Item(3, "GammaWidget", "https://c.com", Item.Pool.L, LocalDate.now(), 0);
        am.insertItemsBatch(List.of(i1, i2, i3));

        Optional<List<Item>> result1 = am.searchItemsByName("widget");
        assertTrue(result1.isPresent());
        List<Item> matches = result1.get();
        assertEquals(2, matches.size());
        assertTrue(matches.stream().anyMatch(i -> i.getItemName().equals("AlphaWidget")));
        assertTrue(matches.stream().anyMatch(i -> i.getItemName().equals("GammaWidget")));

        Optional<List<Item>> result2 = am.searchItemsByName("beta");
        assertTrue(result2.isPresent());
        assertEquals(1, result2.get().size());
        assertEquals("BetaTool", result2.get().get(0).getItemName());

        Optional<List<Item>> result3 = am.searchItemsByName("nonexistent");
        assertTrue(result3.isPresent());
        assertTrue(result3.get().isEmpty());
    }


    @Test
    void testClearDatabase() {
        am.insertItem(baseItem);
        assertTrue(am.clearDatabase());
        assertEquals(0, am.getAllItems().get().size());
    }
}
