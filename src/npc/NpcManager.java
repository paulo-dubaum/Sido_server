package npc;

import navmesh.Funnel;
import navmesh.NavMeshPoint;
import navmesh.NavMeshPolygon;
import navmesh.NavMeshPortal;
import world.AStar;
import world.ServerNavMesh;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.mmo.IMMOItemVariable;
import com.smartfoxserver.mmo.MMOItemVariable;
import com.smartfoxserver.mmo.MMORoom;
import com.smartfoxserver.mmo.Vec3D;

import character.CharacterManager;

public class NpcManager {

	private final MMORoom room;
    private final ServerNavMesh navMesh;
    private final CharacterManager characterManager;
    private long tickCount = 0;

    /*
     * Chave = MMOItem ID.
     *
     * Por enquanto usamos o ID gerado pelo SmartFox
     * para localizar a instância em runtime.
     */
    private final Map<Integer, NpcItem> npcs =
            new HashMap<>();
    
    private static final float PATH_RECALC_DISTANCE = 2.0f;
    private static final int PATH_RECALC_INTERVAL_TICKS = 10;
    


    public NpcManager(
            MMORoom room,
            ServerNavMesh navMesh,
            CharacterManager characterManager) {

        this.room = room;
        this.navMesh = navMesh;
        this.characterManager = characterManager;
    }

    // =========================================================
    // SPAWN
    // =========================================================

    public NpcItem spawnNpc(
            int npcTypeId,
            float x,
            float y,
            float z,
            float detectionRange) {

        // ---------------------------------------------
        // 1. Validar posição no NavMesh
        // ---------------------------------------------

        NavMeshPolygon polygon =
                navMesh.findContainingPolygon(
                        x,
                        y,
                        z
                );
        
        

        if (polygon == null) {

            System.out.println(
                    "NpcManager: spawn recusado. "
                            + "Posição fora do NavMesh: "
                            + x + ", "
                            + y + ", "
                            + z
            );

            return null;
        }

        // ---------------------------------------------
        // 2. Criar NPC
        // ---------------------------------------------

        NpcItem npc =
                new NpcItem(
                        npcTypeId,
                        x,
                        y,
                        z,
                        detectionRange
                );

        npc.setCurrentPolygonId(
                polygon.id
        );

        // ---------------------------------------------
        // 3. Registrar no SmartFox / MMORoom
        // ---------------------------------------------

        SmartFoxServer
                .getInstance()
                .getAPIManager()
                .getMMOApi()
                .setMMOItemPosition(
                        npc,
                        new Vec3D(
                                x,
                                y,
                                z
                        ),
                        room
                );

        // ---------------------------------------------
        // 4. Registrar no nosso manager
        // ---------------------------------------------

        npcs.put(
                npc.getId(),
                npc
        );

        System.out.println(
                "NpcManager: NPC criado."
                        + " ID=" + npc.getId()
                        + " Type=" + npcTypeId
                        + " Polygon=" + polygon.id
                        + " Range=" + detectionRange
        );

        return npc;
    }
    


    public void update() {

        tickCount++;

        for (NpcItem npc : npcs.values()) {

            updateNpc(npc);
        }
    }

    private void updateNpc(NpcItem npc) {

        switch (npc.getState()) {

            case IDLE:
                updateIdle(npc);
                break;

            case CHASE:
            	updateChase(npc);
                break;

            case ATTACK:
                updateAttack(npc);
                break;

            case RETURNING:
            	updateReturning(npc);
                break;

            case DEAD:
                break;
                
            case PATROL:
				break;
        }
    }
    
