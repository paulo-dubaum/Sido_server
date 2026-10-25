package server;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.variables.UserVariable;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;
import com.smartfoxserver.mmo.Vec3D;
import character.Character;
import navmesh.NavMeshPolygon;

public class UserVariableUpdate extends BaseServerEventHandler {
    @Override
    public void handleServerEvent(ISFSEvent event) throws SFSException {
    	
        User user = (User) event.getParameter(SFSEventParam.USER);
        
        trace("Aeeeeeeeeeeeeeeee666");
        
        if (user == null) return;

        // O evento tambem pode ser disparado por variaveis nao relacionadas ao movimento.
        UserVariable xv = user.getVariable("x");
        UserVariable yv = user.getVariable("y");
        UserVariable zv = user.getVariable("z");
        if (xv == null || yv == null || zv == null) return;

        Float x = xv.getFloatValue();
        Float y = yv.getFloatValue();
        Float z = zv.getFloatValue();
        if (x == null || y == null || z == null ||
                !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return;
        
        trace("Aeeeeeeeeeeeeeeee2222222222222");

        SidoExtension extension = (SidoExtension) getParentExtension();
        // Procura o contexto que realmente possui o personagem.
        // Nao assume que Norencia e a sala atual.
        for (RoomSimulationContext context : extension.getRoomContexts()) {
            synchronized (context) {
                Character character = context.getCharacterManager().getByUser(user);
                
                trace("IIIIIIIIIII");
                
                
                if (character == null) continue;
                
                trace("OOO");
                
                trace(context.getRoom() + " " + user.getJoinedRooms());

                // Rejeita movimentos de um contexto do qual o usuario ja saiu.
                if (!user.getJoinedRooms().contains(context.getRoom())) return;

                NavMeshPolygon polygon = context.getNavMesh().findContainingPolygon(x, y, z);
                character.setPosition(x, y, z);
                character.setCurrentPolygonId(polygon == null ? -1 : polygon.id);
                
                trace("Aeeeeeeeeeeeeeeee");

                SmartFoxServer.getInstance().getAPIManager().getMMOApi()
                        .setUserPosition(user, new Vec3D(x, y, z), context.getRoom());
                return;
            }
        }
    }
}
