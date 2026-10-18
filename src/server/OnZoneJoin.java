package server;

import java.sql.SQLException;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.db.IDBManager;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSArray;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.entities.data.SFSObject;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

public class OnZoneJoin extends BaseServerEventHandler {

    @Override
    public void handleServerEvent(ISFSEvent event) throws SFSException {
        User user = (User) event.getParameter(SFSEventParam.USER);
        Object storedId = user.getSession().getProperty(OnLogin.DATABASE_USER_ID_KEY);

        if (!(storedId instanceof Number)) {
            trace("ID persistente ausente na sessao de " + user.getName());
            ISFSObject errorData = new SFSObject();
            errorData.putBool("success", false);
            send("loadCharactersError", errorData, user);
            return;
        }

        long databaseUserId = ((Number) storedId).longValue();

        // 2. Recupera o gerenciador do banco de dados
        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        
        try {
            String sql = "SELECT id, name, class_id, level, map_id FROM characters WHERE user_id = ?";

            // O SFS3 executa a query e já mapeia o resultado do PostgreSQL em um ISFSArray automaticamente
            ISFSArray charList = dbManager.executeQuery(sql, new Object[] { databaseUserId });
            
            // 3. Cria o objeto de resposta que cruzará a rede até o cliente
            ISFSObject responseData = new SFSObject();
            
            // Injeta a lista física de personagens dentro do objeto com a chave "characters"
            responseData.putSFSArray("characters", charList);
            
            // 5. Envia o pacote de rede diretamente para o cliente que acabou de conectar
            // Parâmetros: "comandoCustomizado", dadosDoComando, usuarioDestinatario
            send("loadCharacters", responseData, user);
            
            trace("Lista com " + charList.size() + " personagens enviada para o usuário: " + user.getName());
            
        } catch (SQLException e) {
            trace("Erro ao carregar os personagens no OnZoneJoin: " + e.getMessage());
            
            // Opcional: Enviar um aviso de erro para o cliente tratar na interface
            ISFSObject errorData = new SFSObject();
            errorData.putBool("success", false);
            send("loadCharactersError", errorData, user);
        } 
        
  
        // 6. Joga o usuário para dentro da sala de seleção
        //getApi().joinRoom(user, targetRoom);
            
    }
}