    private void updateReturning(NpcItem npc) {

        float dx = npc.getSpawnX() - npc.getX();
        float dz = npc.getSpawnZ() - npc.getZ();

        float distanceSquared = dx * dx + dz * dz;

        // Consideramos que chegou ao spawn
        // quando estiver a 0.2 metro dele.
        if (distanceSquared <= 0.04f) {

            npc.setPosition(
                    npc.getSpawnX(),
                    npc.getSpawnY(),
                    npc.getSpawnZ()
            );

            npc.clearPath();
            npc.setState(NpcState.IDLE);

            System.out.println(
                    "NPC " + npc.getId()
                    + " retornou ao spawn."
            );

            return;
        }

        // Planeja somente se ainda não tiver caminho.
        if (!npc.hasPath()) {

            if (!calculateReturnPath(npc)) {
                return;
            }
        }

        // Reutiliza o movimento existente.
        moveNpc(npc, 0.1f);
    }

	private boolean isInsideDetectionRange(
            NpcItem npc,
            float targetX,
            float targetZ) {

        float dx =
                targetX - npc.getX();

        float dz =
                targetZ - npc.getZ();

        float distanceSquared =
                dx * dx +
                dz * dz;

        float range =
                npc.getDetectionRange();

        return distanceSquared <=
                range * range;
    }
	
	private boolean calculateReturnPath(NpcItem npc) {

	    int startPolygonId = npc.getCurrentPolygonId();

	    NavMeshPolygon spawnPolygon =
	            navMesh.findContainingPolygon(
	                    npc.getSpawnX(),
	                    npc.getSpawnY(),
	                    npc.getSpawnZ()
	            );

	    if (startPolygonId < 0 || spawnPolygon == null) {
	        return false;
	    }

	    List<Integer> path =
	            AStar.findPath(
	                    navMesh,
	                    startPolygonId,
	                    spawnPolygon.id
	            );

	    if (path.isEmpty()) {
	        return false;
	    }

	    List<NavMeshPortal> portals =
	            navMesh.buildPortals(path);

	    if (portals.size() != path.size() - 1) {
	        return false;
	    }

	    NavMeshPoint start =
	            new NavMeshPoint(
	                    npc.getX(),
	                    npc.getY(),
	                    npc.getZ()
	            );

	    NavMeshPoint end =
	            new NavMeshPoint(
	                    npc.getSpawnX(),
	                    npc.getSpawnY(),
	                    npc.getSpawnZ()
	            );

	    List<NavMeshPoint> waypoints =
	            Funnel.findPath(start, end, portals);

	    npc.setWaypoints(waypoints);

	    System.out.println(
	            "NPC " + npc.getId()
	            + " ReturnPath:"
	            + " Polygons=" + path.size()
	            + " Waypoints=" + waypoints.size()
	    );

	    return true;
	}

    private void updateIdle(NpcItem npc) {
    	
    	if (npc.hasTarget()) {
            return;
        }

        for (character.Character character
                : characterManager.getCharacters()) {

            boolean detected =
                    isInsideDetectionRange(
                            npc,
                            character.getX(),
                            character.getZ()
                    );

            if (!detected) {
                continue;
            }
            
            if (character.getCurrentPolygonId() < 0) {
                continue;
            }

            npc.setTarget(character);
            
            
            npc.setState(NpcState.CHASE);

            System.out.println(
                    "NPC "
                            + npc.getId()
                            + " adquiriu Character "
                            + character.getCharacterId()
                            + " como alvo."
            );

            return;
        }
    }
    
    private boolean isTargetNavigable(character.Character target) {

        return target != null
                && target.getCurrentPolygonId() >= 0;
    }
    
