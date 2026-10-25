package infra;

import java.sql.SQLException;
import com.smartfoxserver.db.IDBManager;
import com.smartfoxserver.entities.data.ISFSArray;
import com.smartfoxserver.entities.data.ISFSObject;
import character.CharacterData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Operacoes transacionais de criacao e exclusao de personagens. */
public final class CharacterLifecycleDAO {

	private final IDBManager db;

	public CharacterLifecycleDAO(IDBManager db) {
		this.db = db;
	}

	public CharacterData findByIdAndUserId(long characterId, long userId) throws SQLException {

		String sql = "SELECT id, user_id, name, class_id, level, total_experience, map_id, "
		        + "position_x, position_y, position_z, current_health, current_mana, "
		        + "current_agility, strength_points, agility_points, power_points, "
		        + "vitality_points, available_attribute_points "
		        + "FROM characters WHERE id = ? AND user_id = ?";
		ISFSArray rows = db.executeQuery(sql, new Object[] { characterId, userId });

		if (rows == null || rows.size() == 0)
			return null;

		ISFSObject r = rows.getSFSObject(0);
		return new CharacterData(((Number) r.getLong("id")).longValue(), ((Number) r.getLong("user_id")).longValue(),
				r.getString("name"), r.getInt("class_id"), r.getInt("level"), r.getLong("total_experience"),
				r.getInt("map_id"), (float) r.getDouble("position_x").floatValue(),
				(float) r.getDouble("position_y").floatValue(), (float) r.getDouble("position_z").floatValue(),
				r.getInt("current_health"), r.getInt("current_mana"), r.getInt("current_agility"),
				r.getInt("strength_points"), r.getInt("agility_points"), r.getInt("power_points"),
				r.getInt("vitality_points"), r.getInt("vitality_points"), r.getInt("available_attribute_points"));
	}

	/**
	 * Persiste somente os campos de estado, sem sobrescrever identidade ou classe.
	 */
	public void saveState(long characterId, long userId, int mapId, float x, float y, float z, int level,
			long totalExperience, int health, int mana, int currentAgility, int strength, int agility, int power,
			int resourcePoints, int availableAttributePoints) throws SQLException {
		String sql = "UPDATE characters SET map_id=?, position_x=?, position_y=?, position_z=?, "
				+ "level=?, total_experience=?, current_health=?, current_mana=?, current_agility=?, "
				+ "strength_points=?, agility_points=?, power_points=?, vitality_points=?, available_attribute_points = ?, "
				+ "last_saved_at=CURRENT_TIMESTAMP, last_logout_at=CURRENT_TIMESTAMP " + "WHERE id=? AND user_id=?";
		db.executeUpdate(sql, new Object[] { mapId, (double) x, (double) y, (double) z, level, totalExperience, health,
				mana, currentAgility, strength, agility, power, resourcePoints, availableAttributePoints, characterId, userId });
	}

	public long create(long userId, int slot, String name, int classId, int mapId, float x, float y, float z,
			int health, int mana, int agility, int strength, int dexterity, int intelligence, int vitality,
			int availablePoints) throws SQLException {
		try (Connection c = db.getConnection()) {
			boolean oldAutoCommit = c.getAutoCommit();
			c.setAutoCommit(false);
			try {
				lockAccount(c, userId);
				// Pre-validacoes com bloqueio da conta para evitar corridas no mesmo usuario.
				try (PreparedStatement ps = c
						.prepareStatement("SELECT 1 FROM characters WHERE user_id = ? AND slot = ?")) {
					ps.setLong(1, userId);
					ps.setInt(2, slot);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next())
							throw new CharacterLifecycleException("SLOT_OCCUPIED");
					}
				}
				try (PreparedStatement ps = c
						.prepareStatement("SELECT 1 FROM characters WHERE LOWER(name) = LOWER(?)")) {
					ps.setString(1, name);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next())
							throw new CharacterLifecycleException("NAME_ALREADY_EXISTS");
					}
				}
				String sql = "INSERT INTO characters (user_id, slot, name, class_id, level, "
						+ "total_experience, map_id, position_x, position_y, position_z, "
						+ "current_health, current_mana, current_agility, "
						+ "strength_points, agility_points, power_points, vitality_points, "
						+ "available_attribute_points) VALUES "
						+ "(?, ?, ?, ?, 1, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
				try (PreparedStatement ps = c.prepareStatement(sql)) {
					int i = 1;
					ps.setLong(i++, userId);
					ps.setInt(i++, slot);
					ps.setString(i++, name);
					ps.setInt(i++, classId);
					ps.setInt(i++, mapId);
					ps.setFloat(i++, x);
					ps.setFloat(i++, y);
					ps.setFloat(i++, z);
					ps.setInt(i++, health);
					ps.setInt(i++, mana);
					ps.setInt(i++, agility);
					ps.setInt(i++, strength);
					ps.setInt(i++, dexterity);
					ps.setInt(i++, intelligence);
					ps.setInt(i++, vitality);
					ps.setInt(i++, availablePoints);
					try (ResultSet rs = ps.executeQuery()) {
						if (!rs.next())
							throw new SQLException("INSERT nao retornou id.");
						long id = rs.getLong(1);
						c.commit();
						return id;
					}
				}
			} catch (SQLException | RuntimeException ex) {
				c.rollback();
				throw ex;
			} finally {
				c.setAutoCommit(oldAutoCommit);
			}
		}
	}

	public boolean delete(long userId, long characterId) throws SQLException {
		try (Connection c = db.getConnection()) {
			boolean oldAutoCommit = c.getAutoCommit();
			c.setAutoCommit(false);
			try {
				lockAccount(c, userId);
				// Com tabelas de inventario, remover dependencias na mesma transacao
				// (ou usar FKs ON DELETE CASCADE cuidadosamente definidas).
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM characters WHERE id = ? AND user_id = ?")) {
					ps.setLong(1, characterId);
					ps.setLong(2, userId);
					boolean deleted = ps.executeUpdate() == 1;
					c.commit();
					return deleted;
				}
			} catch (SQLException | RuntimeException ex) {
				c.rollback();
				throw ex;
			} finally {
				c.setAutoCommit(oldAutoCommit);
			}
		}
	}

	/** Erro de regra de negocio; seguro para retornar como codigo ao cliente. */
	public static final class CharacterLifecycleException extends SQLException {
		private final String code;

		public CharacterLifecycleException(String code) {
			super(code);
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	private static void lockAccount(Connection c, long userId) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE id = ? FOR UPDATE")) {
			ps.setLong(1, userId);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					throw new SQLException("Conta nao encontrada.");
			}
		}
	}
}
