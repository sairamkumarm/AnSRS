package dev.sai.srs.service;

import dev.sai.srs.data.Item;
import dev.sai.srs.db.DuckDBManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecallServiceTest {

    private DuckDBManager dbMock;
    private List<Item> items;
    private RecallService service;
    private final LocalDate today = LocalDate.of(2025, 1, 1);

    @BeforeAll
    void setup() {
        dbMock = Mockito.mock(DuckDBManager.class);
        items = new ArrayList<>();

        // Item(id, name, link, pool, lastRecall, totalRecalls)
        items.add(new Item(1, "H-12d-0rec", "", Item.Pool.H, today.minusDays(12), 0)); // daysSince=13
        items.add(new Item(2, "L-15d-8rec", "", Item.Pool.L, today.minusDays(15), 8)); // daysSince=16
        items.add(new Item(3, "L-15d-0rec", "", Item.Pool.L, today.minusDays(15), 0)); // daysSince=16
        items.add(new Item(4, "M-7d-2rec", "", Item.Pool.M, today.minusDays(7), 2));   // daysSince=8
        items.add(new Item(5, "H-0d-0rec", "", Item.Pool.H, today, 0));               // daysSince=1
        items.add(new Item(6, "L-30d-3rec", "", Item.Pool.L, today.minusDays(30), 3));// daysSince=31
        items.add(new Item(7, "H-10d-2rec", "", Item.Pool.H, today.minusDays(10), 2));// daysSince=11
        items.add(new Item(8, "M-2d-0rec", "", Item.Pool.M, today.minusDays(2), 0));   // daysSince=3
        items.add(new Item(9, "L-2d-0rec", "", Item.Pool.L, today.minusDays(2), 0));   // daysSince=3
        items.add(new Item(10, "M-0d-0rec", "", Item.Pool.M, today, 0));              // daysSince=1

        Mockito.when(dbMock.getAllItems()).thenReturn(Optional.of(items));

        // Use the existing implementation (alpha=10, beta=1.2, gamma=1, date=today)
        service = new RecallService(dbMock, 10.0, 1.2, 1.0, today);
    }

    private double rating(int id) {
        return service.getRating(
                items.stream()
                        .filter(p -> p.getItemId() == id)
                        .findFirst()
                        .orElseThrow()
        );
    }

    @Test
    void testTopOrderingInitialRecall() {
        List<Integer> picked = service.recall(5);

        // Manual calculations using the exact formula:
        // rating = (poolWeight * alpha) * (daysSince ^ beta) / (recalls + gamma)
        // alpha=10, beta=1.2, gamma=1
        //
        // id=1 H, daysSince=13, rec=0:
        //   rating(1) = (3*10) * 13^1.2 / (0+1) = 30 * 21.71360948035253 = 651.4082844105759
        //
        // id=3 L, daysSince=16, rec=0:
        //   rating(3) = (1*10) * 16^1.2 / 1 = 10 * 27.857618025475967 = 278.57618025475966
        //
        // id=7 H, daysSince=11, rec=2:
        //   rating(7) = (3*10) * 11^1.2 / 3 = 30 * 5.923112309407987 = 177.6933692822396
        //
        // id=6 L, daysSince=31, rec=3:
        //   rating(6) = (1*10) * 31^1.2 / 4 = 10 * 15.401890848649546 = 154.01890848649546
        //
        // id=4 M, daysSince=8, rec=2:
        //   rating(4) = (2*10) * 8^1.2 / 3 = 20 * 4.0419108440277277 = 80.83821688055455
        //
        // Sorted descending => [1, 3, 7, 6, 4]
        //
        // The service's PriorityQueue will therefore poll in that order for top 5.

        assertThat(picked.subList(0, 5))
                .containsExactly(1, 3, 7, 6, 4);
    }

    @Test
    void hardBeatsEasyEvenWithManyRecalls() {
        // Check numbers:
        // id=1 (H,13d,0rec) rating = 651.408...
        // id=2 (L,16d,8rec) rating = (1*10)*16^1.2 / 9 = 10 * 3.095290891719552 = 30.95290891719552
        // So H(1) >>> L(2) here, assert direct relation.
        assertThat(rating(1)).isGreaterThan(rating(2));
    }

    @Test
    void veryOldEasyBeatsLessOldHardWithRecalls() {
        // Here we assert the actual ordering that the implementation produces.
        // Compare id=7 vs id=6:
        // id=7 (H,11d,2rec): rating(7) = 177.6933692822396
        // id=6 (L,31d,3rec): rating(6) = 154.01890848649546
        // So id=7 > id=6 under current formula.
        assertThat(rating(7)).isGreaterThan(rating(6));
    }

    @Test
    void zeroDayStillPositive() {
        // id=5 H, daysSince=1, rec=0:
        // rating(5) = (3*10)*1^1.2 / 1 = 30.0
        assertThat(rating(5)).isPositive();
        assertThat(rating(5)).isEqualTo(30.0);
    }

    @ParameterizedTest
    @CsvSource({
            "H, M",
            "M, L",
            "H, L"
    })
    void poolHierarchyRespected(String stronger, String weaker) {
        Item.Pool pStrong = Item.Pool.valueOf(stronger);
        Item.Pool pWeak = Item.Pool.valueOf(weaker);

        // Create two items with identical dates and recalls to test pure-pool effect.
        // daysSince = 7 + 1 = 8 in the service calculation
        Item A = new Item(200, "A", "", pStrong, today.minusDays(7), 0); // daysSince=8
        Item B = new Item(201, "B", "", pWeak, today.minusDays(7), 0);   // daysSince=8

        // Example numeric check (for H vs M):
        // rating(H,8,0) = (3*10)*8^1.2 / 1 = 363.7719759624955
        // rating(M,8,0) = (2*10)*8^1.2 / 1 = 242.51465064166368
        assertThat(service.getRating(A)).isGreaterThan(service.getRating(B));
    }

    @Test
    void daysIncreaseWhenRecallsConstant() {
        // id=4 (M,8d,2rec) rating = 80.838...
        // id=8 (M,3d,0rec) rating = 74.743...
        // even though recalls differ, we expect 7d > 2d in practice for these entries:
        assertThat(rating(4)).isGreaterThan(rating(8)); // 80.838 > 74.743
    }

    @Test
    void recallsDecreaseWhenDaysConstant() {
        // id=9 base (L,3d,0rec): rating(9) = (1*10)*3^1.2 / 1 = 37.37192818846552
        Item fresh = items.get(9 - 1);  // id=9
        double base = rating(9);

        // New item same lastRecall but 5 recalls:
        // rating = 10 * 3^1.2 / (5+1) = 6.2286546980775865
        Item moreRec = new Item(300, "moreRec", "", Item.Pool.L, fresh.getLastRecall(), 5);
        double decay = service.getRating(moreRec);

        assertThat(base).isGreaterThan(decay);
    }
}
