package infra;

import java.sql.SQLException;
import com.smartfoxserver.db.IDBManager;
import com.smartfoxserver.entities.data.ISFSArray;
import com.smartfoxserver.entities.data.ISFSObject;
import character.CharacterData;

public final class CharacterDAO {
    private final IDBManager db;
    public CharacterDAO(IDBManager db) { this.db = db; }

    public CharacterData findByIdAndUserId(long characterId, long userId) throws SQLException {
        String sql = "SELECT id, user_id, name, class_id, level, total_experience, map_id, "
                + "position_x, position_y, position_z, current_health, current_mana, "
                + "current_agility, strength_points, agility_points, power_points, resource_points "
                + "FROM characters WHERE id = ? AND user_id = ?";
        ISFSArray rows = db.executeQuery(sql, new Object[] { characterId, userId });
        if (rows == null || rows.size() == 0) return null;
        ISFSObject r = rows.getSFSObject(0);
        return new CharacterData(
                ((Number) r.getLong("id")).longValue(),
                ((Number) r.getLong("user_id")).longValue(),
                r.getString("name"), r.getInt("class_id"), r.getInt("level"),
                r.getLong("total_experience"), r.getInt("map_id"),
                (float) r.getDouble("position_x").floatValue(), (float) r.getDouble("position_y").floatValue(),
                (float) r.getDouble("position_z").floatValue(), r.getInt("current_health"),
                r.getInt("current_mana"), r.getInt("current_agility"),
                r.getInt("strength_points"), r.getInt("agility_points"),
                r.getInt("power_points"), r.getInt("resource_points"));
    }
    /** Persiste somente os campos de estado, sem sobrescrever identidade ou classe. */
    public void saveState(long characterId, long userId, int mapId,
            float x, float y, float z, int level, long totalExperience,
            int health, int mana, int currentAgility,
            int strength, int agility, int power, int resourcePoints) throws SQLException {
        String sql = "UPDATE characters SET map_id=?, position_x=?, position_y=?, position_z=?, "
                + "level=?, total_experience=?, current_health=?, current_mana=?, current_agility=?, "
                + "strength_points=?, agility_points=?, power_points=?, resource_points=?, "
                + "last_saved_at=CURRENT_TIMESTAMP, last_logout_at=CURRENT_TIMESTAMP "
                + "WHERE id=? AND user_id=?";
        db.executeUpdate(sql, new Object[] {mapId, (double)x, (double)y, (double)z,
                level, totalExperience, health, mana, currentAgility,
                strength, agility, power, resourcePoints, characterId, userId});
    }

}
