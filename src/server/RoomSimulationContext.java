package server;

import character.CharacterManager;
import com.smartfoxserver.mmo.MMORoom;
import npc.NpcManager;
import world.ServerNavMesh;

/** Estado isolado de uma instancia; NavMesh compartilhavel apenas se imutavel. */
public final class RoomSimulationContext {
    private final MMORoom room;
    private final ServerNavMesh navMesh;
    private final CharacterManager characters;
    private final NpcManager npcs;
    private String regionId;
    private int instanceId;
    private int weatherId;
    private int initialNpcCount;

    public RoomSimulationContext(MMORoom room, ServerNavMesh navMesh) {
        if (room == null || navMesh == null) throw new IllegalArgumentException("Room/NavMesh obrigatorios");
        this.room = room;
        this.navMesh = navMesh;
        this.characters = new CharacterManager();
        this.npcs = new NpcManager(room, navMesh, characters);
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
    public void update() { npcs.update(); }
}
