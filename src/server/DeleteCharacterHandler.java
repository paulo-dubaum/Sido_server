package server;

import java.sql.SQLException;
import com.smartfoxserver.bitswarm.data.TransportType;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.extensions.BaseClientRequestHandler;
import character.Character;
import infra.CharacterLifecycleDAO;

public final class DeleteCharacterHandler extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User user, ISFSObject params, TransportType transport) {
		if (user == null)
			return;
		Object account = user.getSession().getProperty(OnLogin.DATABASE_USER_ID_KEY);
		if (!(account instanceof Number)) {
			reply(user, false, "NOT_AUTHENTICATED", "Sessao nao autenticada.");
			return;
		}
		if (params == null) {
			reply(user, false, "INVALID_PARAMETERS", "Parametros ausentes.");
			return;
		}
		try {
			long id = params.getLong("characterId");
			if (id <= 0) {
				reply(user, false, "INVALID_CHARACTER_ID", "characterId invalido.");
				return;
			}
			Object pending = user.getSession().getProperty(CharacterSelectHandler.PENDING_CHARACTER_KEY);
			if (pending != null) {
				reply(user, false, "SELECTION_IN_PROGRESS", "Selecao em andamento.");
				return;
			}
			SidoExtension ext = (SidoExtension) getParentExtension();
			for (RoomSimulationContext context : ext.getRoomContexts()) {
				synchronized (context) {
					Character active = context.getCharacterManager().getByUser(user);
					if (active != null) {
						reply(user, false, "CHARACTER_ACTIVE", "Saia do mundo(mmoroom) antes de excluir personagens.");
						return;
					}
				}
			}
			boolean deleted = new CharacterLifecycleDAO(getParentExtension().getParentZone().getDBManager())
					.delete(((Number) account).longValue(), id);
			reply(user, deleted, deleted ? "OK" : "CHARACTER_NOT_FOUND",
					deleted ? "Personagem excluido." : "Personagem nao encontrado ou nao pertence a conta.");

		} catch (SQLException ex) {
			trace("Falha ao excluir personagem: " + ex.getMessage());
			reply(user, false, "DATABASE_ERROR", "Nao foi possivel excluir personagem.");

		} catch (Exception ex) {
			reply(user, false, "INVALID_PARAMETERS", "Parametros invalidos.");
		}
	}

	private void reply(User user, boolean success, String code, String message) {
		ISFSObject out = new SFSObject();
		out.putBool("success", success);
		out.putString("code", code);
		out.putString("message", message);
		send("deleteCharacterResult", out, user);
	}

}
