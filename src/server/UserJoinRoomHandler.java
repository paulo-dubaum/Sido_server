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
        if (user == null || room == null) return;
        SidoExtension extension = (SidoExtension) getParentExtension();
        RoomSimulationContext destination = extension.getRoomContext(room.getId());
        if (destination == null) return;
        Object pending = user.getSession().getProperty(CharacterSelectHandler.PENDING_CHARACTER_KEY);
        if (!(pending instanceof CharacterData)) {
            trace("Entrada sem personagem selecionado: " + user.getName());
            return;
        }
        CharacterData data = (CharacterData) pending;
        RoomSimulationContext expected = extension.selectRoomForMap(data.mapId);
        if (expected != destination) return;
        NavMeshPolygon polygon = destination.getNavMesh().findContainingPolygon(data.x, data.y, data.z);
        if (polygon == null) return;

        for (RoomSimulationContext other : extension.getRoomContexts()) {
            if (other != destination) {
                synchronized (other) { RoomCharacterSupport.remove(user, other); }
            }
        }
        synchronized (destination) {
            if (destination.getCharacterManager().hasCharacter(user)) return;
            Character warrior;
            switch (data.classId) {
                case 1: warrior = new Warrior(data.id, user, data.name, data.x, data.y, data.z); break;
                case 2: warrior = new Archer(data.id, user, data.name, data.x, data.y, data.z); break;
                case 3: warrior = new Mage(data.id, user, data.name, data.x, data.y, data.z); break;
                default: return;
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
            warrior.setResourcePoints(data.resourcePoints);
            warrior.setCurrentPolygonId(polygon.id);
            destination.getCharacterManager().registerCharacter(warrior);
        }
        user.getSession().removeProperty(CharacterSelectHandler.PENDING_CHARACTER_KEY);
        ISFSObject response = new SFSObject();
        response.putBool("success", true);
        response.putLong("characterId", data.id);
        response.putString("name", data.name);
        response.putInt("classId", data.classId);
        response.putInt("level", data.level);
        response.putLong("totalExperience", data.totalExperience);
        response.putInt("mapId", data.mapId);
        response.putFloat("x", data.x);
        response.putFloat("y", data.y);
        response.putFloat("z", data.z);
        response.putInt("currentHealth", data.currentHealth);
        response.putInt("currentMana", data.currentMana);
        response.putInt("currentAgility", data.currentAgility);
        response.putInt("strengthPoints", data.strengthPoints);
        response.putInt("agilityPoints", data.dextePoints);
        response.putInt("powerPoints", data.powerPoints);
        response.putInt("resourcePoints", data.resourcePoints);
        send("characterLoaded", response, user);
    }
}
