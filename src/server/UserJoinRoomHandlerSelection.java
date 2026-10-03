package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

public class UserJoinRoomHandlerSelection extends BaseServerEventHandler {

    @Override
    public void handleServerEvent(ISFSEvent event) throws SFSException {

        User user = (User) event.getParameter(SFSEventParam.USER);

        // Aqui o usuário acabou de entrar na sala de seleção.
        // Ainda não existe necessariamente um personagem selecionado.

        //getLogger().info(
        //    "Usuário " + user.getName() +
        //    " entrou na sala de seleção."
        //);
    }
}