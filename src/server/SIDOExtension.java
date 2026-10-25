package server;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.core.SFSEventType;
import com.smartfoxserver.entities.Room;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.extensions.SFSExtension;
import com.smartfoxserver.mmo.MMORoom;

import character.CharacterManager;
import navmesh.NavMeshLoader;
import npc.NpcData;
import npc.NpcDatabase;
import npc.NpcItem;
import npc.NpcManager;
import world.MapManager;
import world.ServerNavMesh;

/** Extensao da Zone: configuracao, registro de salas e agendamento. */
public class SidoExtension extends SFSExtension {
	private static final String STARTER_ROOM = "norencia_001";
	private static final int NPC_TICK_MS = 100;
	private static final int INITIAL_DELAY_MS = 5000;

	private final WorldRoomManager worldRooms = new WorldRoomManager();
	private world.WorldTimeManager worldTime;
	private volatile int lastPublishedWorldTime = -1;
	private volatile ScheduledFuture<?> npcTaskHandle;
	private volatile boolean stopping;
	private volatile boolean roomVariablesInitialized;
	private MapManager mapManager;

	@Override
	public void init() {
		stopping = false;
		roomVariablesInitialized = false;
		try {
			mapManager = new MapManager();
			registerHandlers();

			addRequestHandler("selectchar", CharacterSelectHandler.class);
			addRequestHandler("createCharacter", CreateCharacterHandler.class);
			addRequestHandler("deleteCharacter", DeleteCharacterHandler.class);

			world.RegionDefinition[] regions = world.RegionConfigLoader
					.load(Paths.get("data", "config", "regions.json"));
			npc.NpcDatabase npcDatabase = new npc.NpcDatabase(Paths.get("data", "config", "npcs.json"));
			worldRooms.initialize(getParentZone(), regions, npcDatabase);
            for (RoomSimulationContext context : worldRooms.getAll()) {
                context.setExtension(this);
            }
			worldTime = new world.WorldTimeManager(720, 60_000L, System.currentTimeMillis());
			int minute = worldTime.getWorldTime(System.currentTimeMillis());
			for (RoomSimulationContext context : worldRooms.getAll()) {
				trace("Sala pronta: " + context.getRoom().getName() + " NPCs=" + context.getInitialNpcCount());
			}
			lastPublishedWorldTime = -1; // Publicacao inicial adiada para o scheduler.
			startSimulationLoop();
			trace("SidoExtension iniciada. Salas simuladas: " + worldRooms.getAll().size());
		} catch (Exception e) {
			stopSimulationLoop();
			worldRooms.clear();
			trace("Falha ao iniciar SidoExtension: " + e.toString());
			throw new IllegalStateException("Falha ao inicializar a Zone Extension", e);
		}
	}

	/** Mapa persistido: 0=Norencia, 1=Cordilheira, 2=Deserto, 3=Castelo. */
	public RoomSimulationContext selectRoomForMap(int mapId) {
		final String[] regions = { "norencia", "cordilheira", "deserto", "castelo" };
		if (mapId < 0 || mapId >= regions.length)
			return null;
		String prefix = regions[mapId] + "_";
		for (RoomSimulationContext context : worldRooms.getAll()) {
			if (context.getRoom().getName().startsWith(prefix))
				return context;
		}
		return null;
	}

	public RoomSimulationContext getRoomContext(int roomId) {
		return worldRooms.get(roomId);
	}

	public Collection<RoomSimulationContext> getRoomContexts() {
		return worldRooms.getAll();
	}

	/** Altera clima de uma instancia; chame a partir do controlador do servidor. */
	public void setWeather(int roomId, int weatherId) {
		RoomSimulationContext context = worldRooms.get(roomId);
		if (context == null)
			throw new IllegalArgumentException("Sala nao registrada: " + roomId);
		if (weatherId < 0)
			throw new IllegalArgumentException("weatherId negativo");
		context.setWeatherId(weatherId);
		RoomStatePublisher.publishWeather(context);
	}

	public int getWorldTime() {
		if (worldTime == null)
			throw new IllegalStateException("Relogio nao inicializado");
		return worldTime.getWorldTime(System.currentTimeMillis());
	}

	private void startSimulationLoop() {
		npcTaskHandle = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(() -> {
			if (stopping)
				return;
			int currentMinute = getWorldTime();
			if (!roomVariablesInitialized) {
				try {
					for (RoomSimulationContext context : getRoomContexts()) {
						RoomStatePublisher.initialize(context, currentMinute);
					}
					roomVariablesInitialized = true;
					lastPublishedWorldTime = currentMinute;
					trace("Room Variables iniciais publicadas.");
				} catch (Exception e) {
					trace("Falha ao publicar Room Variables; nova tentativa no proximo tick: " + e);
					// Evita atualizar o mesmo estado antes da inicializacao.
					return;
				}
			}
			if (currentMinute != lastPublishedWorldTime) {
				for (RoomSimulationContext context : getRoomContexts()) {
					try {
						RoomStatePublisher.publishWorldTime(context, currentMinute);
					} catch (Exception e) {
						trace("Falha worldTime " + context.getRoom().getName() + ": " + e);
					}
				}
				lastPublishedWorldTime = currentMinute;
			}
			for (RoomSimulationContext context : getRoomContexts()) {
				try {
					synchronized (context) {
						context.update();
					}
				} catch (Exception e) {
					trace("Erro NPC sala " + context.getRoom().getName() + ": " + e.toString());
					e.printStackTrace();
				}
			}
		}, INITIAL_DELAY_MS, NPC_TICK_MS, TimeUnit.MILLISECONDS);
	}

	private void stopSimulationLoop() {
		stopping = true;
		ScheduledFuture<?> task = npcTaskHandle;
		npcTaskHandle = null;
		if (task != null)
			task.cancel(false);
	}

	private void registerHandlers() {
		addEventHandler(SFSEventType.USER_LOGIN, OnLogin.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, OnZoneJoin.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinRoomHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, UserExitRoomHandler.class);
		addEventHandler(SFSEventType.USER_VARIABLES_UPDATE, UserVariableUpdate.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, UserDisconnectHandler.class);
	}

	public MapManager getMapManager() {
		return mapManager;
	}
	
	public void sendCharacterLife(SFSObject data, User user) {
	    if (data == null || user == null || !user.isConnected()) {
	        return;
	    }

	    send("characterLife", data, user);
	}

	/**
	 * Compatibilidade temporaria com handlers existentes; use
	 * getRoomContext(roomId).
	 */
	@Deprecated
	public CharacterManager getCharacterManager() {
		return requireStarter().getCharacterManager();
	}

	@Deprecated
	public NpcManager getNpcrManager() {
		return requireStarter().getNpcManager();

	}

	@Deprecated
	public ServerNavMesh getNavMesh() {
		return requireStarter().getNavMesh();
	}

	private RoomSimulationContext requireStarter() {
		Room room = getParentZone().getRoomByName(STARTER_ROOM);
		RoomSimulationContext context = room == null ? null : worldRooms.get(room.getId());
		if (context == null)
			throw new IllegalStateException("Sala inicial nao registrada: " + STARTER_ROOM);
		return context;
	}

	@Override
	public void destroy() {
		stopSimulationLoop();
		worldRooms.clear();
		super.destroy();
	}
}
