package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

import character.CharacterManager;
import character.Warrior;

public class UserJoinRoomHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {

		// 1. Recupera o usuário que acabou de entra em Sido com sucesso
		User user = (User) event.getParameter(SFSEventParam.USER);

		CharacterManager characterManager = ((SidoExtension) getParentExtension()).getCharacterManager();

		if (!characterManager.hasCharacter(user)) {

			Warrior warrior = new Warrior(1001L, user, "TestWarrior", 0.0f, 0.0f, 0.0f);

			characterManager.registerCharacter(warrior);

		}

		character.Character found = characterManager.getByUser(user);

		character.Character character = characterManager.getByUser(user);

		character.setPosition(0.0f, 0.0f, 0.0f);

		trace("Character encontrado: " + found);

		/*
		 * User user = (User) event.getParameter(SFSEventParam.USER);
		 * 
		 * // 3. Recupera o gerenciador do banco de dados IDBManager dbManager =
		 * getParentExtension().getParentZone().getDBManager();
		 * 
		 * PlayerSession playerSession = new PlayerSession();
		 * 
		 * int characterId = params.getInt("characterId");
		 * 
		 * try { // Consulta os personagens baseando-se no nome do usuário (ou ID do
		 * usuário se você tiver) // IMPORTANTE: Altere "user_chars" e "username_coluna"
		 * de acordo com o mapeamento exato da sua tabela String sql =
		 * "SELECT id, nome, classe, exp, mapa FROM user_chars WHERE username_coluna = ?"
		 * ;
		 * 
		 * // O SFS3 executa a query e já mapeia o resultado do PostgreSQL em um
		 * ISFSArray automaticamente ISFSArray charList = dbManager.executeQuery(sql,
		 * new Object[] { user.getName() });
		 * 
		 * playerSession.setCharacterId(character.getId());
		 * playerSession.setName(character.getName());
		 * playerSession.setClassId(character.getClassId());
		 * playerSession.setExp(character.getExp());
		 * playerSession.setMapId(character.getMapId());
		 * 
		 * user.setProperty("playerSession", playerSession);
		 * 
		 * // Criamos a lista de variáveis que serão propagadas na rede
		 * List<UserVariable> userVariables = new ArrayList<>();
		 * 
		 * userVariables.add(new SFSUserVariable("id", id)); userVariables.add(new
		 * SFSUserVariable("name", nome)); userVariables.add(new
		 * SFSUserVariable("class", classe)); userVariables.add(new
		 * SFSUserVariable("exp", exp)); userVariables.add(new SFSUserVariable("map",
		 * mapa));
		 * 
		 * getApi().setUserVariables(user, userVariables);
		 */
	}

}
