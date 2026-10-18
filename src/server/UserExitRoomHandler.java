package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.Room;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

/** Limpa imediatamente alvos e registro quando um jogador sai da MMORoom. */
public class UserExitRoomHandler extends BaseServerEventHandler {
    @Override
    public void handleServerEvent(ISFSEvent event) throws SFSException {
        User user = (User) event.getParameter(SFSEventParam.USER);
        Room room = (Room) event.getParameter(SFSEventParam.ROOM);
        if (user == null || room == null) return;
        RoomSimulationContext context = ((SidoExtension) getParentExtension()).getRoomContext(room.getId());
        if (context != null) RoomCharacterSupport.remove(user, context);
    }
}
