package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;
import character.Character;
import infra.CharacterDAO;

/** Salva e remove personagens de todas as salas, nao apenas da sala inicial. */
public final class UserDisconnectHandler extends BaseServerEventHandler {
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		User user = (User) event.getParameter(SFSEventParam.USER);
		if (user == null)
			return;
		SidoExtension extension = (SidoExtension) getParentExtension();
		Object account = user.getSession().getProperty(OnLogin.DATABASE_USER_ID_KEY);
		for (RoomSimulationContext context : extension.getRoomContexts()) {
			synchronized (context) {
				Character character = context.getCharacterManager().getByUser(user);
				if (character == null)
					continue;
				// Nao remover em caso de falha no banco: permitir nova tentativa de
				// persistencia.
				if (!(account instanceof Number)) {
					trace("Nao foi possivel salvar personagem: conta nao identificada. CharacterID="
							+ character.getCharacterId());
					continue;
				}
				int mapId = mapIdForRoom(context.getRoom().getName());
				if (mapId < 0) {
					trace("Nao foi possivel salvar personagem: regiao desconhecida.");
					continue;
				}
				try {
					new CharacterDAO(extension.getParentZone().getDBManager()).saveState(character.getCharacterId(),
							((Number) account).longValue(), mapId, character.getX(), character.getY(), character.getZ(),
							character.getLevel(), character.getTotalExperience(), character.getHealth(),
							character.getMana(), character.getCurrentAgility(), character.getStrength(),
							character.getDexterity(), character.getIntelligence(), character.getResourcePoints());
				} catch (Exception e) {
					trace("Erro ao salvar CharacterID=" + character.getCharacterId() + ": " + e);
					continue;
				}
				context.getNpcManager().onCharacterDisconnected(character);
				context.getCharacterManager().removeByUser(user);
			}
		}
	}

	private static int mapIdForRoom(String roomName) {
		if (roomName.startsWith("norencia_"))
			return 0;
		if (roomName.startsWith("cordilheira_"))
			return 1;
		if (roomName.startsWith("deserto_"))
			return 2;
		if (roomName.startsWith("castelo_"))
			return 3;
		return -1;
	}
}
