package ansrs.db;

import ansrs.data.Item;
import org.junit.jupiter.api.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DBManagerTest {

    static DBManager db;
    static Item baseItem;

    @BeforeAll
    static void setup() {
        try {
            db = new DBManager("mem:testdb");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        baseItem = new Item(1, "Item1", "https://a.com", Item.Pool.H, LocalDate.now(), 2);
    }

    @AfterEach
    void clear() {
        db.clearDatabase();
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
        assertEquals(2, db.getAllItems().get().size());
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
    void testDeleteAndContains() {
        db.insertItem(baseItem);
        assertTrue(db.contains(1));
        assertTrue(db.deleteItemsById(1));
        assertFalse(db.contains(1));
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
    void testClearDatabase() {
        db.insertItem(baseItem);
        assertTrue(db.clearDatabase());
        assertEquals(0, db.getAllItems().get().size());
    }
}
