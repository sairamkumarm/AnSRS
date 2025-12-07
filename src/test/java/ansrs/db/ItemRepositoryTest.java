package ansrs.db;

import ansrs.data.Item;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemRepositoryTest {

    static ItemRepository db;
    static Item baseItem;

    @BeforeAll
    static void setup() {
        Connection conn = DatabaseInitialiser.initInMemoryDb("testdb");
        db = new ItemRepository(conn);
        baseItem = new Item(1, "Item1", "https://a.com", Item.Pool.H, LocalDate.now(), 2);
    }

    @AfterEach
    void clear() {
        db.clearItems();
    }

    @AfterAll
    static void teardown() throws Exception {
        db.close();
    }

    @Test
    void testInsertAndGetItemById() {
        assertTrue(db.insertItem(baseItem));
        Optional<Item> fetched = db.getItemById(1);
        assertTrue(fetched.isPresent());
        assertEquals("Item1", fetched.get().getItemName());
    }

    @Test
    void testInsertItemsBatch() {
        List<Item> items = List.of(
                baseItem,
                new Item(2, "Two", "https://b.com", Item.Pool.M, LocalDate.now(), 3)
        );
        assertTrue(db.insertItemsBatch(items));
        List<Item> dbItems = db.getAllItems().orElse(Collections.emptyList());
        assertEquals(2, dbItems.size());
    }

    @Test
    void testUpsertItemsBatch() {
        List<Item> items = List.of(baseItem);
        db.insertItemsBatch(items);

        List<Item> updated = List.of(
                new Item(1, "Updated", "https://a.com", Item.Pool.H, LocalDate.now(), 4)
        );
        assertTrue(db.upsertItemsBatch(updated));

        Item fetched = db.getItemById(1).get();
        assertEquals("Updated", fetched.getItemName());
        assertEquals(4, fetched.getTotalRecalls());
    }

    @Test
    void testUpdateItemsBatch() {
        db.insertItem(baseItem);
        List<Item> updates = List.of(
                new Item(1, "Mod", "https://a.com", Item.Pool.H, LocalDate.now(), 9)
        );
        assertTrue(db.updateItemsBatch(updates));

        Item fetched = db.getItemById(1).get();
        assertEquals("Mod", fetched.getItemName());
        assertEquals(9, fetched.getTotalRecalls());
    }

    @Test
    void testDeleteAndExists() {
        db.insertItem(baseItem);
        assertTrue(db.exists(1));
        assertTrue(db.deleteItemsById(1));
        assertFalse(db.exists(1));
    }

    @Test
    void testGetAllItemsIds() {
        db.insertItem(baseItem);
        db.insertItem(new Item(2, "Second", "https://b.com", Item.Pool.L, LocalDate.now(), 0));
        Optional<HashSet<Integer>> ids = db.getAllItemsIds();
        assertTrue(ids.isPresent());
        assertTrue(ids.get().containsAll(List.of(1, 2)));
    }

    @Test
    void testGetItemsFromList() {
        db.insertItem(baseItem);
        db.insertItem(new Item(2, "Other", "https://b.com", Item.Pool.M, LocalDate.now(), 1));
        Optional<List<Item>> list = db.getItemsFromList(List.of(1, 2));
        assertTrue(list.isPresent());
        assertEquals(2, list.get().size());
    }

    @Test
    void testSearchItemsByName() {
        Item i1 = new Item(1, "AlphaWidget", "https://a.com", Item.Pool.H, LocalDate.now(), 2);
        Item i2 = new Item(2, "BetaTool", "https://b.com", Item.Pool.M, LocalDate.now(), 1);
        Item i3 = new Item(3, "GammaWidget", "https://c.com", Item.Pool.L, LocalDate.now(), 0);
        db.insertItemsBatch(List.of(i1, i2, i3));

        Optional<List<Item>> result1 = db.searchItemsByName("widget");
        assertTrue(result1.isPresent());
        List<Item> matches = result1.get();
        assertEquals(2, matches.size());
        assertTrue(matches.stream().anyMatch(i -> i.getItemName().equals("AlphaWidget")));
        assertTrue(matches.stream().anyMatch(i -> i.getItemName().equals("GammaWidget")));

        Optional<List<Item>> result2 = db.searchItemsByName("beta");
        assertTrue(result2.isPresent());
        assertEquals(1, result2.get().size());
        assertEquals("BetaTool", result2.get().get(0).getItemName());

        Optional<List<Item>> result3 = db.searchItemsByName("nonexistent");
        assertTrue(result3.isPresent());
        assertTrue(result3.get().isEmpty());
    }


    @Test
    void testClearItems() {
        db.insertItem(baseItem);
        assertTrue(db.clearItems());
        assertEquals(0, (db.getAllItems().orElse(new ArrayList<>())).size());
    }
}
