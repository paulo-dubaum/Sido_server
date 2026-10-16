package server;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.smartfoxserver.entities.Room;
import com.smartfoxserver.entities.Zone;
import com.smartfoxserver.mmo.MMORoom;
import navmesh.NavMeshLoader;
import npc.NpcDatabase;
import world.NpcSpawnLoader;
import world.RegionDefinition;
import world.ServerNavMesh;

/** Descobre salas configuradas no painel; nao cria MMORooms. */
public final class WorldRoomManager {
    private final Map<Integer, RoomSimulationContext> contexts = new LinkedHashMap<>();
    private final Map<String, ServerNavMesh> meshes = new HashMap<>();

    public void initialize(Zone zone, RegionDefinition[] regions, NpcDatabase database) throws IOException {
        if (!contexts.isEmpty()) throw new IllegalStateException("Salas ja inicializadas");
        // Primeiro valida todas as salas e carrega todos os NavMeshes.
        for (RegionDefinition region : regions) {
            String roomName = region.id + "_001";
            Room room = zone.getRoomByName(roomName);
            if (!(room instanceof MMORoom))
                throw new IOException("MMORoom nao encontrada no painel: " + roomName);
            if (!meshes.containsKey(region.id)) {
                ServerNavMesh mesh = NavMeshLoader.load(Paths.get(region.navMesh));
                if (mesh == null) throw new IOException("NavMesh nulo: " + region.navMesh);
                meshes.put(region.id, mesh);
            }
        }
        for (RegionDefinition region : regions) {
            MMORoom room = (MMORoom) zone.getRoomByName(region.id + "_001");
            RoomSimulationContext context = new RoomSimulationContext(room, meshes.get(region.id));
            context.setRegion(region.id, 1);
            contexts.put(room.getId(), context);
            int count = NpcSpawnLoader.spawn(Paths.get(region.spawns), region.id,
                    database, context.getNpcManager());
            context.setInitialNpcCount(count);
        }
    }

    public RoomSimulationContext get(int roomId) { return contexts.get(roomId); }
    public Collection<RoomSimulationContext> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(contexts.values()));
    }
    public void clear() { contexts.clear(); meshes.clear(); }
}
