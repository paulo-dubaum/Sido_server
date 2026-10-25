package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.Room;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;
import character.CharacterData;
import character.Warrior;
import character.Archer;
import character.Mage;
import character.Character;
import navmesh.NavMeshPolygon;

public final class UserJoinRoomHandler extends BaseServerEventHandler {
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		User user = (User) event.getParameter(SFSEventParam.USER);
		Room room = (Room) event.getParameter(SFSEventParam.ROOM);

		trace("USER_JOIN_ROOM: user=" + user.getName() + " | room=" + room.getName() + " | roomId=" + room.getId());

		if (user == null || room == null)
			return;
		SidoExtension extension = (SidoExtension) getParentExtension();
		RoomSimulationContext destination = extension.getRoomContext(room.getId());
		if (destination == null)
			if (destination == null) {
			    trace("ERRO: Nenhum RoomSimulationContext para " + room.getName());
			    return;
			}

		Object pending = user.getSession().getProperty(
			    CharacterSelectHandler.PENDING_CHARACTER_KEY
			);

			if (!(pending instanceof CharacterData)) {
			    trace("ERRO: pendingCharacter ausente ou invalido. user=" + user.getName());
			    return;
			}
			
			
		CharacterData data = (CharacterData) pending;
		RoomSimulationContext expected = extension.selectRoomForMap(data.mapId);
		if (expected != destination)
		{
			 trace("ERRO: Contexto diferente do esperado. mapId=" + data.mapId);
			return;
		}
		
		NavMeshPolygon polygon = destination.getNavMesh().findContainingPolygon(data.x, data.y, data.z);
		if (polygon == null) {
			
		    trace("ERRO: Posicao fora do NavMesh: "
		        + data.x + ", " + data.y + ", " + data.z);
		    return;
		}
		
		for (RoomSimulationContext other : extension.getRoomContexts()) {
			if (other != destination) {
				synchronized (other) {
					RoomCharacterSupport.remove(user, other);
				}
			}
		}
		synchronized (destination) {
			if (destination.getCharacterManager().hasCharacter(user))
				return;
			Character warrior;
			switch (data.classId) {
			case 1:
				warrior = new Warrior(data.id, user, data.name, data.x, data.y, data.z);
				break;
			case 2:
				warrior = new Archer(data.id, user, data.name, data.x, data.y, data.z);
				break;
			case 3:
				warrior = new Mage(data.id, user, data.name, data.x, data.y, data.z);
				break;
			default:
				return;
			}
			warrior.setCclassId(data.classId);
			warrior.setLevel(data.level);

			// Character atual usa int para experiencia; evitar truncamento silencioso.
			warrior.setTotalExperience(data.totalExperience);
			warrior.setHealth(data.currentHealth);
			warrior.setMana(data.currentMana);
			warrior.setCurrentAgility(data.currentAgility);
			warrior.setStrength(data.strengthPoints);
			warrior.setDexterity(data.dextePoints);
			warrior.setIntelligence(data.powerPoints);
			warrior.setVitalityPoints(data.agilityPoints);
			warrior.setAvailableAttributePoints(data.unallocatedAttributePoints);
			warrior.setCurrentPolygonId(polygon.id);
			
			destination.getCharacterManager().registerCharacter(warrior);

			Character registered = destination.getCharacterManager().getByUser(user);

			trace("REGISTRO: "
			    + (registered != null ? "SUCESSO" : "FALHOU")
			    + " | userId=" + user.getId()
			    + " | characterId=" + data.id
			    + " | room=" + room.getName());
		}
		user.getSession().removeProperty(CharacterSelectHandler.PENDING_CHARACTER_KEY);
	}
}
