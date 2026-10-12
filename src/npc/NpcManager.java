package npc;

import character.CharacterManager;
import navmesh.Funnel;
import navmesh.NavMeshPoint;
import navmesh.NavMeshPolygon;
import navmesh.NavMeshPortal;
import world.AStar;
import world.ServerNavMesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.mmo.IMMOItemVariable;
import com.smartfoxserver.mmo.MMOItemVariable;
import com.smartfoxserver.mmo.MMORoom;
import com.smartfoxserver.mmo.Vec3D;

/** Gerencia o ciclo de vida, a navegação e o combate dos NPCs da sala. */
public class NpcManager {

    private static final int ATTACK_SLOT_COUNT = 8;
    private static final float PATH_RECALC_DISTANCE = 0.2f;
    private static final int PATH_RECALC_INTERVAL_TICKS = 2;
    private static final float MAX_DELTA_TIME = 0.25f;
    private static final float ATTACK_ENTER_TOLERANCE = 0.05f;
    private static final float SPAWN_ARRIVAL_DISTANCE = 0.2f;
    private static final float WAYPOINT_EPSILON = 0.0001f;
    private static final float MOVEMENT_EPSILON = 0.00001f;
    private static final int LOG_INTERVAL_TICKS = 10;
    private static final float COLLISION_EPSILON = 0.0005f;
    // Apenas atacantes autorizados proximos do alvo usam colisao suavizada.
    private static final float ATTACK_APPROACH_RADIUS_MULTIPLIER = 0.65f;
    private static final float ATTACK_APPROACH_MARGIN = 0.60f;
    private static final float CHASE_PROGRESS_EPSILON = 0.10f;
    private static final int TACTICAL_ORBIT_TICKS = 12;
    private static final int TACTICAL_STUCK_TICKS = 10;
    private long tacticalOrbitMoves, tacticalOrbitFailures, tacticalRepositions;
    private static final int TACTICAL_SECTOR_COUNT = 16;
    private static final int TACTICAL_DESTINATION_TICKS = 18;
    private static final int TACTICAL_ATTACK_REPOSITION_TICKS = 25;
    private static final float TACTICAL_MIN_ANGULAR_GAIN = 0.30f;
    private final Map<Integer, Long> tacticalNextReposition = new HashMap<>();
    private long tacticalAttackRepositions;
    private final Map<Integer, TacticalDestination> tacticalDestinations = new HashMap<>();
    private long tacticalSectorSelections, tacticalSectorFailures, tacticalSectorSwitches;
    private static final class TacticalDestination {
        final float x, z, targetX, targetZ;
        final long expires;
        TacticalDestination(float x, float z, float targetX, float targetZ, long expires) {
            this.x=x; this.z=z; this.targetX=targetX; this.targetZ=targetZ; this.expires=expires;
        }
    }
    private long bumpAttempts, bumpAccepted, bumpRejected;
    private static final float BUMP_DISTANCE = 0.20f;
    private static final float BUMP_MAX_DISTANCE = 0.55f;
    private static final float BUMP_CLEARANCE_EPSILON = 0.0001f;
    private long bumpRejectNavMesh, bumpRejectCollision, bumpRejectReservation;
    private long bumpProgressive, intelligentYields, bumpRecoveredChasers;
    private final Map<Integer, Integer> pendingBumpChasers = new HashMap<>();
    private final Map<Integer, Float> bumpChaserDistances = new HashMap<>();
    private static final int BUMP_COOLDOWN_TICKS = 3;
    private final Map<Integer, Long> lastBumpTick = new HashMap<>();
    private static final int SEPARATION_PASSES = 3;
    private long separationNs, separationCorrections, separationRemaining;

    // Reservas de movimento válidas somente durante o tick atual.
    private static final class MoveIntent {
        final NpcItem npc;
        final float fromX, fromZ, x, y, z, rotation;
        final int polygonId;
        final boolean advanceWaypoint;
        MoveIntent(NpcItem npc, float fromX, float fromZ, float x, float y, float z,
                   float rotation, int polygonId, boolean advanceWaypoint) {
            this.npc = npc;
            this.fromX = fromX; this.fromZ = fromZ;
            this.x = x; this.y = y; this.z = z;
            this.rotation = rotation; this.polygonId = polygonId;
            this.advanceWaypoint = advanceWaypoint;
        }
    }
    private final List<MoveIntent> moveIntents = new ArrayList<>();
    private long intentAccepted, intentRejected;


    // Recuperacao de perseguidores sem progresso (10 ticks ~= 1 segundo).
    private static final int STUCK_CHECK_TICKS = 10;
    private static final int STUCK_RETRY_TICKS = 15;
    private static final int SATURATED_RETRY_TICKS = 30;
    private static final int WAIT_RING_RECHECK_TICKS = 20;
    private static final float WAIT_RING_EXTRA_RADIUS = 0.85f;
    private static final float WAIT_RING_WAKE_DISTANCE_SQ = 0.30f * 0.30f;
    private static final float WAIT_RING_ARRIVAL_SQ = 0.12f * 0.12f;
    private long waitRingAssigned, waitRingFailures, waitRingWakeups, waitRingRechecks;
    private long tacticalYieldRequests, tacticalYieldAccepted, tacticalYieldRejected;
    private long waitRetargets, waitRetargetChecks, orbitStuckRecoveries;
    private static final float STUCK_MIN_MOVEMENT_SQ = 0.08f * 0.08f;
    private final Map<Integer, StuckState> stuckStates = new HashMap<>();
    private final Map<Integer, Integer> attackSlotOverrides = new HashMap<>();
    private final Map<Integer, ChaseProgress> chaseProgress = new HashMap<>();
    // Evita reacquisicao imediata do mesmo alvo apos desistir.
    private final Map<Integer, character.Character> ignoredUntilExit = new HashMap<>();
    private long disengagedByTimeout, softCollisionSteps;

    private static final class ChaseProgress {
        float closestDistance;
        long lastProgressNs;
        ChaseProgress(float distance, long nowNs) {
            closestDistance = distance;
            lastProgressNs = nowNs;
        }
    }
    private long stuckDetected, slotChanges, slotChangeFailures;
    private long saturatedWaits, saturatedRechecks, blockedRecoveries;
    // Diagnostico de rejeicoes de destinos alternativos (contagens por candidato).
    private long slotRejectedReserved, slotRejectedNavMesh, slotRejectedOccupied;
    private long slotRejectedPath, slotRejectedSame, slotRejectedSmallGroup;
    private long slotCandidatesEvaluated;

    private static final class StuckState {
        float x, z;
        long lastCheckTick;
        long nextRetryTick;
        boolean waitingForSpace;
        boolean waitingOnOuterRing;
        float waitX, waitZ, targetX, targetZ;
        int orbitSlot = -1;
        long nextOrbitTick;
        float orbitProgressX, orbitProgressZ;
        long orbitProgressTick;
        StuckState(float x, float z, long tick) {
            this.x = x;
            this.z = z;
            this.lastCheckTick = tick;
        }
    }


    private final MMORoom room;
    private final ServerNavMesh navMesh;
    private final CharacterManager characterManager;
    private final Map<Integer, NpcItem> npcs = new HashMap<>();

    private long tickCount;
    private long lastUpdateTimeNs;

    // Métricas acumuladas entre relatórios (100 ticks = ~10 segundos).
    private static final int PERF_INTERVAL_TICKS = 100;
    private long astarNs, funnelNs, portalsNs, syncNs;
    private long astarCalls, funnelCalls, portalsCalls, syncCalls;
    private long maxAstarNs, maxFunnelNs, maxPortalsNs, maxSyncNs;
    private long totalUpdateNs, maxUpdateNs;
    private long spatialNs, coordinatesNs, rotationNs, attackSyncNs;
    private long spatialCalls, coordinatesCalls, rotationCalls, attackSyncCalls;
    private long maxSpatialNs, maxCoordinatesNs, maxRotationNs, maxAttackSyncNs;
    
    private long fixedSlotFull = 0;
    private long fixedSlotAssigned = 0;
    private long fixedSlotReleased = 0;


    public NpcManager(MMORoom room, ServerNavMesh navMesh,
                      CharacterManager characterManager) {
        this.room = room;
        this.navMesh = navMesh;
        this.characterManager = characterManager;
    }

    // ---------------------------------------------------------------------
    // Ciclo de vida
    // ---------------------------------------------------------------------

    public synchronized NpcItem spawnNpc(NpcItem npc) {
        if (npc == null) {
            throw new IllegalArgumentException("NpcItem não pode ser nulo.");
        }

        NavMeshPolygon polygon = navMesh.findContainingPolygon(
            npc.getX(), npc.getY(), npc.getZ()
        );

        if (polygon == null) {
            System.out.println(
                "NpcManager: spawn recusado. Posição fora do NavMesh: "
                + npc.getX() + ", "
                + npc.getY() + ", "
                + npc.getZ()
            );
            return null;
        }

        npc.setCurrentPolygonId(polygon.id);

        SmartFoxServer.getInstance().getAPIManager().getMMOApi()
            .setMMOItemPosition(
                npc,
                new Vec3D(npc.getX(), npc.getY(), npc.getZ()),
                room
            );

        npcs.put(npc.getId(), npc);

        System.out.printf(
            "NPC registrado: ID=%d Type=%d Radius=%.3f Speed=%.2f%n",
            npc.getId(),
            npc.getNpcTypeId(),
            npc.getAgentRadius(),
            npc.getMovementSpeed()
        );

        return npc;
    }
    public synchronized boolean removeNpc(int mmoItemId) {
        NpcItem npc = npcs.remove(mmoItemId);
        tacticalDestinations.remove(mmoItemId);
        tacticalNextReposition.remove(mmoItemId);
        stuckStates.remove(mmoItemId);
        ignoredUntilExit.remove(mmoItemId);
        lastBumpTick.remove(mmoItemId);
        chaseProgress.remove(mmoItemId);
        if (attackSlotOverrides.remove(mmoItemId) != null) fixedSlotReleased++;
        if (npc == null) {
            return false;
        }
        SmartFoxServer.getInstance().getAPIManager().getMMOApi()
                .removeMMOItem(npc);
        return true;
    }