    private void updateChase(NpcItem npc) {

        character.Character target = npc.getTarget();
        


        if (!isTargetNavigable(target)) {

            System.out.println(
                    "NPC " + npc.getId()
                    + " perdeu alvo fora do NavMesh."
            );

            startReturning(npc);
            return;
        }

        // =====================================================
        // 1. VALIDAR ALVO
        // =====================================================

        if (target == null) {

            npc.clearPath();
            npc.setState(NpcState.IDLE);

            return;
        }
        
        if (exceededChaseDistance(npc)) {

            startReturning(npc);
            return;
        }
        

        if (!isTargetNavigable(target)) {

            System.out.println(
                    "NPC " + npc.getId()
                    + " perdeu alvo fora do NavMesh."
            );

            startReturning(npc);
            return;
        }

        // =====================================================
        // 2. VERIFICAR DISTÂNCIA DE ATAQUE
        // =====================================================

        float dx = target.getX() - npc.getX();
        float dz = target.getZ() - npc.getZ();

        float distanceSquared = dx * dx + dz * dz;

        float attackRange = npc.getAttackRange();

        float preferredDistance = npc.getPreferredAttackDistance();

        float attackEnterDistance = preferredDistance + 0.05f;

        if (distanceSquared <= attackEnterDistance * attackEnterDistance) {

            npc.clearPath();
            npc.setState(NpcState.ATTACK);

            npc.setNextAttackTimeMs(
                    System.currentTimeMillis()
                            + npc.getAttackIntervalMs()
            );

            System.out.println(
                    "NPC " + npc.getId()
                    + " entrou em ATTACK. Distance="
                    + Math.sqrt(distanceSquared)
            );

            return;
        }
        

     

        // =====================================================
        // 3. VERIFICAR NECESSIDADE DE RECÁLCULO
        // =====================================================

        boolean shouldRecalculate = !npc.hasPath();

        if (!shouldRecalculate
                && tickCount % PATH_RECALC_INTERVAL_TICKS == 0) {

            if (!npc.hasPathTargetPosition()) {

                shouldRecalculate = true;

            } else {

                float targetDx = target.getX()
                        - npc.getLastPathTargetX();

                float targetDz = target.getZ()
                        - npc.getLastPathTargetZ();

                float targetDistanceSquared =
                        targetDx * targetDx
                        + targetDz * targetDz;

                if (targetDistanceSquared
                        >= PATH_RECALC_DISTANCE
                        * PATH_RECALC_DISTANCE) {

                    shouldRecalculate = true;
                }
            }
        }

        // =====================================================
        // 4. RECALCULAR CAMINHO QUANDO NECESSÁRIO
        // =====================================================

        if (shouldRecalculate) {

            int startPolygonId =
                    npc.getCurrentPolygonId();

            int targetPolygonId =
                    target.getCurrentPolygonId();

            if (startPolygonId >= 0
                    && targetPolygonId >= 0) {

                List<Integer> path =
                        AStar.findPath(
                                navMesh,
                                startPolygonId,
                                targetPolygonId
                        );

                if (!path.isEmpty()) {

                    List<NavMeshPortal> portals =
                            navMesh.buildPortals(path);

                    if (portals.size() == path.size() - 1) {

                        NavMeshPoint start =
                                new NavMeshPoint(
                                        npc.getX(),
                                        npc.getY(),
                                        npc.getZ()
                                );

                        NavMeshPoint end =
                                new NavMeshPoint(
                                        target.getX(),
                                        target.getY(),
                                        target.getZ()
                                );

                        List<NavMeshPoint> waypoints =
                                Funnel.findPath(
                                        start,
                                        end,
                                        portals
                                );

                        if (waypoints.size() >= 2) {

                            npc.setWaypoints(waypoints);

                            npc.setPathTargetPosition(
                                    end.x,
                                    end.z
                            );

                            System.out.println(
                                    "NPC " + npc.getId()
                                    + " recalculou caminho."
                                    + " Polygons=" + path.size()
                                    + " Waypoints=" + waypoints.size()
                                    + " Target=("
                                    + end.x + ", " + end.z + ")"
                            );
                        }
                    }
                }
            }
        }

        // =====================================================
        // 5. CONTINUAR MOVIMENTO
        // =====================================================

        if (tickCount % 10 == 0) {

            dx = target.getX() - npc.getX();
            dz = target.getZ() - npc.getZ();

            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            System.out.println(
                    "NPC " + npc.getId()
                    + " CHASE DEBUG"
                    + " Distance=" + distance
                    + " HasPath=" + npc.hasPath()
                    + " WaypointIndex=" + npc.getCurrentWaypointIndex()
            );
        }
        
        if (npc.hasPath()) {
        	moveNpc(
        	        npc,
        	        0.1f,
        	        npc.getPreferredAttackDistance()
        	);
        }
    }
    
