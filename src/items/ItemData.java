package items;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Definicao de catalogo. Nunca guarda estado de um item pertencente a um jogador. */
public final class ItemData {
    private final int id, requiredLevel, maxRefinement;
    private final String name;
    private final ItemType type;
    private final Set<Integer> allowedClassIds;
    private final double minAttack, maxAttack, defense, attackSpeed, attackReachBonusMeters;

    public ItemData(int id, String name, ItemType type, int requiredLevel,
                    Set<Integer> allowedClassIds, int maxRefinement,
                    double minAttack, double maxAttack, double defense, double attackSpeed, double attackReachBonusMeters) {
        if (id <= 0 || name == null || name.trim().isEmpty() || type == null || requiredLevel < 0)
            throw new IllegalArgumentException("Identificacao/requisitos invalidos");
        if (allowedClassIds == null || allowedClassIds.contains(null))
            throw new IllegalArgumentException("allowedClassIds invalido");
        for (Integer c : allowedClassIds) if (c <= 0) throw new IllegalArgumentException("classId invalido");
        int limit = refinementLimit(type);
        if (maxRefinement < 0 || maxRefinement > limit)
            throw new IllegalArgumentException("Refinamento invalido para " + type + ": " + maxRefinement);
        if (!valid(minAttack) || !valid(maxAttack) || !valid(defense) || !valid(attackSpeed) || !valid(attackReachBonusMeters) || maxAttack < minAttack)
            throw new IllegalArgumentException("Valores numericos invalidos");
        if (!isOffensive(type) && (minAttack != 0 || maxAttack != 0 || attackSpeed != 0))
            throw new IllegalArgumentException("Item nao ofensivo nao pode ter dano/velocidade");
        if (!isEquipment(type) && (maxRefinement != 0 || defense != 0))
            throw new IllegalArgumentException("Item nao equipado nao pode ter refinamento/defesa");
        if (attackReachBonusMeters > 0 && type != ItemType.WEAPON_ONE_HAND && type != ItemType.WEAPON_TWO_HAND)
            throw new IllegalArgumentException("Bonus de alcance reservado para armas de mao nesta versao");
        this.id = id; this.name = name.trim(); this.type = type;
        this.requiredLevel = requiredLevel;
        this.allowedClassIds = Collections.unmodifiableSet(new HashSet<>(allowedClassIds));
        this.maxRefinement = maxRefinement; this.minAttack = minAttack;
        this.maxAttack = maxAttack; this.defense = defense; this.attackSpeed = attackSpeed;
        this.attackReachBonusMeters = attackReachBonusMeters;
    }

    private static boolean valid(double d) { return Double.isFinite(d) && d >= 0; }
    public static boolean isOffensive(ItemType type) {
        return type == ItemType.WEAPON_ONE_HAND || type == ItemType.WEAPON_TWO_HAND ||
               type == ItemType.BOW || type == ItemType.QUIVER || type == ItemType.SHIELD;
    }
    public static boolean isEquipment(ItemType type) {
        return type != ItemType.GOLD && type != ItemType.JEWEL && type != ItemType.CONSUMABLE && type != ItemType.SCROLL;
    }
    public static int refinementLimit(ItemType type) {
        if (type == ItemType.WINGS || type == ItemType.PENTAGRAM || type == ItemType.EARRING) return 15;
        if (type == ItemType.RING || type == ItemType.NECKLACE) return 0;
        return isEquipment(type) ? 20 : 0;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getRequiredLevel() { return requiredLevel; }
    /** Conjunto vazio = todas as classes. */
    public Set<Integer> getAllowedClassIds() { return allowedClassIds; }
    public boolean canEquip(int classId, int level) {
        return isEquipment(type) && level >= requiredLevel && (allowedClassIds.isEmpty() || allowedClassIds.contains(classId));
    }
    public int getMaxRefinement() { return maxRefinement; }
    public double getMinAttack() { return minAttack; }
    public double getMaxAttack() { return maxAttack; }
    public double getDefense() { return defense; }
    /** Unidade: ataques por segundo; a composicao final pertence a formula da classe. */
    public double getAttackSpeed() { return attackSpeed; }
    /** Bonus em metros somado ao alcance desarmado; nao escala com refinamento. */
    public double getAttackReachBonusMeters() { return attackReachBonusMeters; }
}
