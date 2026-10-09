package server;

import java.util.Arrays;
import java.util.List;

import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;
import com.smartfoxserver.mmo.MMOItemVariable;
import com.smartfoxserver.mmo.MMORoom;
import com.smartfoxserver.mmo.Vec3D;

import character.CharacterManager;
import navmesh.NavMeshPolygon;
import world.ServerNavMesh;
import character.Character;

public class UserVariableUpdate extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		
		 // 1. Recupera o usuário que acabou de logar com sucesso
        User user = (User) event.getParameter(SFSEventParam.USER);
        MMORoom room = (MMORoom) getParentExtension()
                .getParentZone()
                .getRoomByName("Norencia");
        
        float x = user.getVariable("x").getFloatValue();
        float y = user.getVariable("y").getFloatValue();
        float z = user.getVariable("z").getFloatValue();
        
        
        CharacterManager characterManager =
                ((SidoExtension) getParentExtension())
                        .getCharacterManager();
        
        Character character =
                characterManager.getByUser(user);

        character.setPosition(
                x,
                y,
                z
        );
        
        SmartFoxServer.getInstance()
        .getAPIManager()
        .getMMOApi()
        .setUserPosition(
                user,
                new Vec3D(x, y, z),
                room
        );
        

        
        
        ServerNavMesh navMesh =
               ((SidoExtension) getParentExtension())
						.getNavMesh();

        NavMeshPolygon polygon =
                navMesh.findContainingPolygon(
                        x,
                        y,
                        z
                );

        if (polygon != null) {

            character.setCurrentPolygonId(polygon.id);

        } else {

            character.setCurrentPolygonId(-1);

            System.out.println(
                    "Character "
                            + character.getCharacterId()
                            + " fora do NavMesh."
            );
        }
		
	}
}
