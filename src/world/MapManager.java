package world;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import navmesh.NavMeshLoader;

public class MapManager {

	 private final Map<String, ServerMap> maps =
	            new HashMap<>();

	    public void loadMap(
	            String mapId,
	            Path navMeshPath)
	            throws IOException {

	        ServerNavMesh navMesh =
	                NavMeshLoader.load(
	                        navMeshPath
	                );

	        ServerMap map =
	                new ServerMap(
	                        mapId,
	                        navMesh
	                );

	        maps.put(
	                mapId,
	                map
	        );
	    }

	    public ServerMap getMap(String mapId) {
	        return maps.get(mapId);
	    }
	    
}
