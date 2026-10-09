package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

public class UserDisconnectHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		
		User user = (User) event.getParameter(SFSEventParam.USER);

		character.CharacterManager characterManager = ((SidoExtension) getParentExtension()).getCharacterManager();
		
		npc.NpcManager npcManager = ((SidoExtension) getParentExtension()).getNpcrManager();

		character.Character character =
		        characterManager.getByUser(user);

		if (character != null) {

		    npcManager.onCharacterDisconnected(character);

		    characterManager.removeByUser(user);
		}
	}	

}
