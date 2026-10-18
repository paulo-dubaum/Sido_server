package character;

/** Dados persistidos de um personagem; nao representa uma entidade ativa. */
public final class CharacterData {
	public final long id, userId, totalExperience;
	public final String name;
	public final int classId, level, mapId, currentHealth, currentMana, currentAgility;
	public final int strengthPoints, dextePoints, powerPoints, resourcePoints;
	public final float x, y, z;

	public CharacterData(long id, long userId, String name, int classId, int level, long totalExperience, int mapId,
			float x, float y, float z, int currentHealth, int currentMana, int currentAgility, int strengthPoints,
			int dextePoints, int powerPoints, int resourcePoints) {
		this.id = id;
		this.userId = userId;
		this.name = name;
		this.classId = classId;
		this.level = level;
		this.totalExperience = totalExperience;
		this.mapId = mapId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.currentHealth = currentHealth;
		this.currentMana = currentMana;
		this.currentAgility = currentAgility;
		this.strengthPoints = strengthPoints;
		this.dextePoints = dextePoints;
		this.powerPoints = powerPoints;
		this.resourcePoints = resourcePoints;
	}
}
