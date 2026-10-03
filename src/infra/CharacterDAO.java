package infra;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.smartfoxserver.db.IDBManager;
import com.smartfoxserver.entities.data.ISFSArray;
import com.smartfoxserver.entities.data.ISFSObject;

import utils.CharacterModel;

public class CharacterDAO {
	
    private final IDBManager dbManager;

    // Passamos o gerenciador do SFS no construtor
    public CharacterDAO(IDBManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<CharacterModel> getCharactersByAccount(String username) throws SQLException {
        List<CharacterModel> list = new ArrayList<>();
        String sql = "SELECT id, nome, classe, exp, mapa FROM user_chars WHERE username_coluna = ?";
        
        ISFSArray rows = dbManager.executeQuery(sql, new Object[] { username });
        
        for (int i = 0; i < rows.size(); i++) {
            ISFSObject row = rows.getSFSObject(i);
            
            CharacterModel character = new CharacterModel(
                row.getInt("id"),
                row.getShortString("nome"), // Padrão SFS3 para strings curtas
                row.getShortString("classe"),
                row.getInt("exp"),
                row.getShortString("mapa")
            );
            list.add(character);
        }
        
        return list;
    }
    
    public CharacterModel getCharactersByID(int id) throws SQLException {
    	
    	CharacterModel character = new CharacterModel(id, "", "", 0, "");
    	
    	String sql = "SELECT classe, exp, mapa FROM characeter WHERE id = ?";
		
		ISFSArray rows = dbManager.executeQuery(sql, new Object[] { id });
		
		if (rows.size() > 0) {
			ISFSObject row = rows.getSFSObject(0);
			
			character.setNome(row.getShortString("nome"));
			character.setClasse(row.getShortString("classe"));
			character.setExp(row.getInt("exp"));
			character.setMapa(row.getShortString("mapa"));
		}
		
		return character;
    }
}
