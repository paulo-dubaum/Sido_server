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
    private float agentRadius = 0.0f; // Construtor legado: sem colisao ate configurar via NpcData.
    private float lastPathTargetX;
    private float lastPathTargetZ;
    private boolean hasPathTargetPosition;

    // Perseguicao e combate.
    private float attackRange = 1.0f;
    private float chaseResumeRange = 2.5f;
    private float preferredAttackDistance = 1.5f;
    private float disengageTime = 2.0f;
    private float maxChaseDistance = 30.0f;
    private long attackIntervalMs = 1500L;
    private long nextAttackTimeMs;
    private int attackSequence;
    
    private int combatIntelligence;

    public int getCombatIntelligence() {
        return combatIntelligence;
    }

    public void setCombatIntelligence(int combatIntelligence) {
        this.combatIntelligence = combatIntelligence;
    }

    public NpcItem(int npcTypeId, int npcId,
            float x, float y, float z, float detectionRange) {

 super(createInitialVariables(npcTypeId, npcId, x, y, z));

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
        setMovementSpeed(data.getMovementSpeed());
        this.agentRadius = requireNonNegativeFinite("agentRadius", data.getAgentRadius());
        this.attackRange = requireNonNegativeFinite("attackRange", data.getAttackRange());
        this.preferredAttackDistance = requireNonNegativeFinite(
                "preferredAttackDistance", data.getPreferredAttackDistance());
        this.chaseResumeRange = requireNonNegativeFinite(
                "chaseResumeRange", data.getChaseResumeRange());
        setMaxChaseDistance(data.getMaxChaseDistance());
        setAttackIntervalMs(data.getAttackIntervalMs());
        
        this.combatIntelligence = data.getCombatIntelligence();
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

    private static List<IMMOItemVariable> createInitialVariables(
            int npcTypeId, int npcId,
            float x, float y, float z) {

        List<IMMOItemVariable> variables = new ArrayList<>(6);

        variables.add(new MMOItemVariable("nid", npcId));
        variables.add(new MMOItemVariable("ntid", npcTypeId));

        variables.add(new MMOItemVariable("x", x));
        variables.add(new MMOItemVariable("y", y));
        variables.add(new MMOItemVariable("z", z));
        variables.add(new MMOItemVariable("rot", 0.0f));

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
    public float getAgentRadius() { return agentRadius; }

    public void setMovementSpeed(float movementSpeed) {
        if (movementSpeed < 0.0f) {
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
        if (maxChaseDistance <= 0.0f) {
            throw new IllegalArgumentException("Distância máxima deve ser positiva.");
        }
        this.maxChaseDistance = maxChaseDistance;
    }

    // Combate.
    public float getAttackRange() { return attackRange; }
    public long getAttackIntervalMs() { return attackIntervalMs; }

    public void setAttackIntervalMs(long attackIntervalMs) {
        if (attackIntervalMs <= 0L) {
            throw new IllegalArgumentException("O intervalo de ataque deve ser positivo.");
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

	public float getDisengageTime() {
		// TODO Auto-generated method stub
		return disengageTime;
	}
	
}
