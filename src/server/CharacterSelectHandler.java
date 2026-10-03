package server;

import java.sql.SQLException;

import com.smartfoxserver.bitswarm.data.TransportType;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.extensions.BaseClientRequestHandler;

import infra.CharacterDAO;
import utils.CharacterModel;

public class CharacterSelectHandler extends BaseClientRequestHandler {

	private CharacterDAO characterDAO;

	@Override
	public void handleClientRequest(User user, ISFSObject params, TransportType arg2) {

		int characterId = params.getInt("characterId");

		int databaseUserId = (Integer) user.getProperty("databaseUserId");
		
		CharacterDAO characterDAO = new CharacterDAO(getParentExtension().getParentZone().getDBManager());
		
	
		// Busca o personagem pertencente ao usuário
		 CharacterModel character;
		try {
			
			character = (CharacterModel) characterDAO.getCharactersByAccount("s");
			
			if (character == null) {

				ISFSObject response = new SFSObject();

				response.putBool("success", false);

				response.putString("message", "Personagem inválido.");

				send("CharacterSelectResponse", response, user);

				return;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Personagem não encontrado
		
		// Agora os dados vieram do PostgreSQL
//        String nome =
//            character.getName();
//
//        int classe =
//            character.getClassId();
//
//        long exp =
//            character.GetExp();
//
//        int mapa =
//            character.GetMapId();
//
//        getLogger().info(
//            "Personagem selecionado: "
//            + nome
//        );

        // Guardar personagem selecionado
//        user.setProperty(
//            "characterId",
//            character.getId()
//        );
//
//        user.setProperty(
//            "characterName",
//            nome
//        );
//
//        user.setProperty(
//            "characterClass",
//            classe
//        );
//
//        user.setProperty(
//            "characterExp",
//            exp
//        );
//
//        user.setProperty(
//            "mapId",
//            mapa
//        );

	}

}
