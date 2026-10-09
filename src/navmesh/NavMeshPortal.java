package navmesh;

public class NavMeshPortal {

    public final NavMeshVertex left;
    public final NavMeshVertex right;

    public NavMeshPortal(
            NavMeshVertex left,
            NavMeshVertex right) {

        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {

        return "Portal{"
                + "Left=("
                + left.x + ", "
                + left.y + ", "
                + left.z + ")"
                + ", Right=("
                + right.x + ", "
                + right.y + ", "
                + right.z + ")"
                + '}';
    }
}