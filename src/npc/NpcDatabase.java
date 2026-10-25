package npc;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/** Carrega definicoes de tipos de NPCs. Aceita array raiz ou objeto {"npcs": [...]}. */
public class NpcDatabase {
    private final Map<Integer, NpcData> npcByTypeId;

    public NpcDatabase(Path jsonPath) throws IOException {
        Gson gson = new Gson();
        JsonElement root;
        try (Reader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            root = gson.fromJson(reader, JsonElement.class);
        } catch (JsonParseException e) {
            throw new IOException("Erro de sintaxe JSON em " + jsonPath + ": " + e.getMessage(), e);
        }

        JsonArray entries;
        if (root != null && root.isJsonArray()) {
            entries = root.getAsJsonArray();
        } else if (root != null && root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            JsonElement npcs = object.get("npcs");
            if (npcs == null || !npcs.isJsonArray()) {
                throw new IOException("Campo 'npcs' deve ser um array em " + jsonPath);
            }
            entries = npcs.getAsJsonArray();
        } else {
            throw new IOException("Raiz de " + jsonPath + " deve ser um array ou objeto com campo 'npcs'");
        }

        Map<Integer, NpcData> loaded = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            JsonElement element = entries.get(i);
            if (element == null || !element.isJsonObject()) {
                throw new IOException("NPC no indice " + i + " deve ser um objeto em " + jsonPath);
            }
            JsonObject object = element.getAsJsonObject();
            JsonElement id = object.get("npcTypeId");
            if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isNumber()) {
                throw new IOException("npcTypeId numerico ausente/invalido no indice " + i + " em " + jsonPath);
            }
            try {
                NpcData npc = gson.fromJson(element, NpcData.class);
                if (npc == null) {
                    throw new IOException("NPC nulo no indice " + i + " em " + jsonPath);
                }
                int typeId = npc.getNpcTypeId();
                if (typeId < 0) {
                    throw new IOException("npcTypeId negativo no indice " + i + " em " + jsonPath);
                }
                if (loaded.putIfAbsent(typeId, npc) != null) {
                    throw new IOException("npcTypeId duplicado: " + typeId + " em " + jsonPath);
                }
            } catch (JsonParseException | IllegalStateException | NumberFormatException e) {
                throw new IOException("NPC invalido no indice " + i + " em " + jsonPath + ": " + e.getMessage(), e);
            }
        }
        npcByTypeId = Collections.unmodifiableMap(loaded);
    }

    public NpcData getNpc(int npcTypeId) { return npcByTypeId.get(npcTypeId); }
    public int size() { return npcByTypeId.size(); }
    public boolean contains(int npcTypeId) { return npcByTypeId.containsKey(npcTypeId); }
}
