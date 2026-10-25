package character.stats;

import java.util.HashMap;
import java.util.Map;

/** Registra formulas por cclassId sem criar valores padrao ficticios. */
public final class CharacterFormulaRegistry {
    private final Map<Integer, CharacterClassFormula> formulas = new HashMap<>();

    public void register(int classId, CharacterClassFormula formula) {
        if (classId <= 0 || formula == null)
            throw new IllegalArgumentException("Classe ou formula invalida.");
        if (formulas.putIfAbsent(classId, formula) != null)
            throw new IllegalStateException("Formula ja registrada para classe " + classId);
    }

    public CharacterDerivedStats calculate(int classId, int level, CharacterAttributes attributes) {
        if (level < 1 || level > 400 || attributes == null)
            throw new IllegalArgumentException("Nivel ou atributos invalidos.");
        CharacterClassFormula formula = formulas.get(classId);
        if (formula == null)
            throw new IllegalStateException("Formula nao cadastrada para classe " + classId);
        CharacterDerivedStats result = formula.calculate(level, attributes);
        if (result == null)
            throw new IllegalStateException("Formula retornou resultado nulo para classe " + classId);
        return result;
    }
}
