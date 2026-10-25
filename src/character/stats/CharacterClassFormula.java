package character.stats;

/** Implementacao especifica de cada classe (e, futuramente, sua progressao). */
public interface CharacterClassFormula {
    CharacterDerivedStats calculate(int level, CharacterAttributes attributes);
}