    public synchronized void onCharacterDisconnected(character.Character character) {
        for (NpcItem npc : npcs.values()) {
            if (npc.getTarget() == character) {
                startReturning(npc);
            }
        }
    }

    public synchronized NpcItem getNpc(int mmoItemId) {
        return npcs.get(mmoItemId);
    }

    public synchronized Collection<NpcItem> getNpcs() {
        return Collections.unmodifiableList(new ArrayList<>(npcs.values()));
    }

    public synchronized int getNpcCount() {
        return npcs.size();
    }

    // ---------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------

    public synchronized void update() {
        long startNs = System.nanoTime();
        if (lastUpdateTimeNs == 0L) {
            lastUpdateTimeNs = startNs;
            return;
        }

        float deltaTime = (startNs - lastUpdateTimeNs) / 1_000_000_000.0f;
        lastUpdateTimeNs = startNs;
        deltaTime = Math.max(0.0f, Math.min(deltaTime, MAX_DELTA_TIME));
        tickCount++;

        // Planeja antes de aplicar; prioridade inicial rotativa por tick.
        moveIntents.clear();
        List<NpcItem> ordered = new ArrayList<>(npcs.values());
        ordered.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        if (!ordered.isEmpty()) {
            Collections.rotate(ordered, -(int)(tickCount % ordered.size()));
        }
        for (NpcItem npc : ordered) {
            updateNpc(npc, deltaTime);
        }
        for (MoveIntent intent : moveIntents) {
            NpcItem npc = intent.npc;
            npc.setPosition(intent.x, intent.y, intent.z);
            npc.setCurrentPolygonId(intent.polygonId);
            if (intent.advanceWaypoint) npc.advanceWaypoint();
            syncNpcPosition(npc, intent.x, intent.y, intent.z, intent.rotation);
        }
        moveIntents.clear();
        // O progresso so conta quando o perseguidor realmente se aproxima apos um empurrao.
        for (java.util.Iterator<Map.Entry<Integer, Integer>> it = pendingBumpChasers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Integer> entry = it.next();
            if (tickCount <= entry.getValue()) continue;
            NpcItem chaser = npcs.get(entry.getKey());
            if (chaser != null && chaser.getTarget() != null) {
                float current = distanceSquaredXZ(chaser.getX(), chaser.getZ(),
                        chaser.getTarget().getX(), chaser.getTarget().getZ());
                Float previous = bumpChaserDistances.get(entry.getKey());
                if (previous != null && current + 0.0001f < previous) bumpRecoveredChasers++;
            }
            bumpChaserDistances.remove(entry.getKey());
            it.remove();
        }
        long separationStartNs = System.nanoTime();
        resolveNpcOverlaps();
        separationNs += System.nanoTime() - separationStartNs;

        long updateNs = System.nanoTime() - startNs;
        totalUpdateNs += updateNs;
        maxUpdateNs = Math.max(maxUpdateNs, updateNs);
        if (tickCount % LOG_INTERVAL_TICKS == 0) {
            System.out.printf("NPC UPDATE DeltaTime=%.6f NPCs=%d Processing=%.3f ms%n",
                    deltaTime, npcs.size(), updateNs / 1_000_000.0);
        }
        if (tickCount % PERF_INTERVAL_TICKS == 0) {
            printPerformance();
        }
        
        if (tickCount % 100 == 0) {
            debugNpcDistances();
        }
    }

    private void updateNpc(NpcItem npc, float deltaTime) {
        if (npc.getState() == NpcState.DEAD) {
            releaseAttackSlot(npc);
            stuckStates.remove(npc.getId());
            chaseProgress.remove(npc.getId());
            return;
        }
        switch (npc.getState()) {
            case IDLE:
                updateIdle(npc);
                break;
            case CHASE:
                updateChase(npc, deltaTime);
                break;
            case ATTACK:
                updateAttack(npc, deltaTime);
                break;
            case RETURNING:
                updateReturning(npc, deltaTime);
                break;
            case DEAD:
                break;
        }
    }

    // ---------------------------------------------------------------------
    // Estados
    // ---------------------------------------------------------------------

    private void updateIdle(NpcItem npc) {
        if (npc.hasTarget()) {
            return;
        }
        character.Character ignored = ignoredUntilExit.get(npc.getId());
        if (ignored != null) {
            // So reconsidera o alvo depois que ele sair da zona de deteccao.
            if (isTargetNavigable(ignored)
                    && isInsideDetectionRange(npc, ignored.getX(), ignored.getZ())) return;
            ignoredUntilExit.remove(npc.getId());
        }
        for (character.Character character : characterManager.getCharacters()) {
            if (!isTargetNavigable(character)
                    || !isInsideDetectionRange(npc, character.getX(), character.getZ())) {
                continue;
            }
            npc.setTarget(character);
            chaseProgress.put(npc.getId(), new ChaseProgress(
                    (float) Math.sqrt(distanceSquaredXZ(npc.getX(), npc.getZ(),
                            character.getX(), character.getZ())), System.nanoTime()));
            npc.setState(NpcState.CHASE);
            System.out.println("NPC " + npc.getId() + " adquiriu Character "
                    + character.getCharacterId() + " como alvo.");
            return;
        }
    }

    private void updateChase(NpcItem npc, float deltaTime) {
        character.Character target = npc.getTarget();
        if (!isTargetNavigable(target) || exceededChaseDistance(npc)) {
            startReturning(npc);
            return;
        }
        // Reserva em espera nao e perseguidor travado: nao aplica timeout.
        if (attackSlotOverrides.containsKey(npc.getId())
                && hasChaseTimedOut(npc, target)) {
            disengagedByTimeout++;
            ignoredUntilExit.put(npc.getId(), target);
            startReturning(npc);
            return;
        }

        StuckState waitState = stuckStates.get(npc.getId());
        boolean hasSlot = attackSlotOverrides.containsKey(npc.getId());

        // A vaga e uma autorizacao logica para atacar, nao uma condicao
        // para que o NPC possa perseguir ou caminhar ate a area de espera.
        if (!hasSlot && (waitState == null || !waitState.waitingOnOuterRing)) {
            hasSlot = acquireAttackSlot(npc, target);
            if (!hasSlot) {
                if (waitState == null) {
                    waitState = new StuckState(npc.getX(), npc.getZ(), tickCount);
                    stuckStates.put(npc.getId(), waitState);
                }
                // Se nao houver vaga, continuar procurando o anel externo.
                // Uma falha temporaria nao deve congelar a perseguicao.
                if (tickCount >= waitState.nextRetryTick) {
                    assignOuterRingWait(npc, target, waitState);
                    waitState.nextRetryTick = tickCount + WAIT_RING_RECHECK_TICKS;
                }
            }
        }

        if (hasSlot && waitState != null) waitState.waitingOnOuterRing = false;
        if (waitState != null && waitState.waitingOnOuterRing) {
            if (tryRetargetWaitingNpc(npc, target)) return;
            updateOuterRingWait(npc, target, deltaTime, waitState);
            return;
        }

        float distanceSquared = distanceSquaredXZ(
                npc.getX(), npc.getZ(), target.getX(), target.getZ());
        float attackEnterDistance = npc.getAttackRange();
        if (hasSlot && distanceSquared <= attackEnterDistance * attackEnterDistance) {
            npc.clearPath();
            npc.setState(NpcState.ATTACK);
            chaseProgress.remove(npc.getId());
            stuckStates.remove(npc.getId());
            npc.setNextAttackTimeMs(System.currentTimeMillis()
                    + npc.getAttackIntervalMs());
            return;
        }

        if (hasSlot) {
            if (shouldRecalculateChasePath(npc, target)) {
                NavMeshPoint destination = isTactical(npc)
                        ? getTacticalAttackDestination(npc, target)
                        : getAttackSlotDestination(npc, target);
                if (!isTactical(npc) && distanceSquared <= (npc.getAttackRange() + ATTACK_APPROACH_MARGIN)
                        * (npc.getAttackRange() + ATTACK_APPROACH_MARGIN)) {
                    destination = new NavMeshPoint(target.getX(), target.getY(), target.getZ());
                }
                if (destination != null) {
                    NavMeshPolygon polygon = navMesh.findContainingPolygon(
                            destination.x, destination.y, destination.z);
                    if (polygon != null && calculatePath(npc, polygon.id, destination, false)) {
                        npc.setPathTargetPosition(destination.x, destination.z);
                    }
                }
            }
            // Destino radial fora do NavMesh ou sem rota: tentar aproximacao
            // pelo poligono do jogador. Nao libera a vaga de ataque.
            if (!npc.hasPath()) {
                NavMeshPoint fallback = new NavMeshPoint(target.getX(), target.getY(), target.getZ());
                if (calculatePath(npc, target.getCurrentPolygonId(), fallback, false)) {
                    npc.setPathTargetPosition(fallback.x, fallback.z);
                }
            }
        } else if (!npc.hasPath() && waitState != null
                && tickCount >= waitState.nextRetryTick) {
            // Nenhuma vaga e nenhum caminho: tentar novamente o anel externo.
            assignOuterRingWait(npc, target, waitState);
            waitState.nextRetryTick = tickCount + WAIT_RING_RECHECK_TICKS;
        }

        if (npc.hasPath()) moveNpc(npc, deltaTime);
        if (hasSlot) checkStuckChaser(npc, target);
    }

