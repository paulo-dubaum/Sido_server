package server;

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.components.signup.ISignUpAssistantPlugin;
import com.smartfoxserver.components.signup.PasswordMode;
import com.smartfoxserver.components.signup.SignUpAssistantComponent;
import com.smartfoxserver.components.signup.SignUpConfiguration;
import com.smartfoxserver.components.signup.SignUpErrorCodes;
import com.smartfoxserver.components.signup.SignUpValidationException;
import com.smartfoxserver.core.SFSEventType;
import com.smartfoxserver.entities.Room;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.extensions.SFSExtension;
import com.smartfoxserver.mmo.MMORoom;
import com.smartfoxserver.mmo.Vec3D;

import character.CharacterManager;
import navmesh.NavMeshLoader;
import navmesh.NavMeshPolygon;
import npc.NpcData;
import npc.NpcDatabase;
import npc.NpcItem;
import npc.NpcManager;
import world.MapManager;
import world.ServerNavMesh;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SidoExtension extends SFSExtension {

	private CharacterManager characterManager;
	private NpcManager npcManager;
	private MapManager mapManager;
	private ServerNavMesh lorenciaNavMesh;
	private ScheduledFuture<?> npcTaskHandle;

	private SignUpAssistantComponent suac;

	@Override
	public void init() {
		// Envia uma mensagem para o log do servidor ao iniciar
		trace("A Extensao do Sido foi iniciada com sucesso!");

		// 1. Obtém a instância global do SmartFoxServer
		SmartFoxServer sfs = SmartFoxServer.getInstance();

		try {
			
			lorenciaNavMesh = NavMeshLoader.load(Path.of("data", "maps", "Norencia.navmesh"));
			
		} catch (IOException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Envia uma mensagem para o log do servidor ao iniciar
				trace("Os mapas foram carregados com sucesso!");
				
				MMORoom room = (MMORoom) getParentZone().getRoomByName("Norencia");

				characterManager = new CharacterManager();
				mapManager = new MapManager();

				npcManager =
				        new NpcManager(
				                room,
				                lorenciaNavMesh,
				                characterManager
				        );
		

		// Dentro de um método de inicialização:
		try {
		    NpcDatabase database = new NpcDatabase(
		        Paths.get("data/config/npcs.json")
		    );

		    trace("NPCs carregados: " + database.size());

		    NpcData data = database.getNpc(0);

		    int npcId = 1;

		    for (int i = 148; i < 151; i++) {

		        for (int j = 130; j < 133; j++) {

		            NpcItem npc = new NpcItem(
		                data,
		                npcId++,
		                (float) i,
		                5.99507f,
		                (float) j
		            );

		            NpcItem registeredNpc = npcManager.spawnNpc(npc);

		            if (registeredNpc != null) {
		                System.out.printf(
		                    "NPC ID=%d Type=%d Radius=%.3f Speed=%.2f%n",
		                    npc.getId(),
		                    npc.getNpcTypeId(),
		                    npc.getAgentRadius(),
		                    npc.getMovementSpeed()
		                );
		            }
		        }
		    }
		    

		    /*if (npcTeste != null) {
		        trace("NPC: " + npcTeste.getNpcName());
		        trace("Velocidade: " + npcTeste.getMovementSpeed());
		        trace("Raio: " + npcTeste.getAgentRadius());
		    } else {
		        trace("npcTeste (tipo 0) nao encontrado.");
		    }*/

		} catch (Exception e) {
		    trace("Erro ao carregar NPCs: " + e.getMessage());
		}
		
		// Envia uma mensagem para o log do servidor ao iniciar
		trace("Os npcs foram carregados com sucesso!");

		

		/*
		 * try {
		 * 
		 * mapManager.loadMap("norencia", Path.of("data", "maps", "Norencia.navmesh"));
		 * 
		 * 
		 * //mapManager.loadMap("noria", Path.of("data", "maps", "Noria.navmesh"));
		 * 
		 * //mapManager.loadMap("devias", Path.of("data", "maps", "Devias.navmesh"));
		 * 
		 * } catch (Exception e) {
		 * 
		 * trace("Falha ao carregar mapas.");
		 * 
		 * throw new RuntimeException(e); }
		 */

		/*NpcItem npcTest = npcManager.spawnNpc(1, // npcTypeId
				-238.8f, // X
				17.3999f, // Y
				17.2f, // Z
				15.0f // detectionRange
		);

		if (npcTest != null) {

			trace("NPC TEST criado." + " ID=" + npcTest.getId());

			trace("NPCs registrados: " + npcManager.getNpcCount());
		}*/

		NavMeshPolygon p0 = lorenciaNavMesh.getPolygon(0);

		NavMeshPolygon p500 = lorenciaNavMesh.getPolygon(500);
		
		/*for(int i = 190; i < 210; i++) {
			
			for(int j = 141; j < 160; j++) {
				
				NpcItem npc1 = npcManager.spawnNpc(1, 1, (float)i, 5.99507f, (float)j, 15.0f);
				
			}
			
		}
		
		for(int i = 141; i < 181; i++) {
			
			NpcItem npc1 = npcManager.spawnNpc(1, 1, (float)i, 5.99507f, 141.5f, 15.0f);
			
		}*/


		//NpcItem npc2 = npcManager.spawnNpc(1, 2, 141.5f, 5.99507f, 131.5f, 30.0f);

		int period = 100; // Intervalo de 100 milissegundos

		// No SFS3 com Java Puro, você pode usar referências de método diretas (::)
		// O try/catch encapsulado garante que exceções internas no update não quebrem o
		// loop definitivo
		this.npcTaskHandle = sfs.getTaskScheduler().scheduleAtFixedRate(() -> {
			
			try {

				npcManager.update();
				
			} catch (Exception e) {

				trace("Erro crítico no loop do NpcManager: " + e.getMessage());
			}
		}, 5000, // Delay inicial de 5 segundos antes do primeiro
				period, TimeUnit.MILLISECONDS);


		suac = new SignUpAssistantComponent();

		suac.getConfig().extraFields = Arrays.asList("country", "age");

		// Set limits for min/max name and password length
		suac.getConfig().minUserNameLength = 4;
		suac.getConfig().maxUserNameLength = 30;
		suac.getConfig().minPasswordLength = 8;
		suac.getConfig().maxPasswordLength = 30;

		suac.getConfig().passwordMode = PasswordMode.SHA256;

		suac.getConfig().preProcessPlugin = new ISignUpAssistantPlugin() {
			@Override
			public void execute(User user, ISFSObject params, SignUpConfiguration config)
					throws SignUpValidationException {
				Integer age = params.getInt("age");
				String country = params.getShortString("country");

				if (age == null)
					throw new SignUpValidationException(SignUpErrorCodes.CUSTOM_ERROR, "The age is missing");

				if (age < 14)
					throw new SignUpValidationException(SignUpErrorCodes.CUSTOM_ERROR,
							"You must be at least 14 years old to access this game");

				if (country == null || country.length() < 2)
					throw new SignUpValidationException(SignUpErrorCodes.CUSTOM_ERROR, "Please specify your country");
			}
		};

		addRequestHandler(SignUpAssistantComponent.COMMAND_PREFIX, suac);

		// Register for login event
		addEventHandler(SFSEventType.USER_LOGIN, OnLogin.class);
		addEventHandler(SFSEventType.USER_JOIN_ZONE, OnZoneJoin.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, UserJoinRoomHandler.class);
		addEventHandler(SFSEventType.USER_VARIABLES_UPDATE, UserVariableUpdate.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, UserDisconnectHandler.class);

	}

	public MapManager getMapManager() {
		return mapManager;
	}

	/*private NpcItem createTestNpc(Room targetRoom, ServerNavMesh navMesh) {

		float x = -238.8f;
		float y = 17.3999f;
		float z = 17.2f;

		NpcItem npc = new NpcItem(1, x, y, z, 15.0f);

		NavMeshPolygon polygon = navMesh.findContainingPolygon(x, y, z);

		if (polygon == null) {

			trace("NPC não está sobre o NavMesh.");

			return null;
		}

		npc.setCurrentPolygonId(polygon.id);

		SmartFoxServer.getInstance().getAPIManager().getMMOApi().setMMOItemPosition(npc, new Vec3D(x, y, z),
				targetRoom);

		trace("NPC criado." + " MMOItem ID: " + npc.getId() + " Polygon: " + polygon.id + " Range: "
				+ npc.getDetectionRange());

		return npc;
	}*/

	public CharacterManager getCharacterManager() {
		return characterManager;
	}
	public NpcManager getNpcrManager() {
		return npcManager;
	}

	public ServerNavMesh getNavMesh() {
		return lorenciaNavMesh;
	}

	@Override
	public void destroy() {

		// Interrompe e limpa o agendamento no encerramento ou reload da extensão
		if (npcTaskHandle != null && !npcTaskHandle.isCancelled()) {
			// O parâmetro 'true' força a interrupção se a tarefa estiver rodando no exato
			// momento
			npcTaskHandle.cancel(true);
			trace("Loop de NPCs finalizado com sucesso.");
		}

		super.destroy();
	}

}
