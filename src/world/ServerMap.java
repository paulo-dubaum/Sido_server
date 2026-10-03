package world;

public class ServerMap {
	
	private final String id;

    private final ServerNavMesh navMesh;

    public ServerMap(
            String id,
            ServerNavMesh navMesh) {

        this.id = id;
        this.navMesh = navMesh;
    }

    public String getId() {
        return id;
    }

    public ServerNavMesh getNavMesh() {
        return navMesh;
    }

}
