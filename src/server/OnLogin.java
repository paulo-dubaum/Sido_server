package server;

import java.security.MessageDigest;
import java.sql.SQLException;

import com.smartfoxserver.bitswarm.sessions.ISession;
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

public class OnLogin extends BaseServerEventHandler {

    public static final String DATABASE_USER_ID_KEY = "databaseUserId";

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {

		String loginName = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_IN_DATA);
		String clearPasswordFromClient = outData.getShortString("passwd");
		ISession session = (ISession) event.getParameter(SFSEventParam.SESSION);

		

		// 2. Recupera o gerenciador do banco de dados configurado na Zone
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		try {
			String sql = "SELECT id, password FROM users WHERE username = ?";

			ISFSArray rows = dbManager.executeQuery(sql, new Object[] { loginName });

			if (rows.size() == 0) {
				SFSErrorData errorData = new SFSErrorData(SFSErrorCode.LOGIN_BAD_USERNAME);
				errorData.addParameter(loginName);
				throw new SFSLoginException("Usuário inválido.", errorData);
			}

			ISFSObject userRow = rows.getSFSObject(0);

			String hashedPasswordFromDB = userRow.getShortString("password");

			// Transforma a senha digitada pelo usuário em um hash SHA-256 para comparação
			String hashedClientPassword = convertToSHA256(clearPasswordFromClient);



			// Compara os dois hashes hexadecimais
			if (!hashedClientPassword.equalsIgnoreCase(hashedPasswordFromDB)) {
				SFSErrorData errorData = new SFSErrorData(SFSErrorCode.LOGIN_BAD_PASSWORD);
				throw new SFSLoginException("Senha incorreta.", errorData);
			}

			// O ID persistente e armazenado apenas apos a autenticacao.
			Long databaseUserId = userRow.getLong("id");
			session.setProperty(DATABASE_USER_ID_KEY, databaseUserId);
			trace("Login efetuado com sucesso para o usuario: " + loginName);

		} catch (SQLException e) {
			trace("Erro de banco: " + e.getMessage());
			throw new SFSLoginException("Erro interno no servidor.", new SFSErrorData(SFSErrorCode.GENERIC_ERROR));
		}
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
