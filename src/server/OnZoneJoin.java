package server;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.entities.Room;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

public class OnZoneJoin extends BaseServerEventHandler {

    @Override
    public void handleServerEvent(ISFSEvent event) throws SFSException {
        // 1. Recupera o usuário que acabou de logar com sucesso
        User user = (User) event.getParameter(SFSEventParam.USER);
        
        
        // 2. Busca a sala "charSelection" na Zone atual
        //Room targetRoom = getParentExtension().getParentZone().getRoomByName("CharSelection");
        Room targetRoom = getParentExtension().getParentZone().getRoomByName("Norencia");
       
        /*
        // 3. Recupera o gerenciador do banco de dados
        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        
        try {
            // Consulta os personagens baseando-se no nome do usuário (ou ID do usuário se você tiver)
            // IMPORTANTE: Altere "user_chars" e "username_coluna" de acordo com o mapeamento exato da sua tabela
            String sql = "SELECT id, nome, classe, exp, mapa FROM user_chars WHERE username_coluna = ?";
            
            // O SFS3 executa a query e já mapeia o resultado do PostgreSQL em um ISFSArray automaticamente
            ISFSArray charList = dbManager.executeQuery(sql, new Object[] { user.getName() });
            
            // 4. Cria o objeto de resposta que cruzará a rede até o cliente
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
        } */
        
  
        // 6. Joga o usuário para dentro da sala de seleção
        //getApi().joinRoom(user, targetRoom);
            
    }
}
