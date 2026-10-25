package npc;

import java.util.ArrayList;
import java.util.List;

import com.smartfoxserver.mmo.IMMOItemVariable;
import com.smartfoxserver.mmo.MMOItem;
import com.smartfoxserver.mmo.MMOItemVariable;
import com.smartfoxserver.mmo.Vec3D;

import navmesh.NavMeshPoint;

/**
 * Estado de um NPC registrado no SmartFoxServer MMO.
 * A movimentacao, a IA e a sincronizacao sao controladas pelo NpcManager.
 */
public class NpcItem extends MMOItem {

    // Identificacao e configuracao de deteccao.
    private final int npcTypeId;
    private final float detectionRange;

    // Posicao atual e ponto de nascimento.
    private float x;
    private float y;
    private float z;
    private final float spawnX;
    private final float spawnY;
    private final float spawnZ;
    private int currentPolygonId = -1;

    // Estado da IA e alvo atual.
    private NpcState state = NpcState.IDLE;
    private character.Character target;

    // Navegacao.
    private List<NavMeshPoint> waypoints = new ArrayList<>();
    private int currentWaypointIndex;
    private float movementSpeed = 3.0f;
    private float lastPathTargetX;
    private float lastPathTargetZ;
    private boolean hasPathTargetPosition;

    // Perseguicao e combate.
    private float attackRange = 1.0f;
    private float chaseResumeRange = 2.5f;
    private float preferredAttackDistance = 1.5f;
    private float maxChaseDistance = 30.0f;
    private long attackIntervalMs = 1500L;
    private long nextAttackTimeMs;
    private int attackSequence;

    // Contrato estatico e estado de combate da instancia.
    private NpcData data;
    private NpcData.NpcBehavior npcBehavior = NpcData.NpcBehavior.VISCERAL;
    private NpcData.NpcRank npcRank = NpcData.NpcRank.MINION;
    private NpcData.AttackType attackType = NpcData.AttackType.MELEE;
    private float agentRadius = 0.2f;
    private float separationRadius = 0.2f;
    private float disengageTime;
    private double attacksPerSecond = 1000.0 / 1500.0;
    private int attackPreparationMs;
    private int currentHealth;
    private int maxHealth;
    private long deathDurationMs;


    public NpcItem(int npcTypeId, int npcId,
                   float x, float y, float z, float detectionRange) {
        super(createInitialVariables(npcTypeId, npcId));
        this.npcTypeId = npcTypeId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.detectionRange = detectionRange;
    }

    /**
     * Cria o NPC utilizando a velocidade definida no banco de configuracoes.
     * Mantem o construtor original para compatibilidade com o NpcManager.
     */
    public NpcItem(NpcData data, int npcId,
                   float x, float y, float z) {
        this(requireData(data).getNpcTypeId(), npcId,
             x, y, z, data.getDetectionRange());
        data.validate();
        this.data = data;
        this.npcBehavior = data.getNpcBehavior();
        this.npcRank = data.getNpcRank();
        this.attackType = data.getAttackType();
        setMovementSpeed(data.getMovementSpeed());
        this.attackRange = requireNonNegativeFinite("attackRange", data.getAttackRange());
        this.preferredAttackDistance = requireNonNegativeFinite(
                "preferredAttackDistance", data.getPreferredAttackDistance());
        this.chaseResumeRange = requireNonNegativeFinite(
                "chaseResumeRange", data.getChaseResumeRange());
        this.maxChaseDistance = data.getMaxChaseDistance();
        this.agentRadius = data.getAgentRadius();
        this.separationRadius = data.getSeparationRadius();
        this.disengageTime = data.getDisengageTime();
        this.attacksPerSecond = data.getAttacksPerSecond();
        this.attackPreparationMs = data.getAttackPreparationMs();
        this.attackIntervalMs = data.isCombatant() ? data.getAttackIntervalMs() : 0L;
        this.maxHealth = data.getMaxHealth();
        this.currentHealth = maxHealth;
        this.deathDurationMs = data.getDeathDurationMs();
    }

    private static NpcData requireData(NpcData data) {
        if (data == null) {
            throw new IllegalArgumentException("NpcData nao pode ser nulo.");
        }
        return data;
    }

