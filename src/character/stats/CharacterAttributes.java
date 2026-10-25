package character.stats;

/** Snapshot imutavel dos atributos basicos, sem formulas universais. */
public final class CharacterAttributes {
    private final int strength, dexterity, intelligence, vitality;

    public CharacterAttributes(int strength, int dexterity, int intelligence, int vitality) {
        if (strength < 0 || dexterity < 0 || intelligence < 0 || vitality < 0)
            throw new IllegalArgumentException("Atributos nao podem ser negativos.");
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.vitality = vitality;
    }
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getIntelligence() { return intelligence; }
    public int getVitality() { return vitality; }
}