    private void updateAttack(NpcItem npc) {

        character.Character target = npc.getTarget();

        // 1. Alvo inexistente
        if (target == null) {
            startReturning(npc);
            return;
        }

        // 2. Alvo fora do NavMesh
        if (target.getCurrentPolygonId() < 0) {
            startReturning(npc);
            return;
        }

        // 3. NPC ultrapassou limite de perseguição
        if (exceededChaseDistance(npc)) {
            startReturning(npc);
            return;
        }
        
        

        // 4. Distância até o personagem
        float dx = target.getX() - npc.getX();
        float dz = target.getZ() - npc.getZ();

        float distanceSquared = dx * dx + dz * dz;
        
        // teste
        long now = System.currentTimeMillis();

        if (tickCount % 10 == 0) {

            System.out.println(
                    "NPC " + npc.getId()
                    + " ATTACK DEBUG"
                    + " Distance=" + Math.sqrt(distanceSquared)
                    + " Now=" + now
                    + " NextAttack=" + npc.getNextAttackTimeMs()
                    + " Remaining="
                    + (npc.getNextAttackTimeMs() - now)
            );
        }

        if (now < npc.getNextAttackTimeMs()) {
            return;
        }

        executeAttack(npc, target);

        npc.setNextAttackTimeMs(
                now + npc.getAttackIntervalMs()
        );
        
        
        
        // fim teste

        // 5. Verificar se precisa voltar a perseguir
        float resumeRange = npc.getChaseResumeRange();

        if (distanceSquared > resumeRange * resumeRange) {

            npc.setState(NpcState.CHASE);

            System.out.println(
                    "NPC " + npc.getId()
                    + " voltou para CHASE."
            );

            return;
        }
        
        // teste
        if (distanceSquared > resumeRange * resumeRange) {

            System.out.println(
                    "NPC " + npc.getId()
                    + " voltou para CHASE."
                    + " Distance=" + Math.sqrt(distanceSquared)
            );

            npc.setState(NpcState.CHASE);
            return;
        } 
        
        
        float attackRange = npc.getAttackRange();

        if (distanceSquared > attackRange * attackRange) {
            return;
        }
        // fim teste

        // 6. Só executar ataque dentro do alcance real


        if (distanceSquared > attackRange * attackRange) {
            return;
        }

        // 7. Cooldown
        //long now = System.currentTimeMillis();

        if (now < npc.getNextAttackTimeMs()) {
            return;
        }

        // 8. Executar ataque
        executeAttack(npc, target);

        // 9. Programar próximo ataque
        npc.setNextAttackTimeMs(
                now + npc.getAttackIntervalMs()
        );
    }
    
