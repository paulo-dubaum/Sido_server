package server;

import java.util.Arrays;

import com.smartfoxserver.components.signup.ISignUpAssistantPlugin;
import com.smartfoxserver.components.signup.PasswordMode;
import com.smartfoxserver.components.signup.SignUpAssistantComponent;
import com.smartfoxserver.components.signup.SignUpConfiguration;
import com.smartfoxserver.components.signup.SignUpErrorCodes;
import com.smartfoxserver.components.signup.SignUpValidationException;
import com.smartfoxserver.core.SFSEventType;
import com.smartfoxserver.entities.User;
import com.smartfoxserver.entities.data.ISFSObject;
import com.smartfoxserver.extensions.SFSExtension;

/** Extensao da Zone Auth: cadastro de contas, sem dependencias do mundo. */
public final class AuthExtension extends SFSExtension {
    private SignUpAssistantComponent signUpAssistant;

    @Override
    public void init() {
        signUpAssistant = new SignUpAssistantComponent();
        SignUpConfiguration config = signUpAssistant.getConfig();
       
        config.minUserNameLength = 4;
        config.maxUserNameLength = 30;
        config.minPasswordLength = 6;
        config.maxPasswordLength = 30;
        // Mantido apenas para compatibilidade com a configuracao atual.
        // Revisar armazenamento de senhas antes de producao.
        config.passwordMode = PasswordMode.SHA256;
        
        addRequestHandler(SignUpAssistantComponent.COMMAND_PREFIX, signUpAssistant);
        trace("AuthExtension iniciada: cadastro registrado na Zone Auth.");
    }

    @Override
    public void destroy() {
        signUpAssistant = null;
        super.destroy();
    }
}
