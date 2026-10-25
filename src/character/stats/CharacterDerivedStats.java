package character.stats;

/** Resultado das formulas da classe. Nao armazena vida/mana/estamina atuais. */
public final class CharacterDerivedStats {
    private final int minAttack, maxAttack, defense, maxHealth, maxMana, maxStamina;

    public CharacterDerivedStats(int minAttack, int maxAttack, int defense,
                                 int maxHealth, int maxMana, int maxStamina) {
        if (minAttack < 0 || maxAttack < minAttack || defense < 0 ||
                maxHealth < 0 || maxMana < 0 || maxStamina < 0)
            throw new IllegalArgumentException("Atributos derivados invalidos.");
        this.minAttack = minAttack;
        this.maxAttack = maxAttack;
        this.defense = defense;
        this.maxHealth = maxHealth;
        this.maxMana = maxMana;
        this.maxStamina = maxStamina;
    }
    public int getMinAttack() { return minAttack; }
    public int getMaxAttack() { return maxAttack; }
    public int getDefense() { return defense; }
    public int getMaxHealth() { return maxHealth; }
    public int getMaxMana() { return maxMana; }
    public int getMaxStamina() { return maxStamina; }
}
