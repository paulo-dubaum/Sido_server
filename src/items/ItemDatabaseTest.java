package items;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class ItemDatabaseTest {
    private static int tests;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        tests++;
    }
    private static void invalid(String json, ItemDatabase db) throws Exception {
        Path p = Files.createTempFile("sido-invalid-", ".json");
        try {
            Files.write(p, json.getBytes(StandardCharsets.UTF_8));
            boolean rejected = false;
            try { db.reload(p); } catch (IllegalArgumentException ex) { rejected = true; }
            check(rejected, "JSON invalido deveria ser rejeitado");
            check(db.size() == 13, "Reload invalido nao pode destruir o catalogo anterior");
        } finally { Files.deleteIfExists(p); }
    }
    public static void main(String[] args) throws Exception {
        ItemDatabase db = new ItemDatabase(Paths.get(args.length > 0 ? args[0] : "resources/items.json"));
        check(db.size() == 13, "Quantidade de itens");
        check(db.getItem(999) == null, "Consulta desconhecida");
        ItemData sword = db.requireItem(10);
        check(sword.canEquip(1, 1), "Guerreiro pode equipar espada");
        check(!sword.canEquip(2, 1), "Arqueiro nao pode equipar espada exclusiva");
        ItemInstance instance = new ItemInstance(1001L, sword, 10, false);
        EquipmentStatsCalculator.Contribution c = EquipmentStatsCalculator.calculate(sword, instance);
        check(Math.abs(c.minAttack - 40) < 1e-8 && Math.abs(c.maxAttack - 60) < 1e-8, "Refinamento ofensivo 10% por nivel");
        check(Math.abs(c.attackReachBonusMeters - 0.5) < 1e-8, "Alcance espada uma mao");
        ItemData shortSword = db.requireItem(11);
        EquipmentStatsCalculator.Contribution shortC = EquipmentStatsCalculator.calculate(shortSword, new ItemInstance(1003L, shortSword, 0, false));
        check(Math.abs(EquipmentStatsCalculator.effectiveMeleeRange(1.0, c, shortC) - 1.5) < 1e-8, "Duas espadas nao acumulam alcance");
        ItemData twoHand = db.requireItem(30);
        EquipmentStatsCalculator.Contribution twoC = EquipmentStatsCalculator.calculate(twoHand, new ItemInstance(1004L, twoHand, 0, false));
        check(Math.abs(EquipmentStatsCalculator.effectiveMeleeRange(1.0, twoC, null) - 1.7) < 1e-8, "Arma duas maos aumenta alcance");
        ItemData helmet = db.requireItem(40);
        EquipmentStatsCalculator.Contribution h = EquipmentStatsCalculator.calculate(helmet, new ItemInstance(1002L,helmet,5,false));
        check(Math.abs(h.defense - 11) < 1e-8, "Refinamento defensivo 2% por nivel");
        check(db.requireItem(41).getMaxRefinement() == 0, "Anel nao refinavel");
        check(db.requireItem(42).getMaxRefinement() == 15, "Pentagrama max 15");
        check(!db.requireItem(1).canEquip(1,400), "Moeda nao e equipamento");
        invalid("{\"items\":[{\"id\":1,\"name\":\"A\",\"type\":\"GOLD\"},{\"id\":1,\"name\":\"B\",\"type\":\"GOLD\"}]}",db);
        invalid("{\"items\":[{\"id\":5,\"name\":\"Anel\",\"type\":\"RING\",\"maxRefinement\":1}]}",db);
        invalid("{\"items\":[{\"id\":5,\"name\":\"X\",\"type\":\"BAD\"}]}",db);
        invalid("{\"items\":[{\"id\":5,\"name\":\"X\",\"type\":\"GOLD\",\"id\":6}]}",db);
        invalid("{\"items\":[] } lixo",db);
        invalid("{\"items\":[{\"id\":55,\"name\":\"Elmo\",\"type\":\"HELMET\",\"attackReachBonusMeters\":0.5}]}",db);
        System.out.println("OK: " + tests + " verificacoes aprovadas");
    }
}