    private void updateAttack(NpcItem npc, float deltaTime) {
        character.Character target = npc.getTarget();
        if (!isTargetNavigable(target) || exceededChaseDistance(npc)) {
            startReturning(npc);
            return;
        }
        if (!attackSlotOverrides.containsKey(npc.getId())) {
            npc.setState(NpcState.CHASE);
            return;
        }

        float distanceSquared = distanceSquaredXZ(
                npc.getX(), npc.getZ(), target.getX(), target.getZ());
        // Histerese: nunca retoma CHASE antes de sair efetivamente do alcance.
        float chaseResumeRange = Math.max(npc.getChaseResumeRange(),
                npc.getAttackRange() + ATTACK_ENTER_TOLERANCE);
        if (distanceSquared > chaseResumeRange * chaseResumeRange) {
            npc.setState(NpcState.CHASE);
            chaseProgress.put(npc.getId(), new ChaseProgress(
                    (float) Math.sqrt(distanceSquared), System.nanoTime()));
            return;
        }

        float attackRange = npc.getAttackRange();
        if (distanceSquared > attackRange * attackRange) {
            npc.setState(NpcState.CHASE);
            chaseProgress.put(npc.getId(), new ChaseProgress(
                    (float) Math.sqrt(distanceSquared), System.nanoTime()));
            return;
        }

        // Reposicionamento opcional: somente se um setor acessivel trouxer
        // ganho angular real. O NPC continua em ATTACK e conserva sua permissao.
        if (isTactical(npc)) updateTacticalAttackerPosition(npc, target, deltaTime);

        long now = System.currentTimeMillis();
        if (now < npc.getNextAttackTimeMs()) {
            return;
        }

        executeAttack(npc);
        npc.setNextAttackTimeMs(now + npc.getAttackIntervalMs());
    }

    private void updateReturning(NpcItem npc, float deltaTime) {
        float distanceSquared = distanceSquaredXZ(
                npc.getX(), npc.getZ(), npc.getSpawnX(), npc.getSpawnZ());
        if (distanceSquared <= SPAWN_ARRIVAL_DISTANCE * SPAWN_ARRIVAL_DISTANCE) {
            // O snap ao spawn nao pode atravessar um NPC parado.
            if (isMovementBlockedByNpc(npc, npc.getSpawnX(), npc.getSpawnZ())) {
                return;
            }
            npc.setPosition(npc.getSpawnX(), npc.getSpawnY(), npc.getSpawnZ());
            syncNpcPosition(npc, npc.getSpawnX(), npc.getSpawnY(), npc.getSpawnZ());
            npc.clearPath();
            npc.setState(NpcState.IDLE);
            System.out.println("NPC " + npc.getId() + " retornou ao spawn.");
            return;
        }

        if (!npc.hasPath() && !calculateReturnPath(npc)) {
            return;
        }
        moveNpc(npc, deltaTime);
    }

    private void startReturning(NpcItem npc) {
        npc.setTarget(null);
        stuckStates.remove(npc.getId());
        chaseProgress.remove(npc.getId());
        releaseAttackSlot(npc);
        tacticalDestinations.remove(npc.getId());
        npc.clearPath();
        npc.setState(NpcState.RETURNING);
    }

    // ---------------------------------------------------------------------
    // Navegação
    // ---------------------------------------------------------------------

    /** Grupo ordenado; inclui ATTACK para preservar a distribuicao do cerco. */
    private List<NpcItem> getAttackGroup(character.Character target) {
        List<NpcItem> group = new ArrayList<>();
        for (NpcItem other : npcs.values()) {
            if (other.getTarget() == target
                    && (other.getState() == NpcState.CHASE || other.getState() == NpcState.ATTACK)) {
                group.add(other);
            }
        }
        group.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return group;
    }

    /** A vaga pertence ao NPC ate sair do combate, morrer ou ser removido. */
    private int getSlotIndex(NpcItem npc, List<NpcItem> group) {
        Integer slot = attackSlotOverrides.get(npc.getId());
        return slot == null ? -1 : slot;
    }

    private NavMeshPoint getSlotPoint(NpcItem npc, character.Character target,
                                      int slot, int slotCount) {
        double angle = 2.0 * Math.PI * slot / slotCount;
        // Raio geometrico de aproximacao, independente do raio de colisao do NPC.
        float radius = npc.getPreferredAttackDistance();
        return new NavMeshPoint(
                target.getX() + (float) Math.cos(angle) * radius,
                target.getY(),
                target.getZ() + (float) Math.sin(angle) * radius);
    }

    /** Menor separacao angular em radianos entre esta posicao e os atacantes. */
    private float tacticalAngularClearance(NpcItem npc, character.Character target, float x, float z) {
        double angle = Math.atan2(z - target.getZ(), x - target.getX());
        float nearest = (float)Math.PI;
        for (NpcItem other : npcs.values()) {
            if (other == npc || other.getTarget() != target || other.getState() == NpcState.DEAD
                    || !attackSlotOverrides.containsKey(other.getId())) continue;
            double otherAngle = Math.atan2(other.getZ() - target.getZ(), other.getX() - target.getX());
            double difference = Math.abs(angle - otherAngle);
            nearest = Math.min(nearest, (float)Math.min(difference, 2.0 * Math.PI - difference));
        }
        return nearest;
    }

    /** Reposiciona atacantes sem trocar o estado de combate nem a vaga logica. */
    private void updateTacticalAttackerPosition(NpcItem npc, character.Character target, float deltaTime) {
        Long next = tacticalNextReposition.get(npc.getId());
        if (next != null && tickCount < next) {
            if (npc.hasPath()) moveNpc(npc, deltaTime);
            return;
        }
        tacticalNextReposition.put(npc.getId(), tickCount + TACTICAL_ATTACK_REPOSITION_TICKS);
        npc.clearPath();
        // Um destino so e escolhido se melhorar nitidamente a separacao angular.
        float current = tacticalAngularClearance(npc, target, npc.getX(), npc.getZ());
        float radius = Math.min(npc.getPreferredAttackDistance(), npc.getAttackRange() * 0.88f);
        radius = Math.max(0.12f, radius);
        float bestScore = current;
        NavMeshPoint best = null;
        for (int sector = 0; sector < TACTICAL_SECTOR_COUNT; sector++) {
            double angle = 2.0 * Math.PI * sector / TACTICAL_SECTOR_COUNT;
            float x = target.getX() + (float)Math.cos(angle) * radius;
            float z = target.getZ() + (float)Math.sin(angle) * radius;
            NavMeshPolygon polygon = navMesh.findContainingPolygon(x, target.getY(), z);
            if (polygon == null) continue;
            float angular = tacticalAngularClearance(npc, target, x, z);
            float travel = (float)Math.sqrt(distanceSquaredXZ(npc.getX(), npc.getZ(), x, z));
            float score = angular - travel * 0.22f;
            if (score > bestScore + TACTICAL_MIN_ANGULAR_GAIN) {
                bestScore = score;
                best = new NavMeshPoint(x, target.getY(), z);
            }
        }
        if (best == null) return;
        NavMeshPolygon polygon = navMesh.findContainingPolygon(best.x, best.y, best.z);
        if (polygon != null && calculatePath(npc, polygon.id, best, false)) {
            npc.setPathTargetPosition(best.x, best.z);
            tacticalAttackRepositions++;
            if (npc.hasPath()) moveNpc(npc, deltaTime);
        }
    }

    /** Posicao de aproximacao inteligente independente do indice da permissao.
     *  Amostra setores validos do NavMesh, privilegia espacos desocupados e
     *  mantem o destino por alguns ticks para impedir oscilacoes. */
    private NavMeshPoint getTacticalAttackDestination(NpcItem npc, character.Character target) {
        TacticalDestination cached = tacticalDestinations.get(npc.getId());
        if (cached != null && tickCount < cached.expires
                && distanceSquaredXZ(cached.targetX, cached.targetZ, target.getX(), target.getZ()) < 0.16f) {
            return new NavMeshPoint(cached.x, target.getY(), cached.z);
        }
        float radius = Math.min(npc.getPreferredAttackDistance(), npc.getAttackRange() * 0.88f);
        radius = Math.max(0.12f, radius);
        float bestScore = -Float.MAX_VALUE;
        float bestX = 0f, bestZ = 0f;
        boolean found = false;
        for (int sector = 0; sector < TACTICAL_SECTOR_COUNT; sector++) {
            double angle = 2.0 * Math.PI * sector / TACTICAL_SECTOR_COUNT;
            float x = target.getX() + (float)Math.cos(angle) * radius;
            float z = target.getZ() + (float)Math.sin(angle) * radius;
            if (navMesh.findContainingPolygon(x, target.getY(), z) == null) continue;
            float nearest = Float.MAX_VALUE;
            float congestion = 0f;
            for (NpcItem other : npcs.values()) {
                if (other == npc || other.getTarget() != target || other.getState() == NpcState.DEAD
                        || !attackSlotOverrides.containsKey(other.getId())) continue;
                float d = (float)Math.sqrt(distanceSquaredXZ(x, z, other.getX(), other.getZ()));
                nearest = Math.min(nearest, d);
                float clearance = npc.getAgentRadius() + other.getAgentRadius();
                if (d < clearance * 1.2f) congestion += (clearance * 1.2f - d) * 12f;
            }
            float travel = (float)Math.sqrt(distanceSquaredXZ(npc.getX(), npc.getZ(), x, z));
            float angular = tacticalAngularClearance(npc, target, x, z);
            float score = Math.min(nearest, 2f) * 1.3f + angular * 2.4f
                    - congestion - travel * 0.65f;
            // Desempate deterministico, evitando todos escolherem o mesmo setor.
            score -= (Math.floorMod(sector - npc.getId(), TACTICAL_SECTOR_COUNT)) * 0.001f;
            if (!found || score > bestScore) {
                found = true; bestScore = score; bestX = x; bestZ = z;
            }
        }
        if (!found) {
            tacticalSectorFailures++;
            return getAttackSlotDestination(npc, target);
        }
        if (cached != null && distanceSquaredXZ(cached.x, cached.z, bestX, bestZ) > 0.04f)
            tacticalSectorSwitches++;
        tacticalSectorSelections++;
        tacticalDestinations.put(npc.getId(), new TacticalDestination(bestX, bestZ,
                target.getX(), target.getZ(), tickCount + TACTICAL_DESTINATION_TICKS));
        return new NavMeshPoint(bestX, target.getY(), bestZ);
    }