    private void moveNpc(
            NpcItem npc,
            float deltaTime,
            float stoppingDistance
    ) {
        NavMeshPoint waypoint = npc.getCurrentWaypoint();

        if (waypoint == null) {
            return;
        }

        float dx = waypoint.x - npc.getX();
        float dz = waypoint.z - npc.getZ();

        float distance = (float) Math.sqrt(
                dx * dx + dz * dz
        );

        if (distance <= 0.0001f) {
            npc.advanceWaypoint();
            return;
        }

        float directionX = dx / distance;
        float directionZ = dz / distance;

        float movement = Math.min(
                npc.getMovementSpeed() * deltaTime,
                distance
        );

        // Aplicar distância de parada apenas na perseguição.
        if (stoppingDistance > 0.0f
                && npc.getTarget() != null) {

            movement = limitMovementByTarget(
                    npc,
                    npc.getTarget(),
                    directionX,
                    directionZ,
                    movement,
                    stoppingDistance
            );
        }

        if (movement <= 0.00001f) {
            return;
        }

        float factor = movement / distance;

        float newX = npc.getX() + dx * factor;
        float newY = npc.getY()
                + (waypoint.y - npc.getY()) * factor;
        float newZ = npc.getZ() + dz * factor;

        npc.setPosition(newX, newY, newZ);

        if (movement >= distance - 0.00001f) {
            npc.advanceWaypoint();
        }

        NavMeshPolygon polygon =
                navMesh.findContainingPolygon(newX, newY, newZ);

        if (polygon != null) {
            npc.setCurrentPolygonId(polygon.id);
        }

     // Atualiza a posição no servidor.
        npc.setPosition(newX, newY, newZ);
        
        float rotationY = (float) Math.toDegrees(
                Math.atan2(directionX, directionZ)
        );

        if (rotationY < 0.0f) {
            rotationY += 360.0f;
        }
        
     // Sincroniza com o sistema espacial do SmartFox.
        SmartFoxServer.getInstance()
                .getAPIManager()
                .getMMOApi()
                .setMMOItemPosition(
                        npc,
                        new Vec3D(newX, newY, newZ),
                        room
                );
        
        List<IMMOItemVariable> variables = Arrays.asList(
                new MMOItemVariable("x", newX),
                new MMOItemVariable("y", newY),
                new MMOItemVariable("z", newZ),
                new MMOItemVariable("rot", rotationY)
        );
         
        SmartFoxServer.getInstance()
        .getAPIManager()
        .getMMOApi().setMMOItemVariables(npc, variables);
    }
        
    
    private void moveNpc(NpcItem npc, float deltaTime) {

        NavMeshPoint waypoint =
                npc.getCurrentWaypoint();

        if (waypoint == null) {
            return;
        }

        float dx = waypoint.x - npc.getX();
        float dz = waypoint.z - npc.getZ();

        float distance = (float) Math.sqrt(
                dx * dx + dz * dz
        );

        float maxMovement =
                npc.getMovementSpeed() * deltaTime;

        float newX;
        float newY;
        float newZ;

        if (distance <= maxMovement) {

            newX = waypoint.x;
            newY = waypoint.y;
            newZ = waypoint.z;

            npc.advanceWaypoint();

        } else {

            float factor = maxMovement / distance;

            newX = npc.getX() + dx * factor;
            newZ = npc.getZ() + dz * factor;

            // Interpolação temporária da altura.
            newY = npc.getY()
                    + (waypoint.y - npc.getY()) * factor;
        }

        // Atualiza a posição no servidor.
        npc.setPosition(newX, newY, newZ);
        
        SmartFoxServer.getInstance()
        .getAPIManager()
        .getMMOApi()
        .setMMOItemPosition(
                npc,
                new Vec3D(
                        npc.getSpawnX(),
                        npc.getSpawnY(),
                        npc.getSpawnZ()
                ),
                room
        );
        
        NavMeshPolygon polygon =
                navMesh.findContainingPolygon(
                        newX,
                        newY,
                        newZ
                );
        
        
        
        

        if (polygon != null) {
            npc.setCurrentPolygonId(polygon.id);
        }
        
        if (tickCount % 10 == 0) {

            System.out.println(
                    "NPC "
                            + npc.getId()
                            + " Position=("
                            + npc.getX() + ", "
                            + npc.getY() + ", "
                            + npc.getZ() + ")"
                            + " Polygon="
                            + npc.getCurrentPolygonId()
                            + " WaypointIndex="
                            + npc.getCurrentWaypointIndex()
            );
        }

        // Sincroniza com o sistema espacial do SmartFox.
        SmartFoxServer.getInstance()
                .getAPIManager()
                .getMMOApi()
                .setMMOItemPosition(
                        npc,
                        new Vec3D(newX, newY, newZ),
                        room
                );
        
        List<IMMOItemVariable> variables = Arrays.asList(
                new MMOItemVariable("x", newX),
                new MMOItemVariable("y", newY),
                new MMOItemVariable("z", newZ)
        );
        
        SmartFoxServer.getInstance()
        .getAPIManager()
        .getMMOApi().setMMOItemVariables(npc, variables);
    }
    
