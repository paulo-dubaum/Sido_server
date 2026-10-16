package server;

import java.util.ArrayList;
import java.util.List;
import com.smartfoxserver.SmartFoxServer;
import com.smartfoxserver.entities.variables.RoomVariable;
import com.smartfoxserver.entities.variables.SFSRoomVariable;

/** Publica metadados da instancia e estado global do mundo. */
public final class RoomStatePublisher {
    private RoomStatePublisher() { }

    public static void initialize(RoomSimulationContext context, int worldTime) {
        List<RoomVariable> variables = new ArrayList<>();
        variables.add(new SFSRoomVariable("regionId", context.getRegionId()));
        variables.add(new SFSRoomVariable("instanceId", context.getInstanceId()));
        variables.add(new SFSRoomVariable("worldTime", worldTime));
        variables.add(new SFSRoomVariable("weatherId", context.getWeatherId()));
        SmartFoxServer.getInstance().getAPIManager().getSFSApi()
                .setRoomVariables(null, context.getRoom(), variables);
    }

    public static void publishWorldTime(RoomSimulationContext context, int worldTime) {
        List<RoomVariable> variables = new ArrayList<>(1);
        variables.add(new SFSRoomVariable("worldTime", worldTime));
        SmartFoxServer.getInstance().getAPIManager().getSFSApi()
                .setRoomVariables(null, context.getRoom(), variables);
    }

    public static void publishWeather(RoomSimulationContext context) {
        List<RoomVariable> variables = new ArrayList<>(1);
        variables.add(new SFSRoomVariable("weatherId", context.getWeatherId()));
        SmartFoxServer.getInstance().getAPIManager().getSFSApi()
                .setRoomVariables(null, context.getRoom(), variables);
    }
}
