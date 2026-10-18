package character;

import com.smartfoxserver.entities.User;

/**
 * Representa a base de um personagem do jogo.
 *
 * Esta classe é abstrata e centraliza os atributos e comportamentos comuns
 * a todos os tipos de personagem do sistema, como identidade, posição no
 * mundo e navegação.
 *
 * Cada personagem possui:
 * - um identificador único;
 * - um usuário associado ao SmartFoxServer;
 * - um nome;
 * - coordenadas de posição no mapa (x, y, z);
 * - o identificador do polígono atual em que se encontra.
 *
 * Classes especializadas, como Warrior, Mage e Archer, devem herdar desta
 * classe e adicionar comportamentos específicos de cada tipo de personagem.
 *
 * @author Paulo Dubaum
 * @since 1.0
 */

public abstract class Character {

    // =========================================================
    // IDENTITY
    // =========================================================

    private final long characterId;
    private final User user;

    private String name;
    
    // =========================================================
    // STATS
    // =========================================================
    
    private int cclassId;
	private int level;
    private int experience;
    private long totalExperience;
    private int currentAgility;
    private int health;
    private int mana;
    private int strength;
    private int dexterity;
    private int intelligence;
    private int resourcePoints;
    
    // =========================================================
    // WORLD POSITION
    // =========================================================

    private float x;
    private float y;
    private float z;

    // =========================================================
    // NAVIGATION
    // =========================================================

    private int currentPolygonId = -1;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    
    // O que precisamos implementar e que cada "raça" vai definir
    /* Calcular Defesa()
     * Calcular Ataque Mim e Max()
     * Calcular Total de Pontos de vida()
     * Calcular Total de pontos de mana()
     * Adicionar Stamina
     * Calcular o level pela experience
     * 
     */

    protected Character(
            long characterId,
            User user,
            String name,
            float x,
            float y,
            float z) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Character precisa estar associado a um User."
            );
        }

        this.characterId = characterId;
        this.user = user;
        this.name = name;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    // =========================================================
    // IDENTITY
    // =========================================================

    public long getCharacterId() {
        return characterId;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // =========================================================
    // POSITION
    // =========================================================

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void setPosition(
            float x,
            float y,
            float z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    public int getCurrentPolygonId() {
        return currentPolygonId;
    }

    public void setCurrentPolygonId(
            int currentPolygonId) {

        this.currentPolygonId =
                currentPolygonId;
    }

    // =========================================================
    // DEBUG
    // =========================================================

    @Override
    public String toString() {

        return getClass().getSimpleName()
                + "{"
                + "characterId=" + characterId
                + ", name='" + name + '\''
                + ", userId=" + user.getId()
                + ", position=("
                + x + ", "
                + y + ", "
                + z + ")"
                + ", polygon="
                + currentPolygonId
                + '}';
    }
    
    public int getCclassId() {
		return cclassId;
	}

	public void setCclassId(int cclassId) {
		this.cclassId = cclassId;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setExperience(int experience) {
	    this.experience = experience;
	}

	public long getTotalExperience() {
	    return totalExperience;
	}

	public void setTotalExperience(long totalExperience) {
	    this.totalExperience = totalExperience;
	}

    public int getCurrentAgility() { 
    	return currentAgility; 
    }
    
    public void setCurrentAgility(int value)
    {
    	this.currentAgility = value; 
    }
    
    
    //this.totalExperience = experience;
	

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public int getMana() {
		return mana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = strength;
	}

	public int getDexterity() {
		return dexterity;
	}

	public void setDexterity(int dexterity) {
		this.dexterity = dexterity;
	}

	public int getIntelligence() {
		return intelligence;
	}

	public void setIntelligence(int intelligence) {
		this.intelligence = intelligence;
	}

	public int getResourcePoints() {
		return resourcePoints;
	}

	public void setResourcePoints(int resourcePoints) {
		this.resourcePoints = resourcePoints;
	}

}
