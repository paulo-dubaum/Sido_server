package server;

import character.CharacterManager;
import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.mmo.MMORoom;
import com.smartfoxserver.mmo.Vec3D;
import navmesh.NavMeshPolygon;
import npc.NpcManager;
import world.ServerNavMesh;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Estado isolado da sala; morte e respawn processados no mesmo tick dos NPCs. */
public final class RoomSimulationContext {
    private static final long RESPAWN_DELAY_NS = 3_000_000_000L;
    // Somente para testes em Norencia. Futuramente substituir por politica por mapa.
    private static final int TEST_SPAWN_MAP = 0;
    private static final float TEST_SPAWN_X = 245.0f;
    private static final float TEST_SPAWN_Y = 6.039f;
    private static final float TEST_SPAWN_Z = 167.5f;

    private final MMORoom room;
    private final ServerNavMesh navMesh;
    private final CharacterManager characters;
    private final NpcManager npcs;
    private SidoExtension extension;
    private String regionId;
    private int instanceId;
    private int weatherId;
    private int initialNpcCount;
    private final Map<Long, Long> respawnAtNs = new HashMap<>();

    public RoomSimulationContext(MMORoom room, ServerNavMesh navMesh) {
        if (room == null || navMesh == null)
            throw new IllegalArgumentException("Room, NavMesh e extension obrigatorios");
        this.room = room;
        this.navMesh = navMesh;
        this.characters = new CharacterManager();
        this.npcs = new NpcManager(room, navMesh, characters);
    }

    public void setExtension(SidoExtension extension) {
        if (extension == null) throw new IllegalArgumentException("Extension obrigatoria");
        this.extension = extension;
    }
    void setRegion(String regionId, int instanceId) { this.regionId = regionId; this.instanceId = instanceId; }
    void setInitialNpcCount(int count) { this.initialNpcCount = count; }
    public String getRegionId() { return regionId; }
    public int getInstanceId() { return instanceId; }
    public int getWeatherId() { return weatherId; }
    public void setWeatherId(int weatherId) { this.weatherId = weatherId; }
    public int getInitialNpcCount() { return initialNpcCount; }
    public MMORoom getRoom() { return room; }
    public ServerNavMesh getNavMesh() { return navMesh; }
    public CharacterManager getCharacterManager() { return characters; }
    public NpcManager getNpcManager() { return npcs; }

    public void update() {
        npcs.update();
        updateRespawns();
    }

    /** Chamavel por PvP, habilidades e outras fontes de dano, no lock da sala. */
    public void onCharacterDied(character.Character character) {
        if (character == null || character.getHealth() > 0) return;
        if (characters.getByUser(character.getUser()) != character) return;
        npcs.onCharacterDied(character);
        if (respawnAtNs.putIfAbsent(character.getCharacterId(),
                System.nanoTime() + RESPAWN_DELAY_NS) == null) {
            sendLifeEvent(character, "DEAD", 0, character.getX(), character.getY(), character.getZ());
        }
    }

    private void updateRespawns() {
        long now = System.nanoTime();
        Set<Long> active = new HashSet<>();
        for (character.Character character : characters.getCharacters()) {
            long id = character.getCharacterId();
            active.add(id);
            if (character.getHealth() <= 0) onCharacterDied(character);
            Long due = respawnAtNs.get(id);
            if (due != null && now >= due) {
                if (character.getHealth() <= 0) respawn(character);
                respawnAtNs.remove(id);
            }
        }
        respawnAtNs.keySet().retainAll(active);
    }

    private void respawn(character.Character character) {
        // O teste atual usa apenas Norencia. Nao mover silenciosamente entre salas.
        if (!room.getName().toLowerCase(java.util.Locale.ROOT).startsWith("norencia_"))
            throw new IllegalStateException("Respawn entre mapas ainda nao implementado: " + room.getName());
        NavMeshPolygon polygon = navMesh.findContainingPolygon(TEST_SPAWN_X, TEST_SPAWN_Y, TEST_SPAWN_Z);
        if (polygon == null) throw new IllegalStateException("Spawn de teste fora do NavMesh");
        int vitality = character.getVitalityPoints();
        int maxHealth;
        switch (character.getCclassId()) {
            case 1: maxHealth = vitality * 3 + 35; break;
            case 2: maxHealth = vitality * 2 + 30; break;
            case 3: maxHealth = vitality * 2 + 25; break;
            default: throw new IllegalStateException("Classe desconhecida: " + character.getCclassId());
        }
        if (maxHealth <= 0) throw new IllegalStateException("maxHealth invalida");
        // Primeiro atualiza o estado e a posicao no servidor, depois o AOI do SmartFox.
        character.setPosition(TEST_SPAWN_X, TEST_SPAWN_Y, TEST_SPAWN_Z);
        character.setCurrentPolygonId(polygon.id);
        character.setHealth(maxHealth);
        SmartFoxServer.getInstance().getAPIManager().getMMOApi().setUserPosition(
                character.getUser(), new Vec3D(TEST_SPAWN_X, TEST_SPAWN_Y, TEST_SPAWN_Z), room);
        sendLifeEvent(character, "ALIVE", maxHealth, TEST_SPAWN_X, TEST_SPAWN_Y, TEST_SPAWN_Z);
    }

    private void sendLifeEvent(character.Character character, String state, int health,
                               float x, float y, float z) {
        SFSObject data = new SFSObject();
        data.putLong("characterId", character.getCharacterId());
        data.putString("lifeState", state);
        data.putInt("health", health);
        data.putInt("respawnDelayMs", 3000);
        data.putFloat("x", x); data.putFloat("y", y); data.putFloat("z", z);
        data.putInt("mapId", TEST_SPAWN_MAP);
        if (extension == null) throw new IllegalStateException("SidoExtension nao configurada na sala");
        extension.sendCharacterLife(data, character.getUser());
    }
}
