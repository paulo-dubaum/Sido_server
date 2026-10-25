package server;

import java.sql.SQLException;
import com.smartfoxserver.bitswarm.data.TransportType;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.extensions.BaseClientRequestHandler;
import infra.CharacterLifecycleDAO;

public final class CreateCharacterHandler extends BaseClientRequestHandler {
	// CONFIGURAR com uma posicao REAL validada no NavMesh do mapa 0.
	private static final int SPAWN_MAP = 0;
	private static final float SPAWN_X = 245.0f;
	private static final float SPAWN_Y = 6.039f;
	private static final float SPAWN_Z = 167.5f;

	@Override
	public void handleClientRequest(User user, ISFSObject params, TransportType transport) {
		if (user == null)
			return;
		
		
		Object account = user.getSession().getProperty(OnLogin.DATABASE_USER_ID_KEY);
		if (!(account instanceof Number)) {
			reply(user, false, "NOT_AUTHENTICATED", "Sessao nao autenticada.", 0);
			return;
		}
		if (params == null) {
			reply(user, false, "INVALID_PARAMETERS", "Parametros ausentes.", 0); 
			return;
		}
		
		
		try {
			String name = params.getString("name");
			int slot = params.getInt("slot");
			int classId = params.getInt("classId");
			
			
			if (name == null || !name.matches("[A-Za-z0-9_]{4,16}")) {
				 reply(user, false, "INVALID_NAME", "Nome deve ter 4 a 16 caracteres ASCII, numeros ou _.", 0);
				return;
			}
			if (slot < 1 || slot > 7 || classId < 1 || classId > 3) {
				reply(user, false, "INVALID_CLASS_OR_SLOT", "Slot ou classe invalido.", 0);
				return;
			}
			
			
			SidoExtension ext = (SidoExtension) getParentExtension();
			
			RoomSimulationContext context = ext.selectRoomForMap(SPAWN_MAP);
			
			if (context == null || !Float.isFinite(SPAWN_X) || !Float.isFinite(SPAWN_Y) || !Float.isFinite(SPAWN_Z)
					|| context.getNavMesh().findContainingPolygon(SPAWN_X, SPAWN_Y, SPAWN_Z) == null) {
				trace("Spawn inicial nao configurado ou fora do NavMesh.");
				reply(user, false, "SPAWN_UNAVAILABLE", "Spawn inicial nao configurado ou fora do NavMesh.", 0);
				return;
			}

			
			// Valores provisórios: substituir por configuracao de atributos iniciais por
			// classe.
			int health = 100, mana = 100, agility = 0;
			int strength = 0, dexterity = 0, intelligence = 0, vitality = 0;
			int available = 0;
			long id = new CharacterLifecycleDAO(getParentExtension().getParentZone().getDBManager()).create(
					((Number) account).longValue(), slot, name, classId, SPAWN_MAP, SPAWN_X, SPAWN_Y, SPAWN_Z, health,
					mana, agility, strength, dexterity, intelligence, vitality, available);
			reply(user, true, "OK", "Personagem criado.", id);
		} catch (SQLException ex) {
			 String code = classifySqlError(ex);

			reply(user, false, code, messageFor(code), 0);

		} catch (Exception ex) {
			reply(user, false, "INVALID_PARAMETERS", "Parametros invalidos.", 0);
		}
	}
	
	private static String classifySqlError(SQLException ex) {
        for (SQLException current = ex; current != null; current = current.getNextException()) {
            if ("23505".equals(current.getSQLState())) {
                String detail = String.valueOf(current.getMessage()).toLowerCase(java.util.Locale.ROOT);
                if (detail.contains("ux_characters_name")) return "NAME_ALREADY_EXISTS";
                if (detail.contains("uq_characters_user_slot")
                        || detail.contains("characters_user_id_slot_key")) return "SLOT_OCCUPIED";
                return "UNIQUE_CONFLICT";
            }
        }
        return "DATABASE_ERROR";
    }
	
	 private static String messageFor(String code) {
	        switch (code) {
	            case "NAME_ALREADY_EXISTS": return "Nome ja esta em uso.";
	            case "SLOT_OCCUPIED": return "Slot ja esta ocupado.";
	            case "UNIQUE_CONFLICT": return "Nome ou slot ja esta em uso.";
	            default: return "Nao foi possivel criar personagem.";
	        }
	    }


	 private void reply(User user, boolean success, String code, String message, long id) {
	        ISFSObject out = new SFSObject();
	        out.putBool("success", success); out.putString("code", code); out.putString("message", message);
	        if (success) out.putLong("characterId", id);
	        send("createCharacterResult", out, user);
	    }

}
