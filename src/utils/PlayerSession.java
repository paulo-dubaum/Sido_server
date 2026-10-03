package utils;

public class PlayerSession {

    private int characterId;
    private String name;
    private int classId;
    private long exp;
    private int mapId;
    
    
	public int getCharacterId() {
		return characterId;
	}
	public void setCharacterId(int characterId) {
		this.characterId = characterId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getClassId() {
		return classId;
	}
	
	public void setClassId(int classId) {
		this.classId = classId;
	}
	
	public long getExp() {
		return exp;
	}
	public void setExp(long exp) {
		this.exp = exp;
	}
	private int getMapId() {
		return mapId;
	}
	private void setMapId(int mapId) {
		this.mapId = mapId;
	}

    // getters e setters
}