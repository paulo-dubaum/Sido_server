package npc;
import java.lang.reflect.Field;
public final class NpcDataTest {
    static void set(NpcData d, String key, Object value) throws Exception {
        Field f = NpcData.class.getDeclaredField(key); f.setAccessible(true); f.set(d, value);
    }
    static NpcData valid() throws Exception {
        NpcData d = new NpcData();
        set(d,"npcTypeId",1); set(d,"npcName","Spider"); set(d,"npcLevel",1);
        set(d,"npcRank",NpcData.NpcRank.MINION); set(d,"npcBehavior",NpcData.NpcBehavior.VISCERAL);
        set(d,"attackType",NpcData.AttackType.MELEE); set(d,"maxHealth",30);
        set(d,"attackRange",1.5f); set(d,"movementSpeed",3f); set(d,"detectionRange",10f);
        set(d,"maxChaseDistance",20f); set(d,"agentRadius",.2f);
        set(d,"attacksPerSecond",1.25); set(d,"attackPreparationMs",300);
        return d;
    }
    static void rejected(NpcData d) {
        try { d.validate(); throw new AssertionError("Deveria rejeitar"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        NpcData d = valid(); d.validate();
        if (d.getAttackIntervalMs() != 800) throw new AssertionError("Intervalo incorreto");
        set(d,"attackPreparationMs",801); rejected(d);
        d = valid(); set(d,"attacksPerSecond",0.0); rejected(d);
        d = valid(); set(d,"attacksPerSecond",Double.NaN); rejected(d);
        d = valid(); set(d,"npcBehavior",NpcData.NpcBehavior.BARRIER); rejected(d);
        d = valid(); set(d,"npcBehavior",NpcData.NpcBehavior.MERCHANT);
        set(d,"attackType",NpcData.AttackType.NONE); set(d,"movementSpeed",0f);
        set(d,"attacksPerSecond",0.0); set(d,"attackPreparationMs",0); d.validate();
        System.out.println("6 testes aprovados");
    }
}
