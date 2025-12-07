package ansrs.db;

import ansrs.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GroupItemRepository implements AutoCloseable {

    private final Connection connection;

    public GroupItemRepository(Connection connection) {
        this.connection = connection;
    }

    public boolean addItemToGroup(int groupId, int itemId) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO group_items (group_id, item_id)
                VALUES (?, ?)
        """)) {
            ps.setInt(1, groupId);
            ps.setInt(2, itemId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), "23505"))
                Log.error("Duplicate ITEM_ID["+itemId+"] found in GROUP_ID["+groupId+"]");
            else Log.error("SQL ERROR: "+ e.getMessage());
            return false;
        }
    }

    public boolean addItemsToGroupBatch(int listId, List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return true;

        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO group_items (group_id, item_id)
                VALUES (?, ?)
        """)) {
            connection.setAutoCommit(false);

            for (Integer itemId : itemIds) {
                ps.setInt(1, listId);
                ps.setInt(2, itemId);
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignore) {
            }
            Log.error(e.getMessage());
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignore) {
            }
        }
    }

    public boolean removeItemFromGroup(int listId, int itemId) {
        try (PreparedStatement ps = connection.prepareStatement("""
                DELETE FROM group_items
                WHERE group_id=? AND item_id=?
        """)) {
            ps.setInt(1, listId);
            ps.setInt(2, itemId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return false;
        }
    }

    public boolean removeAllItemsFromGroup(int listId) {
        try (PreparedStatement ps = connection.prepareStatement("""
                DELETE FROM group_items WHERE group_id=?
        """)) {
            ps.setInt(1, listId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return false;
        }
    }

    public boolean exists(int listId, int itemId) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1 FROM group_items
                    WHERE group_id=? AND item_id=?
                )
        """)) {
            ps.setInt(1, listId);
            ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getBoolean(1);
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Integer> getItemIdsForGroup(int listId) {
        List<Integer> itemIds = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT item_id FROM group_items WHERE group_id=?
        """)) {
            ps.setInt(1, listId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                itemIds.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }

        return itemIds;
    }

    public List<Integer> getGroupIdsForItem(int itemId) {
        List<Integer> listIds = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT group_id FROM group_items WHERE item_id=?
        """)) {
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                listIds.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }

        return listIds;
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) connection.close();
    }
}
