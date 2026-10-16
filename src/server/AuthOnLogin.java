package server;

import java.security.MessageDigest;
import java.sql.SQLException;

import com.smartfoxserver.core.ISFSEvent;
import com.smartfoxserver.core.SFSEventParam;
import com.smartfoxserver.db.IDBManager;
import com.smartfoxserver.entities.data.ISFSArray;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.exceptions.SFSErrorCode;
import com.smartfoxserver.exceptions.SFSErrorData;
import com.smartfoxserver.exceptions.SFSException;
import com.smartfoxserver.exceptions.SFSLoginException;
import com.smartfoxserver.extensions.BaseServerEventHandler;

public class AuthOnLogin extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
				
		/*// 1. Extrai os parâmetros enviados pelo cliente
		String username = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		String clearPasswordFromClient = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);

		// 2. Recupera o gerenciador do banco de dados configurado na Zone
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		try {
			String sql = "SELECT password FROM users WHERE username = ?";
			
			ISFSArray rows = dbManager.executeQuery(sql, new Object[] { username });

			if (rows.size() == 0) {
				SFSErrorData errorData = new SFSErrorData(SFSErrorCode.LOGIN_BAD_USERNAME);
				errorData.addParameter(username);
				throw new SFSLoginException("Usuário inválido.", errorData);
			}

			ISFSObject userRow = rows.getSFSObject(0);

			String hashedPasswordFromDB = userRow.getShortString("pword");

			// Transforma a senha digitada pelo usuário em um hash SHA-256 para comparação
			String hashedClientPassword = convertToSHA256(clearPasswordFromClient);

			// Compara os dois hashes hexadecimais
			if (!hashedClientPassword.equalsIgnoreCase(hashedPasswordFromDB)) {
				SFSErrorData errorData = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
				throw new SFSLoginException("Senha incorreta.", errorData);
			}

			trace("Login efetuado com sucesso via SHA-256 para o usuário: " + username);

		} catch (SQLException e) {
			trace("Erro de banco: " + e.getMessage());
			throw new SFSLoginException("Erro interno no servidor.", new SFSErrorData(SFSErrorCode.GENERIC_ERROR));
		}*/
		
		
	}

	private String convertToSHA256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			StringBuilder hexString = new StringBuilder();

			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Erro ao gerar o hash SHA-256", ex);
		}
	}

}
