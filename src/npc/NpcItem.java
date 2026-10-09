package npc;


import java.util.ArrayList;
import java.util.LinkedList;


import java.util.List;

import com.smartfoxserver.mmo.IMMOItemVariable;
import com.smartfoxserver.mmo.MMOItem;
import com.smartfoxserver.mmo.MMOItemVariable;
import com.smartfoxserver.mmo.Vec3D;

import navmesh.NavMeshPoint;



public class NpcItem extends MMOItem {

    private final int npcTypeId;

    private float x;
    private float y;
    private float z;

    private final float spawnX;
    private final float spawnY;
    private final float spawnZ;

    private final float detectionRange;

    private int currentPolygonId = -1;

    private NpcState state =
            NpcState.IDLE;
    
    private character.Character target;
    
    private List<NavMeshPoint> waypoints =
            new ArrayList<>();

    private int currentWaypointIndex = 0;
    
    private float movementSpeed = 3.0f;
    private float attackRange = 1.0f;
    
    private float lastPathTargetX;
    private float lastPathTargetZ;

    private boolean hasPathTargetPosition = false;
    
    private float chaseResumeRange = 2.5f;
    
    private float maxChaseDistance = 30.0f;
    
    private long attackIntervalMs = 1500L;
    private long nextAttackTimeMs = 0L;
    
    private int attackSequence = 0;
    
    private float preferredAttackDistance = 1.5f;

    public float getPreferredAttackDistance() {
        return preferredAttackDistance;
    }

    public int getAttackSequence() {
        return attackSequence;
    }

    public void incrementAttackSequence() {
        attackSequence++;
    }

    public long getAttackIntervalMs() {
        return attackIntervalMs;
    }

    public float getChaseResumeRange() {
        return chaseResumeRange;
    }
    
    public float getMaxChaseDistance() {
        return maxChaseDistance;
    }

    public NpcItem(
            int npcTypeId,
            float x,
            float y,
            float z,
            float detectionRange) {

        super(createVariables(npcTypeId));

        this.npcTypeId =
                npcTypeId;

        this.x = x;
        this.y = y;
        this.z = z;

        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;

        this.detectionRange =
                detectionRange;
    }
    
    public void setMaxChaseDistance(float maxChaseDistance) {
        if (maxChaseDistance <= 0.0f) {
            throw new IllegalArgumentException(
                    "Distância máxima deve ser positiva."
            );
        }

        this.maxChaseDistance = maxChaseDistance;
    }
    
    public void setAttackIntervalMs(long attackIntervalMs) {

        if (attackIntervalMs <= 0L) {
            throw new IllegalArgumentException(
                    "O intervalo de ataque deve ser positivo."
            );
        }

        this.attackIntervalMs = attackIntervalMs;
    }

    public long getNextAttackTimeMs() {
        return nextAttackTimeMs;
    }

    public void setNextAttackTimeMs(long nextAttackTimeMs) {
        this.nextAttackTimeMs = nextAttackTimeMs;
    }

    public void resetAttackCooldown() {
        nextAttackTimeMs = 0L;
    }

    private static List<IMMOItemVariable> createVariables(
            int npcTypeId) {

        List<IMMOItemVariable> variables =
                new LinkedList<>();

        /*
         * Informações que o CLIENTE precisa conhecer.
         *
         * "t" = tipo da entidade
         * "n" = npcTypeId
         */

        variables.add(
                new MMOItemVariable(
                        "t",
                        1
                )
        );

        variables.add(
                new MMOItemVariable(
                        "n",
                        npcTypeId
                )
        );

        return variables;
    }
    
    
    
    public List<NavMeshPoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(
            List<NavMeshPoint> waypoints) {

        this.waypoints =
                new ArrayList<>(waypoints);

        /*
         * WP0 é a posição inicial do NPC.
         * Portanto começamos seguindo WP1.
         */
        if (this.waypoints.size() > 1) {
            this.currentWaypointIndex = 1;
        } else {
            this.currentWaypointIndex = 0;
        }
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    public NavMeshPoint getCurrentWaypoint() {

        if (currentWaypointIndex < 0
                || currentWaypointIndex >= waypoints.size()) {

            return null;
        }

        return waypoints.get(
                currentWaypointIndex
        );
    }

    public void advanceWaypoint() {

        if (currentWaypointIndex
                < waypoints.size()) {

            currentWaypointIndex++;
        }
    }

    public void clearPath() {

        waypoints.clear();
        currentWaypointIndex = 0;

        hasPathTargetPosition = false;
    }

    public boolean hasPath() {

        return currentWaypointIndex
                < waypoints.size();
    }
    
    public void setMovementSpeed(float movementSpeed) {
        if (movementSpeed < 0.0f) {
            throw new IllegalArgumentException(
                    "Velocidade não pode ser negativa."
            );
        }

        this.movementSpeed = movementSpeed;
    }
    
    public void setPathTargetPosition(float x, float z) {

        lastPathTargetX = x;
        lastPathTargetZ = z;

        hasPathTargetPosition = true;
    }

    public boolean hasPathTargetPosition() {
        return hasPathTargetPosition;
    }

    public float getLastPathTargetX() {
        return lastPathTargetX;
    }

    public float getLastPathTargetZ() {
        return lastPathTargetZ;
    }
    
    public float getMovementSpeed() {
        return movementSpeed;
    }
    
    public character.Character getTarget() {
        return target;
    }

    public void setTarget(character.Character character) {
        this.target = character;
    }

    public void clearTarget() {
        this.target = null;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public int getNpcTypeId() {
        return npcTypeId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Vec3D getPosition() {

        return new Vec3D(
                x,
                y,
                z
        );
    }

    public void setPosition(
            float x,
            float y,
            float z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public float getSpawnZ() {
        return spawnZ;
    }

    public float getDetectionRange() {
        return detectionRange;
    }

    public int getCurrentPolygonId() {
        return currentPolygonId;
    }

    public void setCurrentPolygonId(
            int currentPolygonId) {

        this.currentPolygonId =
                currentPolygonId;
    }

    public NpcState getState() {
        return state;
    }

    public void setState(
            NpcState state) {

        this.state = state;
    }
    
    public float getAttackRange() {
        return attackRange;
    }
}

