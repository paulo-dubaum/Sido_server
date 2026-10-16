package world;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import npc.NpcData;
import npc.NpcDatabase;
import npc.NpcItem;
import npc.NpcManager;

/** Configuracao de spawns por regiao, replicada independentemente em cada instancia. */
public final class NpcSpawnLoader {
    private static final class SpawnFile { String regionId; Spawn[] npcSpawns; }
    private static final class Spawn {
        int npcTypeId;
        Position position;
        int count;
        Formation formation;
        int respawnSeconds; // Reservado: nao implementa respawn nesta etapa.
    }
    private static final class Position { float x, y, z; }
    private static final class Formation { String type; int columns; float spacingX, spacingZ; }

    private NpcSpawnLoader() { }

    public static int spawn(Path path, String regionId, NpcDatabase database, NpcManager manager)
            throws IOException {
        SpawnFile file;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            file = new Gson().fromJson(reader, SpawnFile.class);
        } catch (com.google.gson.JsonParseException e) {
            throw new IOException("JSON de spawns invalido: " + path, e);
        }
        if (file == null || !regionId.equals(file.regionId) || file.npcSpawns == null)
            throw new IOException("Arquivo de spawns incompativel: " + path);

        // Validar todas as definicoes antes de registrar qualquer NPC.
        for (Spawn spawn : file.npcSpawns) {
            if (spawn == null || spawn.position == null || spawn.count < 1 || spawn.count > 10000)
                throw new IOException("Spawn invalido em " + path);
            if (database.getNpc(spawn.npcTypeId) == null)
                throw new IOException("Tipo de NPC inexistente: " + spawn.npcTypeId);
            if (spawn.count > 1 && (spawn.formation == null ||
                    !"GRID".equals(spawn.formation.type) || spawn.formation.columns <= 0))
                throw new IOException("Formacao GRID obrigatoria para multiplos NPCs em " + path);
        }
        int id = 1;
        int spawned = 0;
        for (Spawn spawn : file.npcSpawns) {
            NpcData data = database.getNpc(spawn.npcTypeId);
            for (int i = 0; i < spawn.count; i++) {
                int col = spawn.formation == null ? 0 : i % spawn.formation.columns;
                int row = spawn.formation == null ? 0 : i / spawn.formation.columns;
                float x = spawn.position.x + (spawn.formation == null ? 0 : col * spawn.formation.spacingX);
                float z = spawn.position.z + (spawn.formation == null ? 0 : row * spawn.formation.spacingZ);
                NpcItem npc = new NpcItem(data, id++, x, spawn.position.y, z);
                if (manager.spawnNpc(npc) == null)
                    throw new IOException("Falha ao registrar NPC em " + regionId + " na posicao " + x + "," + z);
                spawned++;
            }
        }
        return spawned;
    }
}
