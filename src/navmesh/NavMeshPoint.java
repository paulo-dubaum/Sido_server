package navmesh;

public class NavMeshPoint {

    public final float x;
    public final float y;
    public final float z;

    public NavMeshPoint(
            float x,
            float y,
            float z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public NavMeshPoint(
            NavMeshVertex vertex) {

        this(
                vertex.x,
                vertex.y,
                vertex.z
        );
    }

    @Override
    public String toString() {

        return "("
                + x + ", "
                + y + ", "
                + z + ")";
    }
}