    private static float requireNonNegativeFinite(String name, float value) {
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(name + " deve ser finito e nao negativo.");
        }
        return value;
    }

    private static List<IMMOItemVariable> createInitialVariables(int npcTypeId, int npcId) {
        List<IMMOItemVariable> variables = new ArrayList<>(2);
        variables.add(new MMOItemVariable("nid", npcId));
        variables.add(new MMOItemVariable("ntid", npcTypeId));
        return variables;
    }

    // Identificacao.
    public int getNpcTypeId() { return npcTypeId; }
    public float getDetectionRange() { return detectionRange; }

    // Posicao.
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public Vec3D getPosition() {
        return new Vec3D(x, y, z);
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getSpawnX() { return spawnX; }
    public float getSpawnY() { return spawnY; }
    public float getSpawnZ() { return spawnZ; }
    public int getCurrentPolygonId() { return currentPolygonId; }

    public void setCurrentPolygonId(int currentPolygonId) {
        this.currentPolygonId = currentPolygonId;
    }

    // Maquina de estados e alvo.
    public NpcState getState() { return state; }

    public void setState(NpcState state) {
        this.state = state;
    }

    public character.Character getTarget() { return target; }

    public void setTarget(character.Character character) {
        this.target = character;
    }

    public void clearTarget() {
        this.target = null;
    }

    public boolean hasTarget() {
        return target != null;
    }

    // Waypoints. O primeiro ponto representa a origem do caminho.
    public List<NavMeshPoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<NavMeshPoint> waypoints) {
        this.waypoints = new ArrayList<>(waypoints);
        this.currentWaypointIndex = this.waypoints.size() > 1 ? 1 : 0;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    public NavMeshPoint getCurrentWaypoint() {
        return currentWaypointIndex >= 0 && currentWaypointIndex < waypoints.size()
                ? waypoints.get(currentWaypointIndex)
                : null;
    }

    public void advanceWaypoint() {
        if (currentWaypointIndex < waypoints.size()) {
            currentWaypointIndex++;
        }
    }

    public void clearPath() {
        waypoints.clear();
        currentWaypointIndex = 0;
        hasPathTargetPosition = false;
    }

    public boolean hasPath() {
        return currentWaypointIndex < waypoints.size();
    }

    public float getMovementSpeed() { return movementSpeed; }

    public void setMovementSpeed(float movementSpeed) {
        if (!Float.isFinite(movementSpeed) || movementSpeed < 0.0f) {
            throw new IllegalArgumentException("Velocidade não pode ser negativa.");
        }
        this.movementSpeed = movementSpeed;
    }

    public void setPathTargetPosition(float x, float z) {
        this.lastPathTargetX = x;
        this.lastPathTargetZ = z;
        this.hasPathTargetPosition = true;
    }

    public boolean hasPathTargetPosition() { return hasPathTargetPosition; }
    public float getLastPathTargetX() { return lastPathTargetX; }
    public float getLastPathTargetZ() { return lastPathTargetZ; }

    // Perseguicao.
    public float getChaseResumeRange() { return chaseResumeRange; }
    public float getMaxChaseDistance() { return maxChaseDistance; }
    public float getPreferredAttackDistance() { return preferredAttackDistance; }

    public void setMaxChaseDistance(float maxChaseDistance) {
        if (!Float.isFinite(maxChaseDistance) || maxChaseDistance <= 0.0f) {
            throw new IllegalArgumentException("Distância máxima deve ser positiva.");
        }
        this.maxChaseDistance = maxChaseDistance;
    }

    // Atributos estaticos do tipo de NPC e estado da instancia.
    public NpcData getNpcData() { return data; }
    public NpcData.NpcBehavior getNpcBehavior() { return npcBehavior; }
    public NpcData.NpcRank getNpcRank() { return npcRank; }
    public NpcData.AttackType getAttackType() { return attackType; }
    public boolean isCombatant() {
        return npcBehavior == NpcData.NpcBehavior.VISCERAL
                || npcBehavior == NpcData.NpcBehavior.INTELLIGENT;
    }
    public float getAgentRadius() { return agentRadius; }
    public float getSeparationRadius() { return separationRadius; }
    public float getDisengageTime() { return disengageTime; }
    public double getAttacksPerSecond() { return attacksPerSecond; }
    public int getAttackPreparationMs() { return attackPreparationMs; }
    public int getCurrentHealth() { return currentHealth; }
    public int getMaxHealth() { return maxHealth; }
    public long getDeathDurationMs() { return deathDurationMs; }
    public void setCurrentHealth(int health) {
        if (health < 0 || health > maxHealth) throw new IllegalArgumentException("Vida fora do limite");
        this.currentHealth = health;
    }

    // Combate.
    public float getAttackRange() { return attackRange; }
    public long getAttackIntervalMs() { return attackIntervalMs; }

    public void setAttackIntervalMs(long attackIntervalMs) {
        if (attackIntervalMs <= 0L) {
            throw new IllegalArgumentException("O intervalo de ataque deve ser positivo.");
        }
        if (data != null) {
            throw new IllegalStateException("Intervalo derivado do NpcData; altere attacksPerSecond no JSON.");
        }
        this.attackIntervalMs = attackIntervalMs;
    }

    public long getNextAttackTimeMs() { return nextAttackTimeMs; }

    public void setNextAttackTimeMs(long nextAttackTimeMs) {
        this.nextAttackTimeMs = nextAttackTimeMs;
    }

    public void resetAttackCooldown() {
        nextAttackTimeMs = 0L;
    }

    public int getAttackSequence() { return attackSequence; }

    public void incrementAttackSequence() {
        attackSequence++;
    }
}