    private void executeAttack(NpcItem npc, character.Character target) {

        npc.incrementAttackSequence();
        
       
        List<IMMOItemVariable> variables = Arrays.asList(
        		new MMOItemVariable("npcId", npc.getId()),
				new MMOItemVariable("attackSequence", npc.getAttackSequence())
		);
        
        SmartFoxServer.getInstance()
				.getAPIManager()
				.getMMOApi()
				.setMMOItemVariables(npc, variables);
        
        System.out.println(
                "NPC " + npc.getId()
                + " AttackSequence="
                + npc.getAttackSequence()
        );
    }
    
    private void startReturning(NpcItem npc) {

        npc.setTarget(null);
        npc.clearPath();
        npc.setState(NpcState.RETURNING);

        System.out.println(
                "NPC " + npc.getId()
                + " iniciou RETURNING."
        );
    }
    
    private float limitMovementByTarget(
            NpcItem npc,
            character.Character target,
            float directionX,
            float directionZ,
            float maxMovement,
            float stoppingDistance
    ) {
        float dx = npc.getX() - target.getX();
        float dz = npc.getZ() - target.getZ();

        float radiusSquared = stoppingDistance * stoppingDistance;
        float distanceSquared = dx * dx + dz * dz;

        // Já está dentro do raio de parada.
        if (distanceSquared <= radiusSquared) {
            return 0.0f;
        }

        // Movimento normal não alcança o raio.
        float nextX = dx + directionX * maxMovement;
        float nextZ = dz + directionZ * maxMovement;

        // Resolver a interseção entre o segmento de movimento
        // e o círculo de parada.
        float b = 2.0f * (
                dx * directionX + dz * directionZ
        );

        float c = distanceSquared - radiusSquared;

        float discriminant = b * b - 4.0f * c;

        if (discriminant < 0.0f) {
            return maxMovement;
        }

        float sqrt = (float) Math.sqrt(discriminant);

        float entryDistance = (-b - sqrt) * 0.5f;

        if (entryDistance >= 0.0f
                && entryDistance <= maxMovement) {

            return entryDistance;
        }

        return maxMovement;
    }
    
    private boolean exceededChaseDistance(NpcItem npc) {

        float dx = npc.getX() - npc.getSpawnX();
        float dz = npc.getZ() - npc.getSpawnZ();

        float distanceSquared = dx * dx + dz * dz;

        float maxDistance = npc.getMaxChaseDistance();

        return distanceSquared > maxDistance * maxDistance;
    }
    

    public void onCharacterDisconnected(character.Character character) {

        for (NpcItem npc : npcs.values()) {

            if (npc.getTarget() != character) {
                continue;
            }

            npc.setTarget(null);
            npc.clearPath();
            npc.setState(NpcState.IDLE);

            System.out.println(
                    "NPC " + npc.getId()
                    + " perdeu Character "
                    + character.getCharacterId()
                    + " por desconexão."
            );
        }
    }
    // =========================================================
    // GET
    // =========================================================

    public NpcItem getNpc(int mmoItemId) {

        return npcs.get(
                mmoItemId
        );
    }

    // =========================================================
    // GET ALL
    // =========================================================

    public Collection<NpcItem> getNpcs() {

        return Collections.unmodifiableCollection(
                npcs.values()
        );
    }

    // =========================================================
    // COUNT
    // =========================================================

    public int getNpcCount() {

        return npcs.size();
    }

    // =========================================================
    // REMOVE
    // =========================================================

    public boolean removeNpc(
            int mmoItemId) {

        NpcItem npc =
                npcs.remove(
                        mmoItemId
                );

        if (npc == null)
            return false;

        SmartFoxServer
                .getInstance()
                .getAPIManager()
                .getMMOApi()
                .removeMMOItem(
                        npc
                );

        System.out.println(
                "NpcManager: NPC removido."
                        + " ID=" + mmoItemId
        );

        return true;
    }
}
