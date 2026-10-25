package npc;

import java.util.ArrayList;
import java.util.List;

/** Definicao de um tipo de NPC carregada do JSON. Estado runtime fica em NpcItem. */
public final class NpcData {
    public enum NpcRank { NONE, MINION, BOSS, COMMANDER }
    public enum NpcBehavior { VISCERAL, INTELLIGENT, BARRIER, MERCHANT, INTERACTIVE, PORTAL }
    public enum AttackType { NONE, MELEE, RANGED }

    public static final class LootEntry {
        private int itemId;
        private double chance; // 0..1
        private int minQuantity = 1;
        private int maxQuantity = 1;
        public int getItemId() { return itemId; }
        public double getChance() { return chance; }
        public int getMinQuantity() { return minQuantity; }
        public int getMaxQuantity() { return maxQuantity; }
    }

    private int npcTypeId;
    private String npcName;
    private int npcLevel;
    private NpcRank npcRank = NpcRank.NONE;
    private NpcBehavior npcBehavior;
    private AttackType attackType = AttackType.NONE;

    private int maxHealth;
    private int minAttackDamage;
    private int maxAttackDamage;
    private int defense;
    private int minAttackElementalDamage;
    private int maxAttackElementalDamage;
    private int elementalDefense;
    private String elementType; // null = sem elemento; catalogo sera definido depois

    private float movementSpeed;
    private float detectionRange;
    private float attackRange;
    private float preferredAttackDistance;
    private float chaseResumeRange;
    private float maxChaseDistance;
    private float disengageTime; // segundos; preserva unidade do sistema existente
    private float agentRadius;
    private float separationRadius;

    private double attacksPerSecond;
    private int attackPreparationMs;
    private int experienceReward;
    private long deathDurationMs;
    private List<LootEntry> lootTable = new ArrayList<>();
    private List<String> abilities = new ArrayList<>(); // IDs; regras no futuro AbilitySystem

    public int getNpcTypeId() { return npcTypeId; }
    public String getNpcName() { return npcName; }
    public int getNpcLevel() { return npcLevel; }
    public NpcRank getNpcRank() { return npcRank; }
    public NpcBehavior getNpcBehavior() { return npcBehavior; }
    public AttackType getAttackType() { return attackType; }
    public int getMaxHealth() { return maxHealth; }
    public int getMinAttackDamage() { return minAttackDamage; }
    public int getMaxAttackDamage() { return maxAttackDamage; }
    public int getDefense() { return defense; }
    public int getMinAttackElementalDamage() { return minAttackElementalDamage; }
    public int getMaxAttackElementalDamage() { return maxAttackElementalDamage; }
    public int getElementalDefense() { return elementalDefense; }
    public String getElementType() { return elementType; }
    public float getMovementSpeed() { return movementSpeed; }
    public float getDetectionRange() { return detectionRange; }
    public float getAttackRange() { return attackRange; }
    public float getPreferredAttackDistance() { return preferredAttackDistance; }
    public float getChaseResumeRange() { return chaseResumeRange; }
    public float getMaxChaseDistance() { return maxChaseDistance; }
    public float getDisengageTime() { return disengageTime; }
    public float getAgentRadius() { return agentRadius; }
    public float getSeparationRadius() { return separationRadius; }
    public double getAttacksPerSecond() { return attacksPerSecond; }
    public int getAttackPreparationMs() { return attackPreparationMs; }
    public int getExperienceReward() { return experienceReward; }
    public long getDeathDurationMs() { return deathDurationMs; }
    public List<LootEntry> getLootTable() { return lootTable == null ? List.of() : List.copyOf(lootTable); }
    public List<String> getAbilities() { return abilities == null ? List.of() : List.copyOf(abilities); }

    /** Valor derivado: nao deve existir como campo no JSON. */
    public long getAttackIntervalMs() {
        if (!Double.isFinite(attacksPerSecond) || attacksPerSecond <= 0)
            throw new IllegalStateException("attacksPerSecond invalido em NPC " + npcTypeId);
        double interval = 1000.0 / attacksPerSecond;
        if (interval > Long.MAX_VALUE || interval < 1.0)
            throw new IllegalStateException("Intervalo de ataque fora do limite em NPC " + npcTypeId);
        return Math.round(interval);
    }

    public boolean isCombatant() {
        return npcBehavior == NpcBehavior.VISCERAL || npcBehavior == NpcBehavior.INTELLIGENT;
    }

    /** Chamar imediatamente apos desserializar cada registro do JSON. */
    public void validate() {
        if (npcTypeId < 0) fail("npcTypeId negativo");
        if (npcName == null || npcName.isBlank()) fail("npcName obrigatorio");
        if (npcLevel < 1) fail("npcLevel deve ser >= 1");
        if (npcBehavior == null) fail("npcBehavior obrigatorio");
        if (npcRank == null) fail("npcRank invalido");
        if (attackType == null) fail("attackType invalido");
        if (maxHealth < 0 || minAttackDamage < 0 || maxAttackDamage < minAttackDamage || defense < 0)
            fail("atributos fisicos invalidos");
        if (minAttackElementalDamage < 0 || maxAttackElementalDamage < minAttackElementalDamage || elementalDefense < 0)
            fail("atributos elementais invalidos");
        if (!nonNegative(movementSpeed) || !nonNegative(detectionRange) || !nonNegative(attackRange)
                || !nonNegative(preferredAttackDistance) || !nonNegative(chaseResumeRange)
                || !nonNegative(maxChaseDistance) || !nonNegative(disengageTime)
                || !nonNegative(agentRadius) || !nonNegative(separationRadius))
            fail("distancias/velocidades devem ser finitas e nao negativas");
        if (experienceReward < 0 || deathDurationMs < 0) fail("recompensa/tempo de morte invalidos");
        if (attackPreparationMs < 0) fail("attackPreparationMs negativo");
        if (isCombatant()) {
            if (attackType == AttackType.NONE) fail("combatente exige attackType");
            if (maxHealth <= 0 || attackRange <= 0 || agentRadius <= 0) fail("combatente exige vida, alcance e raio positivos");
            if (movementSpeed <= 0 || detectionRange <= 0 || maxChaseDistance <= 0)
                fail("combatente exige movimento, deteccao e perseguicao positivos");
            if (!Double.isFinite(attacksPerSecond) || attacksPerSecond <= 0 || attacksPerSecond > 1000)
                fail("attacksPerSecond deve estar entre 0 (exclusivo) e 1000");
            if (attackPreparationMs > getAttackIntervalMs()) fail("preparacao maior que intervalo de ataque");
        } else {
            if (attackType != AttackType.NONE || attackPreparationMs != 0 || attacksPerSecond != 0)
                fail("NPC nao combatente nao deve possuir ataque basico");
            if (movementSpeed != 0) fail("NPC nao combatente deve ser imovel");
        }
        if (lootTable != null) for (LootEntry entry : lootTable) {
            if (entry == null || entry.itemId <= 0 || !Double.isFinite(entry.chance)
                    || entry.chance < 0 || entry.chance > 1 || entry.minQuantity < 1
                    || entry.maxQuantity < entry.minQuantity) fail("entrada de loot invalida");
        }
        if (abilities != null) for (String id : abilities)
            if (id == null || id.isBlank()) fail("identificador de habilidade invalido");
    }

    private static boolean nonNegative(float value) { return Float.isFinite(value) && value >= 0; }
    private void fail(String reason) { throw new IllegalArgumentException("NpcData[" + npcTypeId + "]: " + reason); }
}
