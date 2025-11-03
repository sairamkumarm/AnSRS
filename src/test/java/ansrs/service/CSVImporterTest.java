package ansrs.service;

import ansrs.data.Item;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVImporterTest {

    @Test
    void testValidSingleRowImport() {
        String csv = "1,Item A,https://a.com,H,2024-01-01,3";
        CSVImporter importer = new CSVImporter(new StringReader(csv));

        List<Item> items = importer.parse();
        assertEquals(1, items.size());
        Item i = items.get(0);
        assertEquals(1, i.getItemId());
        assertEquals("Item A", i.getItemName());
        assertEquals(Item.Pool.H, i.getItemPool());
        assertEquals(LocalDate.parse("2024-01-01"), i.getLastRecall());
        assertEquals(3, i.getTotalRecalls());
    }

    @Test
    void testSkipsInvalidRows() {
        String csv = """
            ITEM_ID,ITEM_NAME,ITEM_LINK,ITEM_POOL,ITEM_LAST_RECALL,totalRecalls
            1,Valid,https://ok.com,M,2024-01-01,2
            -1,Bad,https://x.com,H,2024-01-01,3
            """;
        CSVImporter importer = new CSVImporter(new StringReader(csv));

        List<Item> items = importer.parse();
        assertEquals(1, items.size());
        assertEquals("Valid", items.get(0).getItemName());
    }

    @Test
    void testMalformedCSVThrows() {
        String csv = "1,OnlyOneField";
        CSVImporter importer = new CSVImporter(new StringReader(csv));

        assertThrows(RuntimeException.class, importer::parse);
    }

    @Test
    void testFutureDateIsIgnored() {
        String future = LocalDate.now().plusDays(5).toString();
        String csv = "5,FutureItem,https://x.com,L," + future + ",0";
        CSVImporter importer = new CSVImporter(new StringReader(csv));

        List<Item> items = importer.parse();
        assertTrue(items.isEmpty());
    }

    @Test
    void testHeaderIsIgnoredProperly() {
        String csv = """
            ITEM_ID,ITEM_NAME,ITEM_LINK,ITEM_POOL,ITEM_LAST_RECALL,totalRecalls
            2,HeaderTest,https://h.com,H,2024-01-01,1
            """;
        CSVImporter importer = new CSVImporter(new StringReader(csv));

        List<Item> items = importer.parse();
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getItemId());
    }
}
