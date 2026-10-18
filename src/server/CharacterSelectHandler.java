package server;

import java.sql.SQLException;

import com.smartfoxserver.bitswarm.data.TransportType;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.entities.data.SFSVector3;
import com.smartfoxserver.extensions.BaseClientRequestHandler;
import com.smartfoxserver.mmo.Vec3D;

import character.CharacterData;
import infra.CharacterDAO;

/**
 * Valida a selecao de personagem e solicita a entrada na MMORoom.
 *
 * A resposta "characterLoaded" e enviada pelo UserJoinRoomHandler,
 * somente depois de registrar o personagem no CharacterManager.
 */
public final class CharacterSelectHandler extends BaseClientRequestHandler {

    public static final String PENDING_CHARACTER_KEY = "pendingCharacter";

    @Override
    public void handleClientRequest(User user, ISFSObject params, TransportType transport) {
        if (user == null) {
            return;
        }

        if (params == null) {
            error(user, "Parametros de selecao ausentes.");
            return;
        }

        Object account = user.getSession().getProperty(OnLogin.DATABASE_USER_ID_KEY);
        if (!(account instanceof Number)) {
            error(user, "Sessao nao autenticada.");
            return;
        }

        long userId = ((Number) account).longValue();

        long characterId;
        try {
            characterId = params.getLong("characterId");
        } catch (Exception ex) {
            error(user, "characterId invalido.");
            return;
        }

        if (characterId <= 0) {
            error(user, "characterId invalido.");
            return;
        }

        if (user.getSession().getProperty(PENDING_CHARACTER_KEY) != null) {
            error(user, "Selecao ja em andamento.");
            return;
        }

        try {
            CharacterDAO dao = new CharacterDAO(
                getParentExtension().getParentZone().getDBManager()
            );

            CharacterData data = dao.findByIdAndUserId(characterId, userId);
            if (data == null) {
                error(user, "Personagem nao pertence a esta conta.");
                return;
            }

            // 1 = Warrior, 2 = Archer, 3 = Mage
            if (data.classId < 1 || data.classId > 3) {
                error(user, "Classe desconhecida.");
                return;
            }

            SidoExtension extension = (SidoExtension) getParentExtension();
            RoomSimulationContext target = extension.selectRoomForMap(data.mapId);

            if (target == null) {
                error(user, "Regiao indisponivel.");
                return;
            }

            /*if (!Float.isFinite(data.x)
                    || !Float.isFinite(data.y)
                    || !Float.isFinite(data.z)
                    || target.getNavMesh().findContainingPolygon(
                        data.x, data.y, data.z
                    ) == null) {
                error(user, "Posicao salva fora do NavMesh.");
                return;
            }*/

            // O UserJoinRoomHandler consome estes dados apos o ROOM_JOIN.
            user.getSession().setProperty(PENDING_CHARACTER_KEY, data);

            try {
            	
            	ISFSObject responseData = new SFSObject();
                
                // Injeta a lista física de personagens dentro do objeto com a chave "characters"
            	responseData.putLong("id", data.id);
                responseData.putString("name", data.name);
                responseData.putInt("classId", data.classId);
                responseData.putInt("level", data.level);
                responseData.putLong("totalExperience", data.totalExperience);
                responseData.putInt("mapid", data.mapId);
                responseData.putVector3("pos", new SFSVector3(data.x, data.y, data.z));
                responseData.putInt("hp", data.currentHealth);
                responseData.putInt("mana", data.currentMana);
                responseData.putInt("ag", data.currentAgility);
                
                responseData.putInt("s", data.strengthPoints);
                responseData.putInt("a", data.dextePoints);
                responseData.putInt("p", data.powerPoints);
                responseData.putInt("r", data.resourcePoints);
                
                // 5. Envia o pacote de rede diretamente para o cliente que acabou de conectar
                // Parâmetros: "comandoCustomizado", dadosDoComando, usuarioDestinatario
                send("characterLoaded", responseData, user);
            	
                //getApi().joinRoom(user, target.getRoom());
               
                
            } catch (Exception ex) {
                user.getSession().removeProperty(PENDING_CHARACTER_KEY);
                trace("Falha ao entrar na sala: " + ex.getMessage());
                error(user, "Nao foi possivel entrar na sala.");
            }

        } catch (SQLException ex) {
            trace("Erro ao selecionar personagem: " + ex.getMessage());
            error(user, "Erro ao consultar personagem.");
        }
    }

    private void error(User user, String message) {
        ISFSObject response = new SFSObject();
        response.putBool("success", false);
        response.putString("message", message);
        send("selectcharError", response, user);
    }
}
