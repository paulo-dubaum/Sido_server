
package npc;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class NpcDatabase {

    private static class NpcCollection {
        List<NpcData> npcs;
    }

    private final Map<Integer, NpcData> npcByTypeId;

    public NpcDatabase(Path jsonPath) throws IOException {
        Gson gson = new Gson();

        NpcCollection collection;

        try (Reader reader = Files.newBufferedReader(
                jsonPath, StandardCharsets.UTF_8)) {

            collection = gson.fromJson(reader, NpcCollection.class);

        } catch (JsonParseException e) {
            throw new IOException(
                "JSON de NPCs invalido: " + jsonPath, e
            );
        }

        if (collection == null || collection.npcs == null) {
            throw new IOException(
                "Campo 'npcs' ausente: " + jsonPath
            );
        }

        Map<Integer, NpcData> loaded = new HashMap<>();

        for (NpcData npc : collection.npcs) {

            if (npc == null) {
                throw new IOException(
                    "Registro de NPC nulo em: " + jsonPath
                );
            }

            int typeId = npc.getNpcTypeId();

            if (loaded.putIfAbsent(typeId, npc) != null) {
                throw new IOException(
                    "npcTypeId duplicado: " + typeId
                );
            }
        }

        npcByTypeId = Collections.unmodifiableMap(loaded);
    }

    public NpcData getNpc(int npcTypeId) {
        return npcByTypeId.get(npcTypeId);
    }

    public int size() {
        return npcByTypeId.size();
    }

    public boolean contains(int npcTypeId) {
        return npcByTypeId.containsKey(npcTypeId);
    }
}
