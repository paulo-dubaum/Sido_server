package npc;

import navmesh.NavMeshPolygon;
import world.ServerNavMesh;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.mmo.MMORoom;
import com.smartfoxserver.mmo.Vec3D;

public class NpcManager {

	private final MMORoom room;
    private final ServerNavMesh navMesh;
    private long tickCount = 0;

    /*
     * Chave = MMOItem ID.
     *
     * Por enquanto usamos o ID gerado pelo SmartFox
     * para localizar a instância em runtime.
     */
    private final Map<Integer, NpcItem> npcs =
            new HashMap<>();

    public NpcManager(
    		MMORoom room,
            ServerNavMesh navMesh) {

        this.room = room;
        this.navMesh = navMesh;
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

        // Debug temporário
        if (tickCount % 10 == 0) {

            System.out.println(
                    "NpcManager Tick=" + tickCount
                            + " NPCs=" + npcs.size()
            );
        }
    }

    private void updateNpc(NpcItem npc) {

        switch (npc.getState()) {

            case IDLE:
                updateIdle(npc);
                break;

            case CHASE:
                break;

            case ATTACK:
                break;

            case RETURNING:
                break;

            case DEAD:
                break;
                
            case PATROL:
				break;
        }
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

    private void updateIdle(NpcItem npc) {

    	 // TESTE TEMPORÁRIO

        float fakePlayerX =
                npc.getX() + 20.0f;

        float fakePlayerZ =
                npc.getZ();

        boolean detected =
                isInsideDetectionRange(
                        npc,
                        fakePlayerX,
                        fakePlayerZ
                );

        if (detected && tickCount % 10 == 0) {

            System.out.println(
                    "NPC "
                            + npc.getId()
                            + " detectou alvo."
                            + " Range="
                            + npc.getDetectionRange()
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
