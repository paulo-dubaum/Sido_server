package server;

import sfs3.client.*;
import sfs3.client.core.*;
import sfs3.client.requests.*;

public class SFS3Connector {

	SmartFox sfs;
    ConfigData cfg;

    public SFS3Connector()
    {
        // Configure client connection settings
        cfg = new ConfigData();
        cfg.host = "localhost";
        cfg.zone = "Playground";

        // Set up event handlers
        sfs = new SmartFox();
        sfs.addEventListener(SFSEvent.CONNECTION, this::onConnection);
        sfs.addEventListener(SFSEvent.CONNECTION_LOST, this::onConnectionLost);
        sfs.addEventListener(SFSEvent.LOGIN, this::onLogin);
        sfs.addEventListener(SFSEvent.LOGIN_ERROR, this::onLoginError);

        System.out.println("API Ver: " + sfs.getVersion());

        // Connect to the server
        sfs.connect(cfg);
    }
    
 // ----------------------------------------------------------------------
    // Event Handlers
    // ----------------------------------------------------------------------

    private void onConnection(ApiEvent evt)
    {
        var success = (Boolean) evt.getParam(EventParam.Success);

        if (success)
        {
            System.out.println("Connection success");

            // Login as guest
            sfs.send(new LoginRequest(""));
        }
        else
            System.out.println("Connection Failed. Is the server running?"); 
    }

    private void onConnectionLost(ApiEvent evt)
    {
        System.out.println("Connection was lost");
    }

    private void onLogin(ApiEvent evt)
    {
        System.out.println("Logged in as: " + sfs.getMySelf().getName());
    }

    private void onLoginError(ApiEvent evt)
    {
        var message = (String) evt.getParam(EventParam.ErrorMessage);
        System.out.println("Login failed. Error: " + message);
    }

}
