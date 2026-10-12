
package npc;

public class NpcData {

    private int npcTypeId;
    private String npcName;

    private int npcLevel;
    private int maxHealth;
    private int minAttackDamage;
    private int maxAttackDamage;
    private int defense;

    private int minAttackElementalDamage;
    private int maxAttackElementalDamage;
    private int elementalDefense;

    private float movementSpeed;
    private float detectionRange;
    private float attackRange;
    private float preferredAttackDistance;
    private float chaseResumeRange;
    private float maxChaseDistance;
    private int attackIntervalMs;

    private float agentRadius;
    private float separationRadius;
    
    private float disengageTime;
    
    private int combatIntelligence = 0;

    public int getCombatIntelligence() {
        return combatIntelligence;
    }

    public int getNpcTypeId() {
        return npcTypeId;
    }

    public String getNpcName() {
        return npcName;
    }

    public int getNpcLevel() {
        return npcLevel;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getMinAttackDamage() {
        return minAttackDamage;
    }

    public int getMaxAttackDamage() {
        return maxAttackDamage;
    }

    public int getDefense() {
        return defense;
    }

    public int getMinAttackElementalDamage() {
        return minAttackElementalDamage;
    }

    public int getMaxAttackElementalDamage() {
        return maxAttackElementalDamage;
    }

    public int getElementalDefense() {
        return elementalDefense;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public float getDetectionRange() {
        return detectionRange;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public float getPreferredAttackDistance() {
        return preferredAttackDistance;
    }

    public float getChaseResumeRange() {
        return chaseResumeRange;
    }

    public float getMaxChaseDistance() {
        return maxChaseDistance;
    }

    public int getAttackIntervalMs() {
        return attackIntervalMs;
    }

    public float getAgentRadius() {
        return agentRadius;
    }

    public float getSeparationRadius() {
        return separationRadius;
    }

	public float getDisengageTime() {
		return disengageTime;
	}

	public void setDisengageTime(float disengageTime) {
		this.disengageTime = disengageTime;
	}
}
