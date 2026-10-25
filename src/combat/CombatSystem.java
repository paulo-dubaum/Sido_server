package combat;

import java.util.concurrent.ThreadLocalRandom;

/** Calcula o dano do impacto; as defesas sao fornecidas pela formula da classe. */
public final class CombatSystem {
    private CombatSystem() {}

    public static final class DamageResult {
        public final int physicalDamage;
        public final int elementalDamage;
        public final int totalDamage;
        public final int healthBefore;
        public final int healthAfter;

        private DamageResult(int physical, int elemental, int before, int after) {
            physicalDamage = physical;
            elementalDamage = elemental;
            totalDamage = before - after;
            healthBefore = before;
            healthAfter = after;
        }
    }

    /**
     * Defesa fisica e elemental sao valores efetivos fornecidos pela classe do alvo.
     * Nenhuma defesa e subtraida do dano bruto: mitigacao aplicada uma unica vez.
     */
    public static DamageResult applyNpcDamage(npc.NpcData attacker,
            character.Character target, int physicalDefense, int elementalDefense) {
        if (attacker == null || target == null) throw new IllegalArgumentException("Atacante/alvo ausente");
        int physical = roll(attacker.getMinAttackDamage(), attacker.getMaxAttackDamage());
        int elemental = roll(attacker.getMinAttackElementalDamage(), attacker.getMaxAttackElementalDamage());
        int level = Math.max(0, attacker.getNpcLevel());
        int mitigatedPhysical = mitigate(physical, physicalDefense, 50, level);
        int mitigatedElemental = mitigate(elemental, elementalDefense, 25, level);
        int before = Math.max(0, target.getHealth());
        int after = (int) Math.max(0L, (long) before - mitigatedPhysical - mitigatedElemental);
        target.setHealth(after);
        return new DamageResult(mitigatedPhysical, mitigatedElemental, before, after);
    }

    private static int roll(int min, int max) {
        int low = Math.max(0, Math.min(min, max));
        int high = Math.max(0, Math.max(min, max));
        return low == high ? low : (int) ThreadLocalRandom.current().nextLong(low, (long) high + 1L);
    }

    private static int mitigate(int gross, int defense, int base, int level) {
        double d = Math.max(0, defense);
        double reduction = d / (d + base + level);
        return (int) Math.floor(gross * (1.0 - reduction));
    }
}
