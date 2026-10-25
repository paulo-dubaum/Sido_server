package items;

/** Apenas contribuicao BASE de itens. Classe, buffs, sorte, excelentes e conjuntos ficam fora. */
public final class EquipmentStatsCalculator {
    private EquipmentStatsCalculator() {}
    public static final class Contribution {
        public final double minAttack, maxAttack, defense, attackSpeed, attackReachBonusMeters;
        private Contribution(double min, double max, double def, double speed, double reach) {
            minAttack = min; maxAttack = max; defense = def; attackSpeed = speed; attackReachBonusMeters = reach;
        }
    }
    /** Duas armas nao somam alcance: prevalece a de maior alcance. */
    public static double effectiveMeleeRange(double unarmedRangeMeters, Contribution mainHand, Contribution offHand) {
        if (!Double.isFinite(unarmedRangeMeters) || unarmedRangeMeters < 0)
            throw new IllegalArgumentException("Alcance desarmado invalido");
        double main = mainHand == null ? 0 : mainHand.attackReachBonusMeters;
        double off = offHand == null ? 0 : offHand.attackReachBonusMeters;
        return unarmedRangeMeters + Math.max(main, off);
    }
    public static Contribution calculate(ItemData definition, ItemInstance instance) {
        if (definition == null || instance == null || definition.getId() != instance.getDefinitionId())
            throw new IllegalArgumentException("Item e definicao divergentes");
        int r = instance.getRefinementLevel();
        double attackFactor = 1.0 + r * 0.10;
        double defenseFactor = 1.0 + r * 0.02;
        return new Contribution(definition.getMinAttack() * attackFactor,
                definition.getMaxAttack() * attackFactor,
                definition.getDefense() * defenseFactor, definition.getAttackSpeed(),
                definition.getAttackReachBonusMeters());
    }
}