    /** Tenta reservar uma das oito vagas, escolhendo a mais proxima acessivel. */
    private boolean acquireAttackSlot(NpcItem npc, character.Character target) {
        if (attackSlotOverrides.containsKey(npc.getId())) return true;
        List<NpcItem> group = getAttackGroup(target);
        boolean[] reserved = new boolean[ATTACK_SLOT_COUNT];
        for (NpcItem other : group) {
            if (other == npc) continue;
            Integer slot = attackSlotOverrides.get(other.getId());
            if (slot != null) reserved[slot] = true;
        }
        int bestSlot = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int slot = 0; slot < ATTACK_SLOT_COUNT; slot++) {
            if (reserved[slot]) continue;
            NavMeshPoint point = getSlotPoint(npc, target, slot, ATTACK_SLOT_COUNT);
            // Reserva logica independente de geometria: um ponto fora do
            // NavMesh nao pode impedir a aquisicao de uma vaga de combate.
            float distance = distanceSquaredXZ(npc.getX(), npc.getZ(), point.x, point.z);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSlot = slot;
            }
        }
        if (bestSlot < 0) {
			fixedSlotFull++;
            return false;
        }
        attackSlotOverrides.put(npc.getId(), bestSlot);
        fixedSlotAssigned++;
        System.out.printf("NPC FIXED SLOT id=%d slot=%d%n", npc.getId(), bestSlot);
        return true;
    }

    private void releaseAttackSlot(NpcItem npc) {
        if (attackSlotOverrides.remove(npc.getId()) != null) fixedSlotReleased++;
    }

    private NavMeshPoint getAttackSlotDestination(NpcItem npc, character.Character target) {
        if (!acquireAttackSlot(npc, target)) return null;
        int slot = attackSlotOverrides.get(npc.getId());
        return getSlotPoint(npc, target, slot, ATTACK_SLOT_COUNT);
    }

    /** Em bloqueio, so troca para outra vaga realmente livre. */
    private boolean changeBlockedAttackSlot(NpcItem npc, character.Character target) {
        Integer current = attackSlotOverrides.get(npc.getId());
        if (current == null) return acquireAttackSlot(npc, target) &&
                calculateAttackSlotPath(npc, target);
        List<NpcItem> group = getAttackGroup(target);
        boolean[] reserved = new boolean[ATTACK_SLOT_COUNT];
        for (NpcItem other : group) {
            if (other == npc) continue;
            Integer slot = attackSlotOverrides.get(other.getId());
            if (slot != null) reserved[slot] = true;
        }
        for (int offset = 1; offset < ATTACK_SLOT_COUNT; offset++) {
            for (int side = 0; side < 2; side++) {
                int slot = Math.floorMod(current + (side == 0 ? offset : -offset), ATTACK_SLOT_COUNT);
                if (slot == current) { slotRejectedSame++; continue; }
                if (reserved[slot]) { slotRejectedReserved++; continue; }
                slotCandidatesEvaluated++;
                NavMeshPoint point = getSlotPoint(npc, target, slot, ATTACK_SLOT_COUNT);
                NavMeshPolygon polygon = navMesh.findContainingPolygon(point.x, point.y, point.z);
                if (polygon == null) { slotRejectedNavMesh++; continue; }
                boolean occupied = false;
                for (NpcItem other : group) {
                    if (other == npc) continue;
                    float clearance = npc.getAgentRadius() + other.getAgentRadius();
                    if (distanceSquaredXZ(point.x, point.z, other.getX(), other.getZ())
                            < clearance * clearance) { occupied = true; break; }
                }
                if (occupied) { slotRejectedOccupied++; continue; }
                if (!calculatePath(npc, polygon.id, point, false)) {
                    slotRejectedPath++; continue;
                }
                attackSlotOverrides.put(npc.getId(), slot);
                npc.setPathTargetPosition(point.x, point.z);
                System.out.printf("NPC SLOT CHANGE id=%d from=%d to=%d%n",
                        npc.getId(), current, slot);
                return true;
            }
        }
        return false;
    }

    private boolean calculateAttackSlotPath(NpcItem npc, character.Character target) {
        Integer slot = attackSlotOverrides.get(npc.getId());
        if (slot == null) return false;
        NavMeshPoint point = getSlotPoint(npc, target, slot, ATTACK_SLOT_COUNT);
        NavMeshPolygon polygon = navMesh.findContainingPolygon(point.x, point.y, point.z);
        if (polygon == null || !calculatePath(npc, polygon.id, point, false)) return false;
        npc.setPathTargetPosition(point.x, point.z);
        return true;
    }

    /** Diagnostico conservador: todos os pontos amostrados do anel
     *  estao cobertos por NPCs em ATTACK. Nao altera a colisao ou o movimento.
     */
    private boolean isAttackRingSaturated(NpcItem npc, character.Character target) {
        int count = 0;
        for (NpcItem other : getAttackGroup(target)) {
            if (other != npc && attackSlotOverrides.containsKey(other.getId())) count++;
        }
        return count >= ATTACK_SLOT_COUNT;
    }

    private void checkStuckChaser(NpcItem npc, character.Character target) {
        int id = npc.getId();
        StuckState state = stuckStates.get(id);
        if (state == null) {
            stuckStates.put(id, new StuckState(npc.getX(), npc.getZ(), tickCount));
            return;
        }
        if (tickCount - state.lastCheckTick < STUCK_CHECK_TICKS) return;
        float displacementSq = distanceSquaredXZ(state.x, state.z, npc.getX(), npc.getZ());
        state.x = npc.getX();
        state.z = npc.getZ();
        state.lastCheckTick = tickCount;
        if (displacementSq >= STUCK_MIN_MOVEMENT_SQ) {
            state.waitingForSpace = false;
            return;
        }
        if (tickCount < state.nextRetryTick) return;
        if (state.waitingForSpace) {
            saturatedRechecks++;
            state.waitingForSpace = false;
        }
        float attackDistance = npc.getAttackRange();
        if (distanceSquaredXZ(npc.getX(), npc.getZ(), target.getX(), target.getZ())
                <= attackDistance * attackDistance) return;
        stuckDetected++;
        blockedRecoveries++;
        state.nextRetryTick = tickCount + STUCK_RETRY_TICKS;
        // O titular mantem a permissao e nao entra no anel de espera.
        // Reinicia a rota para evitar insistir em um waypoint bloqueado.
        npc.clearPath();
        if (isTactical(npc)) tacticalDestinations.remove(npc.getId());
        if (isTactical(npc) && tryTacticalReposition(npc, target)) {
            tacticalRepositions++;
            return;
        }
        NavMeshPoint destination = new NavMeshPoint(target.getX(), target.getY(), target.getZ());
        if (calculatePath(npc, target.getCurrentPolygonId(), destination, false)) {
            npc.setPathTargetPosition(destination.x, destination.z);
        } else {
            slotChangeFailures++;
        }
    }

    private boolean tryTacticalReposition(NpcItem npc, character.Character target) {
        // Tangencial ao alvo; inverte o sentido por ID, sem trocar permissao.
        float dx = npc.getX() - target.getX();
        float dz = npc.getZ() - target.getZ();
        float length = (float)Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001f) return false;
        float side = (npc.getId() & 1) == 0 ? 1f : -1f;
        float radius = Math.max(npc.getAttackRange() * 0.9f, length);
        double angle = Math.atan2(dz, dx) + side * Math.PI / 5.0;
        float x = target.getX() + (float)Math.cos(angle) * radius;
        float z = target.getZ() + (float)Math.sin(angle) * radius;
        NavMeshPolygon polygon = navMesh.findContainingPolygon(x, target.getY(), z);
        if (polygon == null) return false;
        NavMeshPoint point = new NavMeshPoint(x, target.getY(), z);
        if (!calculatePath(npc, polygon.id, point, false)) return false;
        npc.setPathTargetPosition(x, z);
        return true;
    }

    /** Destino externo temporario, sem substituir as reservas do anel de ataque. */
    private boolean isTactical(NpcItem npc) {
        // 0: irracional; 1+: inteligente. Exige getter em NpcItem.
        return npc.getCombatIntelligence() > 0;
    }

    private boolean assignOuterRingWait(NpcItem npc, character.Character target, StuckState state) {
        return assignOuterRingWait(npc, target, state, false);
    }

    private boolean assignOuterRingWait(NpcItem npc, character.Character target,
                                        StuckState state, boolean advance) {
        List<NpcItem> group = getAttackGroup(target);
        int count = Math.max(12, group.size() * 2);
        float radius = npc.getPreferredAttackDistance() + WAIT_RING_EXTRA_RADIUS;
        int start = advance && state.orbitSlot >= 0
                ? (state.orbitSlot + 1) % count
                : Math.floorMod(npc.getId(), count);
        for (int i = 0; i < count; i++) {
            int slot = (start + i) % count;
            double angle = 2.0 * Math.PI * slot / count;
            float x = target.getX() + (float) Math.cos(angle) * radius;
            float z = target.getZ() + (float) Math.sin(angle) * radius;
            NavMeshPolygon polygon = navMesh.findContainingPolygon(x, target.getY(), z);
            if (polygon == null) continue;
            boolean occupied = false;
            for (NpcItem other : npcs.values()) {
                if (other == npc || other.getState() == NpcState.DEAD) continue;
                float clearance = npc.getAgentRadius() + other.getAgentRadius() + 0.12f;
                if (distanceSquaredXZ(x, z, other.getX(), other.getZ()) < clearance * clearance) {
                    occupied = true;
                    break;
                }
                StuckState otherState = stuckStates.get(other.getId());
                if (otherState != null && otherState.waitingOnOuterRing
                        && distanceSquaredXZ(x, z, otherState.waitX, otherState.waitZ)
                        < clearance * clearance) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            NavMeshPoint point = new NavMeshPoint(x, target.getY(), z);
            if (!calculatePath(npc, polygon.id, point, false)) continue;
            npc.setPathTargetPosition(x, z);
            state.waitingOnOuterRing = true;
            state.waitingForSpace = false;
            state.orbitSlot = slot;
            state.nextOrbitTick = tickCount + TACTICAL_ORBIT_TICKS;
            state.orbitProgressX = npc.getX();
            state.orbitProgressZ = npc.getZ();
            state.orbitProgressTick = tickCount;
            state.waitX = x;
            state.waitZ = z;
            state.targetX = target.getX();
            state.targetZ = target.getZ();
            state.nextRetryTick = tickCount + WAIT_RING_RECHECK_TICKS;
            waitRingAssigned++;
            System.out.printf("NPC WAIT RING id=%d slot=%d radius=%.2f%n",
                    npc.getId(), slot, radius);
            return true;
        }
        waitRingFailures++;
        return false;
    }

    /** Reserva logica disponivel em outro jogador, sem roubar vaga existente. */
    private boolean tryRetargetWaitingNpc(NpcItem npc, character.Character current) {
        StuckState state = stuckStates.get(npc.getId());
        if (state == null || tickCount < state.nextRetryTick) return false;
        waitRetargetChecks++;
        character.Character candidate = null;
        float nearest = Float.MAX_VALUE;
        for (character.Character other : characterManager.getCharacters()) {
            if (other == current || !isTargetNavigable(other)
                    || !isInsideDetectionRange(npc, other.getX(), other.getZ())) continue;
            int reserved = 0;
            for (NpcItem member : npcs.values()) {
                if (member.getTarget() == other && member.getState() != NpcState.DEAD
                        && attackSlotOverrides.containsKey(member.getId())) reserved++;
            }
            if (reserved >= ATTACK_SLOT_COUNT) continue;
            float d = distanceSquaredXZ(npc.getX(), npc.getZ(), other.getX(), other.getZ());
            if (d < nearest) { nearest = d; candidate = other; }
        }
        if (candidate == null) return false;
        // Nao transfere slot: este NPC estava no anel, sem permissao.
        npc.clearPath();
        npc.setTarget(candidate);
        stuckStates.remove(npc.getId());
        chaseProgress.put(npc.getId(), new ChaseProgress((float)Math.sqrt(nearest), System.nanoTime()));
        waitRetargets++;
        return true;
    }

    private void updateOuterRingWait(NpcItem npc, character.Character target,
                                     float deltaTime, StuckState state) {
        boolean targetMoved = distanceSquaredXZ(target.getX(), target.getZ(),
                state.targetX, state.targetZ) >= WAIT_RING_WAKE_DISTANCE_SQ;
        if (targetMoved || tickCount >= state.nextRetryTick) {
            waitRingRechecks++;
            // A cada reavaliacao, tenta primeiro voltar ao anel de ataque.
            boolean acquired = acquireAttackSlot(npc, target);
            if (acquired) {
                waitRingWakeups++;
                state.waitingOnOuterRing = false;
                state.x = npc.getX();
                state.z = npc.getZ();
                state.lastCheckTick = tickCount;
                state.nextRetryTick = tickCount + STUCK_RETRY_TICKS;
                return;
            }
            state.nextRetryTick = tickCount + WAIT_RING_RECHECK_TICKS;
            if (targetMoved) {
                // O antigo ponto de espera pode ter ficado longe do novo cerco.
                // Se nao houver novo ponto valido, preserva o caminho anterior.
                if (!assignOuterRingWait(npc, target, state)) {
                    state.targetX = target.getX();
                    state.targetZ = target.getZ();
                }
            }
        }
        if (distanceSquaredXZ(npc.getX(), npc.getZ(), state.waitX, state.waitZ)
                <= WAIT_RING_ARRIVAL_SQ) {
            npc.clearPath();
            if (isTactical(npc) && tickCount >= state.nextOrbitTick) {
                if (assignOuterRingWait(npc, target, state, true)) tacticalOrbitMoves++;
                else {
                    tacticalOrbitFailures++;
                    state.nextOrbitTick = tickCount + TACTICAL_ORBIT_TICKS;
                }
            }
            return;
        }
        if (npc.hasPath()) moveNpc(npc, deltaTime);
        // Sem progresso por um intervalo: procura outro destino, sem ficar
        // preso indefinidamente no mesmo waypoint do anel.
        if (isTactical(npc) && tickCount - state.orbitProgressTick >= STUCK_CHECK_TICKS) {
            float progressed = distanceSquaredXZ(npc.getX(), npc.getZ(),
                    state.orbitProgressX, state.orbitProgressZ);
            state.orbitProgressX = npc.getX();
            state.orbitProgressZ = npc.getZ();
            state.orbitProgressTick = tickCount;
            if (progressed < STUCK_MIN_MOVEMENT_SQ && tickCount >= state.nextOrbitTick) {
                if (assignOuterRingWait(npc, target, state, true)) {
                    orbitStuckRecoveries++;
                    tacticalOrbitMoves++;
                } else {
                    tacticalOrbitFailures++;
                    state.nextOrbitTick = tickCount + TACTICAL_ORBIT_TICKS;
                }
            }
        }
    }

    private boolean shouldRecalculateChasePath(NpcItem npc, character.Character target) {
        if (!npc.hasPath()) return true;
        if (tickCount % PATH_RECALC_INTERVAL_TICKS != 0) return false;
        if (!npc.hasPathTargetPosition()) return true;
        NavMeshPoint destination = isTactical(npc)
                ? getTacticalAttackDestination(npc, target)
                : getAttackSlotDestination(npc, target);
        float near = npc.getAttackRange() + ATTACK_APPROACH_MARGIN;
        if (!isTactical(npc) && distanceSquaredXZ(npc.getX(), npc.getZ(), target.getX(), target.getZ()) <= near * near) {
            destination = new NavMeshPoint(target.getX(), target.getY(), target.getZ());
        }
        return destination != null && distanceSquaredXZ(
                destination.x, destination.z,
                npc.getLastPathTargetX(), npc.getLastPathTargetZ())
                >= PATH_RECALC_DISTANCE * PATH_RECALC_DISTANCE;
    }

    private boolean calculateReturnPath(NpcItem npc) {
        NavMeshPolygon spawnPolygon = navMesh.findContainingPolygon(
                npc.getSpawnX(), npc.getSpawnY(), npc.getSpawnZ());
        if (spawnPolygon == null) {
            return false;
        }
        return calculatePath(npc, spawnPolygon.id,
                new NavMeshPoint(npc.getSpawnX(), npc.getSpawnY(), npc.getSpawnZ()),
                true);
    }

    /** Caminho comum para CHASE e RETURNING: A* -> portais -> Funnel. */
    private boolean calculatePath(NpcItem npc, int destinationPolygonId,
                                  NavMeshPoint destination, boolean returning) {
        int startPolygonId = npc.getCurrentPolygonId();
        if (startPolygonId < 0 || destinationPolygonId < 0) {
            return false;
        }

        long t0 = System.nanoTime();
        List<Integer> polygonPath;
        try {
            polygonPath = AStar.findPath(navMesh, startPolygonId, destinationPolygonId);
        } finally {
            recordAstar(System.nanoTime() - t0);
        }
        if (polygonPath == null || polygonPath.isEmpty()) {
            return false;
        }

        long t1 = System.nanoTime();
        List<NavMeshPortal> portals;
        try {
            portals = navMesh.buildPortals(polygonPath);
        } finally {
            recordPortals(System.nanoTime() - t1);
        }
        if (portals == null || portals.size() != polygonPath.size() - 1) {
            return false;
        }

        NavMeshPoint start = new NavMeshPoint(npc.getX(), npc.getY(), npc.getZ());
        long t2 = System.nanoTime();
        List<NavMeshPoint> waypoints;
        try {
            waypoints = Funnel.findPath(start, destination, portals);
        } finally {
            recordFunnel(System.nanoTime() - t2);
        }
        if (waypoints == null || waypoints.size() < 2) {
            return false;
        }

        npc.setWaypoints(waypoints);
        if (returning) {
            System.out.println("NPC " + npc.getId() + " ReturnPath: Polygons="
                    + polygonPath.size() + " Waypoints=" + waypoints.size());
        }
        return true;
    }

    private void moveNpc(NpcItem npc, float deltaTime) {
        moveNpc(npc, deltaTime, 0.0f);
    }

    // Desvio local experimental: angulos moderados preservam progresso ao waypoint.
    private static final float DETOUR_ANGLE_COS = 0.70710678f;
    private static final float DETOUR_ANGLE_SIN = 0.70710678f;

    private void moveNpc(NpcItem npc, float deltaTime, float stoppingDistance) {
        NavMeshPoint waypoint = npc.getCurrentWaypoint();
        if (waypoint == null) return;

        float dx = waypoint.x - npc.getX();
        float dz = waypoint.z - npc.getZ();
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance <= WAYPOINT_EPSILON) {
            npc.advanceWaypoint();
            return;
        }

        float directionX = dx / distance;
        float directionZ = dz / distance;
        float movement = Math.min(npc.getMovementSpeed() * deltaTime, distance);
        if (stoppingDistance > 0.0f && npc.getTarget() != null) {
            movement = limitMovementByTarget(npc, npc.getTarget(), directionX,
                    directionZ, movement, stoppingDistance);
        }
        if (movement <= MOVEMENT_EPSILON) return;

        float startX = npc.getX();
        float startY = npc.getY();
        float startZ = npc.getZ();
        float factor = movement / distance;
        float newX = startX + dx * factor;
        float newY = startY + (waypoint.y - startY) * factor;
        float newZ = startZ + dz * factor;

        NavMeshPolygon polygon = navMesh.findContainingPolygon(newX, newY, newZ);
        boolean normalMove = polygon != null && !isMovementBlockedByNpc(npc, newX, newZ)
                && !conflictsWithReservedMovement(npc, startX, startZ, newX, newZ);

        if (!normalMove && npc.getState() == NpcState.CHASE
                && attackSlotOverrides.containsKey(npc.getId())
                && npc.getTarget() != null) {
            // Irracionais empurram; inteligentes solicitam passagem.
            // O mesmo mecanismo de deslocamento seguro executa a cessao.
            if (isTactical(npc)) tacticalYieldRequests++;
            boolean yielded = tryBumpAttacker(npc, newX, newZ);
            if (isTactical(npc)) {
                if (yielded) tacticalYieldAccepted++;
                else tacticalYieldRejected++;
            }
        }
        if (!normalMove) {
            // Prefere consistentemente um lado por NPC, reduzindo alternancia por tick.
            int preferredSide = (npc.getId() & 1) == 0 ? 1 : -1;
            boolean found = false;
            for (int attempt = 0; attempt < 2; attempt++) {
                int side = attempt == 0 ? preferredSide : -preferredSide;
                float candidateDirX = directionX * DETOUR_ANGLE_COS
                        - side * directionZ * DETOUR_ANGLE_SIN;
                float candidateDirZ = directionZ * DETOUR_ANGLE_COS
                        + side * directionX * DETOUR_ANGLE_SIN;
                float candidateX = startX + candidateDirX * movement;
                float candidateZ = startZ + candidateDirZ * movement;

                // Nunca ultrapassa o circulo de parada do jogador.
                if (stoppingDistance > 0.0f && npc.getTarget() != null) {
                    float tx = candidateX - npc.getTarget().getX();
                    float tz = candidateZ - npc.getTarget().getZ();
                    if (tx * tx + tz * tz < stoppingDistance * stoppingDistance) continue;
                }
                // O desvio deve aproximar o NPC do waypoint, nao apenas mover de lado.
                if (distanceSquaredXZ(candidateX, candidateZ, waypoint.x, waypoint.z)
                        >= distance * distance) continue;

                // Interpola Y apenas como aproximacao; valida a posicao no NavMesh.
                float candidateY = startY + (waypoint.y - startY)
                        * Math.min(1.0f, movement * DETOUR_ANGLE_COS / distance);
                NavMeshPolygon candidatePolygon = navMesh.findContainingPolygon(
                        candidateX, candidateY, candidateZ);
                if (candidatePolygon == null
                        || isMovementBlockedByNpc(npc, candidateX, candidateZ)
                        || conflictsWithReservedMovement(npc, startX, startZ, candidateX, candidateZ)) continue;

                newX = candidateX;
                newY = candidateY;
                newZ = candidateZ;
                polygon = candidatePolygon;
                found = true;
                break;
            }
            if (!found) return;
        }

        // Nenhuma posição é aplicada durante o planejamento.
        float rotationY = (float) Math.toDegrees(
                Math.atan2(newX - startX, newZ - startZ));
        if (rotationY < 0.0f) rotationY += 360.0f;
        moveIntents.add(new MoveIntent(npc, startX, startZ, newX, newY, newZ,
                rotationY, polygon.id, normalMove && movement >= distance - MOVEMENT_EPSILON));
        intentAccepted++;
        if (isFinalAttackApproach(npc)) softCollisionSteps++;
    }

    /** Empurrao progressivo: desloca o bloqueador primeiro, sem exigir que o
     * perseguidor conclua seu passo no mesmo tick. So aceita ganho de espaco. */
    private boolean tryBumpAttacker(NpcItem chaser, float proposedX, float proposedZ) {
        Long previous = lastBumpTick.get(chaser.getId());
        if (previous != null && tickCount - previous < BUMP_COOLDOWN_TICKS) return false;
        NpcItem blocker = null;
        float best = Float.MAX_VALUE;
        for (NpcItem other : npcs.values()) {
            if (other == chaser || other.getState() != NpcState.ATTACK
                    || other.getTarget() != chaser.getTarget()
                    || !attackSlotOverrides.containsKey(other.getId())) continue;
            float d = distanceSquaredXZ(proposedX, proposedZ, other.getX(), other.getZ());
            float radius = collisionRadiusBetween(chaser, other);
            if (d < radius * radius && d < best) { best = d; blocker = other; }
        }
        if (blocker == null) return false;
        bumpAttempts++;
        lastBumpTick.put(chaser.getId(), tickCount);
        for (MoveIntent intent : moveIntents) {
            if (intent.npc == blocker || intent.npc == chaser) {
                bumpRejectReservation++; bumpRejected++; return false;
            }
        }
        float radialX = blocker.getX() - chaser.getTarget().getX();
        float radialZ = blocker.getZ() - chaser.getTarget().getZ();
        float len = (float)Math.sqrt(radialX * radialX + radialZ * radialZ);
        if (len < 0.001f) { bumpRejectCollision++; bumpRejected++; return false; }
        float tangentX = -radialZ / len, tangentZ = radialX / len;
        float preferred = (chaser.getId() & 1) == 0 ? 1f : -1f;
        boolean navRejected = false, collisionRejected = false, reservationRejected = false;
        // Inteligentes cedem de maneira deliberada; irracionais recebem esbarroes.
        float[] distances = isTactical(blocker)
                ? new float[] {0.30f, 0.42f, BUMP_MAX_DISTANCE, BUMP_DISTANCE}
                : new float[] {BUMP_DISTANCE, 0.30f, 0.42f, BUMP_MAX_DISTANCE};
        float original = distanceSquaredXZ(blocker.getX(), blocker.getZ(), proposedX, proposedZ);
        for (float amount : distances) {
            for (int side = 0; side < 2; side++) {
                float sign = side == 0 ? preferred : -preferred;
                if (isTactical(blocker)) {
                    float ax = blocker.getX() + tangentX * amount * preferred;
                    float az = blocker.getZ() + tangentZ * amount * preferred;
                    float bx = blocker.getX() - tangentX * amount * preferred;
                    float bz = blocker.getZ() - tangentZ * amount * preferred;
                    boolean preferOtherSide = tacticalAngularClearance(blocker, chaser.getTarget(), bx, bz)
                            > tacticalAngularClearance(blocker, chaser.getTarget(), ax, az);
                    if (preferOtherSide) sign = -sign;
                }
                float dx = tangentX * amount * sign;
                float dz = tangentZ * amount * sign;
                float x = blocker.getX() + dx, z = blocker.getZ() + dz;
                // Deve realmente abrir passagem para o passo desejado.
                if (distanceSquaredXZ(x, z, proposedX, proposedZ)
                        <= original + BUMP_CLEARANCE_EPSILON) { collisionRejected = true; continue; }
                NavMeshPolygon polygon = navMesh.findContainingPolygon(x, blocker.getY(), z);
                if (polygon == null) { navRejected = true; continue; }
                if (!isProgressiveBumpSafe(blocker, chaser, x, z)) {
                    collisionRejected = true; continue;
                }
                if (conflictsWithReservedMovement(blocker, blocker.getX(), blocker.getZ(), x, z)) {
                    reservationRejected = true; continue;
                }
                float rot = (float)Math.toDegrees(Math.atan2(dx, dz));
                if (rot < 0f) rot += 360f;
                moveIntents.add(new MoveIntent(blocker, blocker.getX(), blocker.getZ(),
                        x, blocker.getY(), z, rot, polygon.id, false));
                intentAccepted++;
                bumpAccepted++;
                bumpProgressive++;
                if (isTactical(blocker)) intelligentYields++;
                pendingBumpChasers.put(chaser.getId(), (int)tickCount);
                bumpChaserDistances.put(chaser.getId(), distanceSquaredXZ(chaser.getX(),
                        chaser.getZ(), chaser.getTarget().getX(), chaser.getTarget().getZ()));
                return true;
            }
        }
        if (navRejected) bumpRejectNavMesh++;
        if (collisionRejected) bumpRejectCollision++;
        if (reservationRejected) bumpRejectReservation++;
        bumpRejected++;
        return false;
    }

    /** Permite desfazer aglomeracoes existentes, mas nunca aprofundar uma
     * sobreposicao nem atravessar um NPC que ainda estava separado. */
    private boolean isProgressiveBumpSafe(NpcItem blocker, NpcItem chaser, float x, float z) {
        float sx = blocker.getX(), sz = blocker.getZ();
        for (NpcItem other : npcs.values()) {
            if (other == blocker || other.getState() == NpcState.DEAD) continue;
            float radius = collisionRadiusBetween(blocker, other);
            float limitSq = radius * radius;
            float before = distanceSquaredXZ(sx, sz, other.getX(), other.getZ());
            float after = distanceSquaredXZ(x, z, other.getX(), other.getZ());
            if (before < limitSq) {
                if (after + BUMP_CLEARANCE_EPSILON < before) return false;
            } else {
                if (segmentsDistanceSquared(sx, sz, x, z,
                        other.getX(), other.getZ(), other.getX(), other.getZ()) < limitSq)
                    return false;
            }
        }
        return true;
    }

    private boolean isFinalAttackApproach(NpcItem npc) {
        if (npc.getState() != NpcState.CHASE
                || !attackSlotOverrides.containsKey(npc.getId()) || npc.getTarget() == null) return false;
        float range = npc.getAttackRange() + ATTACK_APPROACH_MARGIN;
        return distanceSquaredXZ(npc.getX(), npc.getZ(),
                npc.getTarget().getX(), npc.getTarget().getZ()) <= range * range;
    }

    private float collisionRadiusBetween(NpcItem a, NpcItem b) {
        float radius = a.getAgentRadius() + b.getAgentRadius();
        if (isFinalAttackApproach(a) || isFinalAttackApproach(b)) {
            return radius * ATTACK_APPROACH_RADIUS_MULTIPLIER;
        }
        return radius;
    }

    // Mede progresso real de aproximacao; nao reinicia o prazo por movimento lateral.
    private boolean hasChaseTimedOut(NpcItem npc, character.Character target) {
        float seconds = npc.getDisengageTime(); // Requer getter em NpcItem/NpcData.
        if (seconds <= 0.0f) return false;
        long now = System.nanoTime();
        float distance = (float) Math.sqrt(distanceSquaredXZ(
                npc.getX(), npc.getZ(), target.getX(), target.getZ()));
        ChaseProgress progress = chaseProgress.get(npc.getId());
        if (progress == null) {
            chaseProgress.put(npc.getId(), new ChaseProgress(distance, now));
            return false;
        }
        // Timeout somente quando o alvo ja se afastou da zona de deteccao
        // do spawn. Dentro dela, bloqueio ou posicionamento nao e fuga.
        float spawnDistanceSq = distanceSquaredXZ(target.getX(), target.getZ(),
                npc.getSpawnX(), npc.getSpawnZ());
        float detection = npc.getDetectionRange();
        if (spawnDistanceSq <= detection * detection) {
            progress.closestDistance = distance;
            progress.lastProgressNs = now;
            return false;
        }
        // Janela movel: progresso recente, mesmo apos um desvio, reinicia prazo.
        if (distance <= progress.closestDistance - CHASE_PROGRESS_EPSILON) {
            progress.closestDistance = distance;
            progress.lastProgressNs = now;
        }
        return (now - progress.lastProgressNs) >= (long) (seconds * 1_000_000_000L);
    }

    // Verifica a menor distância entre os dois segmentos XZ percorridos no tick.
    private boolean conflictsWithReservedMovement(NpcItem npc, float ax, float az,
                                                   float bx, float bz) {
        for (MoveIntent intent : moveIntents) {
            if (intent.npc == npc) continue;
            float radius = collisionRadiusBetween(npc, intent.npc);
            if (segmentsDistanceSquared(ax, az, bx, bz,
                    intent.fromX, intent.fromZ, intent.x, intent.z) < radius * radius) {
                intentRejected++;
                return true;
            }
        }
        return false;
    }

    private static float segmentsDistanceSquared(float ax, float az, float bx, float bz,
                                                  float cx, float cz, float dx, float dz) {
        float ux = bx - ax, uz = bz - az;
        float vx = dx - cx, vz = dz - cz;
        float wx = ax - cx, wz = az - cz;
        float a = ux * ux + uz * uz;
        float b = ux * vx + uz * vz;
        float c = vx * vx + vz * vz;
        float d = ux * wx + uz * wz;
        float e = vx * wx + vz * wz;
        float denominator = a * c - b * b;
        float sn, sd = denominator, tn, td = denominator;
        if (denominator < 1e-8f) {
            sn = 0f; sd = 1f; tn = e; td = c;
        } else {
            sn = b * e - c * d;
            tn = a * e - b * d;
            if (sn < 0f) { sn = 0f; tn = e; td = c; }
            else if (sn > sd) { sn = sd; tn = e + b; td = c; }
        }
        if (tn < 0f) {
            tn = 0f;
            if (-d < 0f) sn = 0f;
            else if (-d > a) sn = sd;
            else { sn = -d; sd = a; }
        } else if (tn > td) {
            tn = td;
            if (-d + b < 0f) sn = 0f;
            else if (-d + b > a) sn = sd;
            else { sn = -d + b; sd = a; }
        }
        float sc = Math.abs(sn) < 1e-8f || Math.abs(sd) < 1e-8f ? 0f : sn / sd;
        float tc = Math.abs(tn) < 1e-8f || Math.abs(td) < 1e-8f ? 0f : tn / td;
        float px = wx + sc * ux - tc * vx;
        float pz = wz + sc * uz - tc * vz;
        return px * px + pz * pz;
    }

    /** Colisao circular horizontal entre NPCs, sem fisica 3D.
     *  Versao inicial O(N) por movimento; adequada para teste com poucos NPCs.
     */
    private boolean isMovementBlockedByNpc(NpcItem npc, float newX, float newZ) {
        float radius = npc.getAgentRadius();
        if (radius <= 0.0f) {
            return false;
        }

        float startX = npc.getX();
        float startZ = npc.getZ();
        float stepX = newX - startX;
        float stepZ = newZ - startZ;
        float stepSquared = stepX * stepX + stepZ * stepZ;
        if (stepSquared <= 0.0f) {
            return false;
        }

        for (NpcItem other : npcs.values()) {
            if (other == npc || other.getState() == NpcState.DEAD) {
                continue;
            }
            float combinedRadius = collisionRadiusBetween(npc, other);
            if (combinedRadius <= 0.0f) {
                continue;
            }
            float limitSquared = combinedRadius * combinedRadius;
            float fromX = startX - other.getX();
            float fromZ = startZ - other.getZ();
            float startDistanceSquared = fromX * fromX + fromZ * fromZ;
            float endX = newX - other.getX();
            float endZ = newZ - other.getZ();
            float endDistanceSquared = endX * endX + endZ * endZ;

            // Se ja nasceram sobrepostos, permite somente movimento de afastamento.
            if (startDistanceSquared < limitSquared) {
                if (endDistanceSquared < startDistanceSquared) {
                    return true;
                }
                continue;
            }

            // Distancia minima do centro do outro NPC ao segmento do passo.
            float t = -(fromX * stepX + fromZ * stepZ) / stepSquared;
            t = Math.max(0.0f, Math.min(1.0f, t));
            float nearestX = fromX + t * stepX;
            float nearestZ = fromZ + t * stepZ;
            if (nearestX * nearestX + nearestZ * nearestZ < limitSquared) {
                return true;
            }
        }
        return false;
    }

    /** Corrige sobreposicoes depois de TODOS os estados, inclusive ATTACK e IDLE.
     *  Apenas X/Z; preserva Y. Custo O(N^2): usar somente no teste com poucos NPCs.
     */
    private void resolveNpcOverlaps() {
        List<NpcItem> active = new ArrayList<>(npcs.size());
        for (NpcItem npc : npcs.values()) {
            if (npc.getState() != NpcState.DEAD && npc.getAgentRadius() > 0.0f) {
                active.add(npc);
            }
        }
        // Ordem estavel para que pares com centros identicos tenham direcao deterministica.
        active.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        for (int pass = 0; pass < SEPARATION_PASSES; pass++) {
            for (int i = 0; i < active.size(); i++) {
                NpcItem a = active.get(i);
                for (int j = i + 1; j < active.size(); j++) {
                    NpcItem b = active.get(j);
                    float minDistance = collisionRadiusBetween(a, b);
                    float dx = b.getX() - a.getX();
                    float dz = b.getZ() - a.getZ();
                    float distanceSq = dx * dx + dz * dz;
                    if (distanceSq >= minDistance * minDistance) continue;

                    float distance = (float) Math.sqrt(distanceSq);
                    float nx = distance > COLLISION_EPSILON ? dx / distance : 1.0f;
                    float nz = distance > COLLISION_EPSILON ? dz / distance : 0.0f;
                    float correction = (minDistance - distance + COLLISION_EPSILON) * 0.5f;

                    // Valida cada deslocamento antes de alterar a posicao oficial.
                    boolean movedA = trySeparate(a, -nx * correction, -nz * correction);
                    boolean movedB = trySeparate(b, nx * correction, nz * correction);
                    if (movedA || movedB) separationCorrections++;
                    // Caso apenas um lado consiga se mover, tenta absorver o restante.
                    if (movedA != movedB) {
                        float remaining = correction;
                        if (movedA) trySeparate(a, -nx * remaining, -nz * remaining);
                        else trySeparate(b, nx * remaining, nz * remaining);
                    }
                }
            }
        }
        long remaining = 0;
        for (int i = 0; i < active.size(); i++) {
            NpcItem a = active.get(i);
            for (int j = i + 1; j < active.size(); j++) {
                NpcItem b = active.get(j);
                float r = collisionRadiusBetween(a, b);
                if (distanceSquaredXZ(a.getX(), a.getZ(), b.getX(), b.getZ())
                        < (r - COLLISION_EPSILON) * (r - COLLISION_EPSILON)) remaining++;
            }
        }
        separationRemaining += remaining;
    }

    private boolean trySeparate(NpcItem npc, float dx, float dz) {
        if (dx * dx + dz * dz <= 0.0f) return false;
        float x = npc.getX() + dx;
        float z = npc.getZ() + dz;
        // Esta API verifica o poligono de destino, nao todo o segmento.
        // O teste de separacao usa deslocamentos curtos, mas cantos e paredes
        // ainda exigirao uma validacao de percurso na etapa seguinte.
        NavMeshPolygon polygon = navMesh.findContainingPolygon(x, npc.getY(), z);
        if (polygon == null) return false;
        npc.setPosition(x, npc.getY(), z);
        npc.setCurrentPolygonId(polygon.id);
        syncNpcPosition(npc, x, npc.getY(), z);
        return true;
    }

    /** Interseção geométrica do passo com o círculo de parada do alvo. */
    private float limitMovementByTarget(NpcItem npc, character.Character target,
                                        float directionX, float directionZ,
                                        float maxMovement, float stoppingDistance) {
        float dx = npc.getX() - target.getX();
        float dz = npc.getZ() - target.getZ();
        float radiusSquared = stoppingDistance * stoppingDistance;
        float distanceSquared = dx * dx + dz * dz;
        if (distanceSquared <= radiusSquared) {
            return 0.0f;
        }

        float b = 2.0f * (dx * directionX + dz * directionZ);
        float c = distanceSquared - radiusSquared;
        float discriminant = b * b - 4.0f * c;
        if (discriminant < 0.0f) {
            return maxMovement;
        }

        float entryDistance = (-b - (float) Math.sqrt(discriminant)) * 0.5f;
        return entryDistance >= 0.0f && entryDistance <= maxMovement
                ? entryDistance : maxMovement;
    }

    private boolean isInsideDetectionRange(NpcItem npc, float x, float z) {
        float range = npc.getDetectionRange();
        return distanceSquaredXZ(npc.getX(), npc.getZ(), x, z) <= range * range;
    }

    private boolean isTargetNavigable(character.Character target) {
        return target != null && target.getCurrentPolygonId() >= 0;
    }

    private boolean exceededChaseDistance(NpcItem npc) {
        float range = npc.getMaxChaseDistance();
        return distanceSquaredXZ(npc.getX(), npc.getZ(),
                npc.getSpawnX(), npc.getSpawnZ()) > range * range;
    }

    private static float distanceSquaredXZ(float x1, float z1, float x2, float z2) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        return dx * dx + dz * dz;
    }

    // ---------------------------------------------------------------------
    // Sincronização SmartFox
    // ---------------------------------------------------------------------

    private void syncNpcPosition(NpcItem npc, float x, float y, float z) {
        // Usado ao concluir RETURNING; não altera a rotação atual do NPC.
        syncNpcPosition(npc, x, y, z, null);
    }

    private void syncNpcPosition(NpcItem npc, float x, float y, float z, Float rotationY) {
        long t0 = System.nanoTime();
        try {
            SmartFoxServer.getInstance().getAPIManager().getMMOApi()
                    .setMMOItemPosition(npc, new Vec3D(x, y, z), room);
        } finally {
            recordSpatial(System.nanoTime() - t0);
        }
        // Uma única atualização de variáveis para x/y/z/rot durante movimento.
        List<IMMOItemVariable> variables = new ArrayList<>(4);
        variables.add(new MMOItemVariable("x", x));
        variables.add(new MMOItemVariable("y", y));
        variables.add(new MMOItemVariable("z", z));
        if (rotationY != null) {
            variables.add(new MMOItemVariable("rot", rotationY));
        }
        setNpcVariables(npc, variables, SyncKind.MOVEMENT);
    }

    private enum SyncKind { MOVEMENT, ATTACK }

    private void setNpcVariables(NpcItem npc, List<IMMOItemVariable> variables, SyncKind kind) {
        long t0 = System.nanoTime();
        try {
            SmartFoxServer.getInstance().getAPIManager().getMMOApi()
                    .setMMOItemVariables(npc, variables);
        } finally {
            long elapsed = System.nanoTime() - t0;
            recordSync(elapsed);
            switch (kind) {
                case MOVEMENT: recordCoordinates(elapsed); break;
                case ATTACK: recordAttackSync(elapsed); break;
            }
        }
    }

    private void executeAttack(NpcItem npc) {
        npc.incrementAttackSequence();
        setNpcVariables(npc, Arrays.asList(
                new MMOItemVariable("npcId", npc.getId()),
                new MMOItemVariable("attackSequence", npc.getAttackSequence())), SyncKind.ATTACK);
    }
    private void recordSpatial(long ns) {
        recordSync(ns);
        spatialNs += ns; spatialCalls++; maxSpatialNs = Math.max(maxSpatialNs, ns);
    }

    private void recordCoordinates(long ns) {
        coordinatesNs += ns; coordinatesCalls++; maxCoordinatesNs = Math.max(maxCoordinatesNs, ns);
    }

    private void recordRotation(long ns) {
        rotationNs += ns; rotationCalls++; maxRotationNs = Math.max(maxRotationNs, ns);
    }

    private void recordAttackSync(long ns) {
        attackSyncNs += ns; attackSyncCalls++; maxAttackSyncNs = Math.max(maxAttackSyncNs, ns);
    }

    private void recordAstar(long ns) {
        astarNs += ns; astarCalls++; maxAstarNs = Math.max(maxAstarNs, ns);
    }

    private void recordPortals(long ns) {
        portalsNs += ns; portalsCalls++; maxPortalsNs = Math.max(maxPortalsNs, ns);
    }

    private void recordFunnel(long ns) {
        funnelNs += ns; funnelCalls++; maxFunnelNs = Math.max(maxFunnelNs, ns);
    }

    private void recordSync(long ns) {
        syncNs += ns; syncCalls++; maxSyncNs = Math.max(maxSyncNs, ns);
    }

    private static double ms(long ns) { return ns / 1_000_000.0; }

    private static void printMetric(String label, long calls, long totalNs, long maxNs) {
        System.out.printf("  %-12s calls=%d total=%.3f ms avg=%.4f ms max=%.3f ms%n",
                label, calls, ms(totalNs), calls == 0 ? 0 : ms(totalNs) / calls, ms(maxNs));
    }

    private void printPerformance() {
        System.out.printf("NPC PERF ticks=%d NPCs=%d updateAvg=%.3f ms updateMax=%.3f ms%n",
                PERF_INTERVAL_TICKS, npcs.size(), ms(totalUpdateNs) / PERF_INTERVAL_TICKS,
                ms(maxUpdateNs));
        printMetric("AStar", astarCalls, astarNs, maxAstarNs);
        printMetric("Portals", portalsCalls, portalsNs, maxPortalsNs);
        printMetric("Funnel", funnelCalls, funnelNs, maxFunnelNs);
        printMetric("SmartFox", syncCalls, syncNs, maxSyncNs);
        printMetric("  Spatial", spatialCalls, spatialNs, maxSpatialNs);
        printMetric("  XYZ+Rot", coordinatesCalls, coordinatesNs, maxCoordinatesNs);
        printMetric("  Attack", attackSyncCalls, attackSyncNs, maxAttackSyncNs);
        System.out.printf("  MoveIntent accepted=%d reservationRejects=%d%n", intentAccepted, intentRejected);
        System.out.printf("  Tactical yieldRequests=%d yieldAccepted=%d yieldRejected=%d orbitStuckRecoveries=%d waitRetargets=%d waitRetargetChecks=%d%n",
                tacticalYieldRequests, tacticalYieldAccepted, tacticalYieldRejected,
                orbitStuckRecoveries, waitRetargets, waitRetargetChecks);
        System.out.printf("  Tactical attackRepositions=%d%n", tacticalAttackRepositions);
        tacticalAttackRepositions = 0;
        System.out.printf("  Tactical sectors selected=%d failed=%d switched=%d%n",
                tacticalSectorSelections, tacticalSectorFailures, tacticalSectorSwitches);
        tacticalSectorSelections = tacticalSectorFailures = tacticalSectorSwitches = 0;
        System.out.printf("  Tactical orbitMoves=%d orbitFailures=%d repositions=%d%n",
                tacticalOrbitMoves, tacticalOrbitFailures, tacticalRepositions);
        tacticalOrbitMoves = tacticalOrbitFailures = tacticalRepositions = 0;
        System.out.printf("  Bump attempts=%d accepted=%d rejected=%d%n",
                bumpAttempts, bumpAccepted, bumpRejected);
        System.out.printf("  Bump rejectNavMesh=%d rejectCollision=%d rejectReservation=%d progressive=%d intelligentYields=%d recoveredChasers=%d%n",
                bumpRejectNavMesh, bumpRejectCollision, bumpRejectReservation,
                bumpProgressive, intelligentYields, bumpRecoveredChasers);
        bumpAttempts = bumpAccepted = bumpRejected = 0;
        bumpRejectNavMesh = bumpRejectCollision = bumpRejectReservation = 0;
        bumpProgressive = intelligentYields = bumpRecoveredChasers = 0;
        System.out.printf("  Combat disengagedTimeout=%d softApproachSteps=%d%n",
                disengagedByTimeout, softCollisionSteps);
        disengagedByTimeout = softCollisionSteps = 0;
        intentAccepted = intentRejected = 0;
        System.out.printf("  FixedSlots assigned=%d released=%d full=%d active=%d%n",
                fixedSlotAssigned, fixedSlotReleased, fixedSlotFull, attackSlotOverrides.size());
        fixedSlotAssigned = fixedSlotReleased = fixedSlotFull = 0;
        System.out.printf("  Stuck detected=%d slotChanges=%d slotFailures=%d%n",
                stuckDetected, slotChanges, slotChangeFailures);
        System.out.printf("  WaitRing assigned=%d failures=%d wakeups=%d rechecks=%d%n",
                waitRingAssigned, waitRingFailures, waitRingWakeups, waitRingRechecks);
        waitRingAssigned = waitRingFailures = waitRingWakeups = waitRingRechecks = 0;
        System.out.printf("  Waiting saturated=%d rechecks=%d blockedRecoveries=%d%n",
                saturatedWaits, saturatedRechecks, blockedRecoveries);
        saturatedWaits = saturatedRechecks = blockedRecoveries = 0;
        System.out.printf("  Slot rejects candidates=%d reserved=%d navmesh=%d occupied=%d path=%d same=%d smallGroup=%d%n",
                slotCandidatesEvaluated, slotRejectedReserved, slotRejectedNavMesh,
                slotRejectedOccupied, slotRejectedPath, slotRejectedSame, slotRejectedSmallGroup);
        stuckDetected = slotChanges = slotChangeFailures = 0;
        slotCandidatesEvaluated = slotRejectedReserved = slotRejectedNavMesh = 0;
        slotRejectedOccupied = slotRejectedPath = slotRejectedSame = slotRejectedSmallGroup = 0;
        System.out.printf("  Separation total=%.3f ms corrections=%d remainingPairsSum=%d%n",
                ms(separationNs), separationCorrections, separationRemaining);
        separationNs = separationCorrections = separationRemaining = 0;
        totalUpdateNs = maxUpdateNs = 0;
        astarNs = funnelNs = portalsNs = syncNs = 0;
        astarCalls = funnelCalls = portalsCalls = syncCalls = 0;
        maxAstarNs = maxFunnelNs = maxPortalsNs = maxSyncNs = 0;
        spatialNs = coordinatesNs = rotationNs = attackSyncNs = 0;
        spatialCalls = coordinatesCalls = rotationCalls = attackSyncCalls = 0;
        maxSpatialNs = maxCoordinatesNs = maxRotationNs = maxAttackSyncNs = 0;
    }
    
    private void debugNpcDistances() {
        List<NpcItem> list = new ArrayList<>(npcs.values());

        float minDistance = Float.MAX_VALUE;
        int overlappingPairs = 0;
        int nearbyPairs = 0;

        for (int i = 0; i < list.size(); i++) {
            NpcItem a = list.get(i);

            for (int j = i + 1; j < list.size(); j++) {
                NpcItem b = list.get(j);

                float dx = a.getX() - b.getX();
                float dz = a.getZ() - b.getZ();
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                float minAllowed = a.getAgentRadius()
                        + b.getAgentRadius();

                minDistance = Math.min(minDistance, distance);

                if (distance < minAllowed - 0.0001f) {
                    overlappingPairs++;
                }

                if (distance < 1.0f) {
                    nearbyPairs++;
                }
            }
        }

        System.out.printf(
            "NPC DISTANCES min=%.3f overlaps=%d nearby=%d%n",
            list.size() < 2 ? 0.0f : minDistance,
            overlappingPairs,
            nearbyPairs
        );
    }

}
