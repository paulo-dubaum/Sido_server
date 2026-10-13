package server;

import com.smartfoxserver.extensions.SFSExtension;
import com.smartfoxserver.mmo.MMORoom;

public class MmoRoomExtension extends SFSExtension {
    
    private MMORoom mmoRoom;

    @Override
    public void init() {
    	
        // Obtém a referência da MMORoom atual vinculada
        this.mmoRoom = (MMORoom) getParentRoom();
        

    }
}
