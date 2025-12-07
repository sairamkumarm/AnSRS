package ansrs.db;

import ansrs.data.Group;
import ansrs.util.Log;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GroupRepository implements AutoCloseable {

    private final Connection connection;
    private final GroupItemRepository groupItemRepository;

    public GroupRepository(Connection connection) {
        this.connection = connection;
        this.groupItemRepository = new GroupItemRepository(connection);
    }

    public boolean createGroup(int id, String name, String link) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO groups (id, name, link, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
        """)) {
            Timestamp now = Timestamp.from(Instant.now());
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, link);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), "23505"))
                Log.error("Duplicate GROUP_ID["+id+"] found.");
            else Log.error("SQL ERROR: "+ e.getMessage());
            return false;
        }
    }

    public boolean updateGroup(int id, String name, String link) {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE groups
                SET name=?, link=?, updated_at=?
                WHERE id=?
        """)) {
            ps.setString(1, name);
            ps.setString(2, link);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return false;
        }
    }

    public boolean deleteGroupById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("""
                DELETE FROM groups WHERE id=?
        """)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return false;
        }
    }

    public boolean exists(int id) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT EXISTS(SELECT 1 FROM groups WHERE id=?)
        """)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getBoolean(1);
        } catch (SQLException e) {
            return false;
        }
    }



    public Optional<Group> findById(int id) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT * FROM groups WHERE id=?
        """)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return Optional.empty();

            return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<List<Group>> findAll() {
        List<Group> lists = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT * FROM groups
        """)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lists.add(mapRow(rs));
            }
            return Optional.of(lists);
        } catch (SQLException e) {
            Log.error(e.getMessage());
            return Optional.empty();
        }
    }

    // ================= JOIN TABLE PROXIES =================

    public boolean addItemToGroup(int listId, int itemId) {
        return groupItemRepository.addItemToGroup(listId, itemId);
    }

    public boolean addItemsToGroupBatch(int listId, List<Integer> itemIds) {
        return groupItemRepository.addItemsToGroupBatch(listId, itemIds);
    }

    public boolean removeItemFromGroup(int listId, int itemId) {
        return groupItemRepository.removeItemFromGroup(listId, itemId);
    }

    public boolean removeAllItemsFromGroup(int listId) {
        return groupItemRepository.removeAllItemsFromGroup(listId);
    }

    public boolean itemExistsInGroup(int listId, int itemId) {
        return groupItemRepository.exists(listId, itemId);
    }

    public List<Integer> getItemIdsForGroup(int listId) {
        return groupItemRepository.getItemIdsForGroup(listId);
    }

    public List<Integer> getListIdsForGroup(int itemId) {
        return groupItemRepository.getGroupIdsForItem(itemId);
    }

    private Group mapRow(ResultSet rs) throws SQLException {
        return new Group(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("link"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) connection.close();
    }


}
