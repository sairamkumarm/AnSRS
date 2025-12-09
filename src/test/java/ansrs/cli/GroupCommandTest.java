package ansrs.cli;

import ansrs.data.Group;
import ansrs.data.Item;
import ansrs.db.GroupRepository;
import ansrs.db.ItemRepository;
import ansrs.set.WorkingSet;
import ansrs.util.Printer;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupCommandTest {

    private GroupRepository gr;
    private ItemRepository ir;
    private SRSCommand parent;
    private GroupCommand cmd;
    private CommandLine cmdLine;
    private WorkingSet ws;

    @BeforeEach
    void setup() {
        gr = mock(GroupRepository.class);
        ir = mock(ItemRepository.class);
        ws = mock(WorkingSet.class);
        parent = new SRSCommand(ws, null, ir, null, gr);
        cmd = new GroupCommand();
        cmdLine = new CommandLine(cmd);
        cmd.parent = parent;
    }

    // ---------- CREATE ----------

    @Test
    void testCreateSuccess() {
        when(gr.exists(1)).thenReturn(false);
        when(gr.createGroup(1, "Test", "https://link.com")).thenReturn(true);

        assertEquals(0, cmdLine.execute("--create", "--id", "1", "--name", "Test", "--link", "https://link.com"));
        verify(gr).createGroup(1, "Test", "https://link.com");
    }

    @Test
    void testCreateFailsIfExists() {
        when(gr.exists(1)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--create", "--id", "1", "--name", "Test"));
    }

    @Test
    void testCreateMissingNameFails() {
        when(gr.exists(1)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--create", "--id", "1"));
    }

    // ---------- UPDATE ----------

    @Test
    void testUpdateNameSuccess() {
        when(gr.exists(2)).thenReturn(true);
        when(gr.updateGroup(2, "New", null)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--update", "--id", "2", "--name", "New"));
        verify(gr).updateGroup(2, "New", null);
    }

    @Test
    void testUpdateFailsWithoutFields() {
        when(gr.exists(2)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--update", "--id", "2"));
    }

    // ---------- DELETE ----------

    @Test
    void testDeleteSuccess() {
        when(gr.exists(3)).thenReturn(true);
        when(gr.deleteGroupById(3)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--delete", "--id", "3"));
        verify(gr).deleteGroupById(3);
    }

    // ---------- SHOW ----------

    @Test
    void testShowSuccess() {
        when(gr.exists(4)).thenReturn(true);
        when(gr.findById(4)).thenReturn(Optional.of(new Group(4, "G", null)));

        assertEquals(0, cmdLine.execute("--show", "--id", "4"));
    }

    @Test
    void testShowAllSuccess() {
        when(gr.findAll()).thenReturn(Optional.of(List.of(new Group(4, "G", null), new Group(5, "Group link", "https://group.com"), new Group(6, "G name", null))));

        assertEquals(0, cmdLine.execute("--show-all"));
    }

    @Test
    void testShowFailsIfMissing() {
        when(gr.exists(4)).thenReturn(true);
        when(gr.findById(4)).thenReturn(Optional.empty());

        assertEquals(2, cmdLine.execute("--show", "--id", "4"));
    }

    // ---------- SHOW ITEMS ----------

    @Test
    void testShowItemsEmpty() {
        when(gr.exists(5)).thenReturn(true);
        when(gr.getItemIdsForGroup(5)).thenReturn(List.of());

        assertEquals(0, cmdLine.execute("--show-items", "--id", "5"));
    }

    @Test
    void testShowItemsSuccess() {
        when(gr.exists(6)).thenReturn(true);
        when(gr.getItemIdsForGroup(6)).thenReturn(List.of(10, 20));
        when(ir.getItemsFromList(anyList())).thenReturn(Optional.of(List.of(
                new Item(10, "A", "x", Item.Pool.M, null, 0),
                new Item(20, "B", "x", Item.Pool.L, null, 0)
        )));

        assertEquals(0, cmdLine.execute("--show-items", "--id", "6"));
    }

    // ---------- ADD ITEM ----------

    @Test
    void testAddItemSuccess() {
        when(gr.exists(7)).thenReturn(true);
        when(ir.exists(100)).thenReturn(true);
        when(gr.itemExistsInGroup(7, 100)).thenReturn(false);
        when(gr.addItemToGroup(7, 100)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--id", "7", "--add-item", "100"));
        verify(gr).addItemToGroup(7, 100);
    }

    @Test
    void testAddItemFailsIfAlreadyExists() {
        when(gr.exists(7)).thenReturn(true);
        when(ir.exists(100)).thenReturn(true);
        when(gr.itemExistsInGroup(7, 100)).thenReturn(true);

        assertEquals(2, cmdLine.execute("--id", "7", "--add-item", "100"));
    }

    // ---------- ADD BATCH ----------

    @Test
    void testAddBatchSuccess() {
        when(gr.exists(8)).thenReturn(true);
        when(ir.exists(anyInt())).thenReturn(true);
        when(gr.addItemsToGroupBatch(eq(8), anyList())).thenReturn(true);

        assertEquals(0, cmdLine.execute("--id", "8", "--add-batch", "1,2,3"));
        verify(gr).addItemsToGroupBatch(eq(8), anyList());
    }

    @Test
    void testAddBatchFailsOnInvalidItem() {
        when(gr.exists(8)).thenReturn(true);
        when(ir.exists(1)).thenReturn(true);
        when(ir.exists(2)).thenReturn(false);

        assertEquals(2, cmdLine.execute("--id", "8", "--add-batch", "1,2"));
    }

    @Test
    void testAddBatchWithDuplicates() {
        when(gr.exists(8)).thenReturn(true);
        when(ir.exists(anyInt())).thenReturn(true);

        // Simulate duplicate check
        when(gr.itemExistsInGroup(eq(8), eq(2))).thenReturn(true); // item 2 is already in group
        when(gr.itemExistsInGroup(eq(8), eq(1))).thenReturn(false);
        when(gr.itemExistsInGroup(eq(8), eq(3))).thenReturn(false);

        when(gr.addItemsToGroupBatch(eq(8), argThat(list -> list.size() == 2 && list.contains(1) && list.contains(3))))
                .thenReturn(true);

        int result = cmdLine.execute("--id", "8", "--add-batch", "1,2,3");
        assertEquals(0, result);

        // verify batch add called with duplicates removed
        verify(gr).addItemsToGroupBatch(eq(8), argThat(list -> list.size() == 2 && list.contains(1) && list.contains(3)));
    }
    // ---------- REMOVE ITEM ----------

    @Test
    void testRemoveItemSuccess() {
        when(gr.exists(9)).thenReturn(true);
        when(ir.exists(55)).thenReturn(true);
        when(gr.itemExistsInGroup(9, 55)).thenReturn(true);
        when(gr.removeItemFromGroup(9, 55)).thenReturn(true);

        assertEquals(0, cmdLine.execute("--id", "9", "--remove-item", "55"));
        verify(gr).removeItemFromGroup(9, 55);
    }

    @Test
    void testRemoveItemFailsIfItemMissing() {
        when(gr.exists(9)).thenReturn(true);
        when(ir.exists(55)).thenReturn(false);

        assertEquals(2, cmdLine.execute("--id", "9", "--remove-item", "55"));
    }

    @Test
    void testRemoveItemFailsIfItemMissingFromGroup() {
        when(gr.exists(9)).thenReturn(true);
        when(ir.exists(55)).thenReturn(true);
        when(gr.itemExistsInGroup(9,55)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--id", "9", "--remove-item", "55"));
    }

    // ---------- RECALL GROUP ----------
    @Test
    void testRecallOverwriteSuccess() {
        when(gr.exists(10)).thenReturn(true);
        when(gr.getItemIdsForGroup(10)).thenReturn(List.of(1, 2, 3));

        when(ws.clearSet()).thenReturn(true);

        int rc = cmdLine.execute("--id", "10", "--recall", "overwrite");
        assertEquals(0, rc);

        verify(ws).clearSet();
        verify(ws).fillSet(List.of(1, 2, 3));
    }

    @Test
    void testRecallAppendSuccess() {
        when(gr.exists(11)).thenReturn(true);
        when(gr.getItemIdsForGroup(11)).thenReturn(List.of(5, 6));

        int rc = cmdLine.execute("--id", "11", "--recall", "append");
        assertEquals(0, rc);

        verify(ws, never()).clearSet();
        verify(ws).fillSet(List.of(5, 6));
    }

    @Test
    void testRecallInvalidModeFails() {
        when(gr.exists(12)).thenReturn(true);

        assertEquals(2, cmdLine.execute("--id", "12", "--recall", "badmode"));
    }

    @Test
    void testRecallOverwriteClearFails() {
        when(gr.exists(13)).thenReturn(true);
        when(gr.getItemIdsForGroup(13)).thenReturn(List.of(9));

        when(ws.clearSet()).thenReturn(false);

        int rc = cmdLine.execute("--id", "13", "--recall", "overwrite");
        assertEquals(1, rc);

        verify(ws, never()).fillSet(List.of(9));
    }

    @Test
    void testRecallEmptyGroupWarns() {
        when(gr.exists(13)).thenReturn(true);
        when(gr.getItemIdsForGroup(13)).thenReturn(Collections.emptyList());

        int rc = cmdLine.execute("--id", "13", "--recall", "overwrite");
        assertEquals(0, rc);

        verify(ws, never()).fillSet(Collections.emptyList());
    }


    // ---------- GENERIC VALIDATION ----------

    @Test
    void testMultipleOpsRejected() {
        when(gr.exists(1)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--id", "1", "--show", "--delete"));
    }

    @Test
    void testMissingIdRejectedForNonCreate() {
        assertEquals(2, cmdLine.execute("--delete"));
    }

    @Test
    void testInvalidURLFormat() {
        assertEquals(2, cmdLine.execute("--create", "--id", "1", "--name", "Test", "--link", "http://link.com"));
    }

    @Test
    void testNonExistentGroupRejected() {
        when(gr.exists(999)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--delete", "--id", "999"));
    }

    @Test
    void testMissingIdFailsForNonCreate() {
        assertEquals(2, cmdLine.execute("--show"));
    }

    @Test
    void testMultipleOpsFails() {
        assertEquals(2, cmdLine.execute("--show", "--delete", "--id", "3"));
    }

    @Test
    void testNoOperationSpecified() {
        when(gr.exists(4)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--id", "4"));
    }

    @Test
    void testUpdateInvalidLinkFails() {
        when(gr.exists(2)).thenReturn(true);
        assertEquals(2, cmdLine.execute("--update", "--id", "2", "--link", "http://bad"));
    }

    @Test
    void testShowItemsFailsIfGroupMissing() {
        when(gr.exists(99)).thenReturn(false);
        assertEquals(2, cmdLine.execute("--show-items", "--id", "99"));
    }

}
