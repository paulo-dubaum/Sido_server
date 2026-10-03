package navmesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import world.ServerNavMesh;

public class NavMeshSpatialGrid {

    private static final float CELL_SIZE = 10.0f;

    private final ServerNavMesh navMesh;

    private final Map<Long, List<NavMeshPolygon>> cells =
            new HashMap<>();

    public NavMeshSpatialGrid(ServerNavMesh navMesh) {

        this.navMesh = navMesh;

        build();
    }

    // =========================================================
    // BUILD
    // =========================================================

    private void build() {

        for (NavMeshPolygon polygon :
                navMesh.getPolygons().values()) {

            NavMeshVertex a =
                    navMesh.getVertex(
                            polygon.vertexA
                    );

            NavMeshVertex b =
                    navMesh.getVertex(
                            polygon.vertexB
                    );

            NavMeshVertex c =
                    navMesh.getVertex(
                            polygon.vertexC
                    );

            float minX =
                    Math.min(
                            a.x,
                            Math.min(b.x, c.x)
                    );

            float maxX =
                    Math.max(
                            a.x,
                            Math.max(b.x, c.x)
                    );

            float minZ =
                    Math.min(
                            a.z,
                            Math.min(b.z, c.z)
                    );

            float maxZ =
                    Math.max(
                            a.z,
                            Math.max(b.z, c.z)
                    );

            int minCellX =
                    worldToCell(minX);

            int maxCellX =
                    worldToCell(maxX);

            int minCellZ =
                    worldToCell(minZ);

            int maxCellZ =
                    worldToCell(maxZ);

            /*
             * Um triângulo pode atravessar mais
             * de uma célula.
             *
             * Portanto ele deve ser registrado
             * em todas as células tocadas pelo
             * seu bounding box.
             */
            for (int cellX = minCellX;
                 cellX <= maxCellX;
                 cellX++) {

                for (int cellZ = minCellZ;
                     cellZ <= maxCellZ;
                     cellZ++) {

                    addPolygon(
                            cellX,
                            cellZ,
                            polygon
                    );
                }
            }
        }
    }

    private void addPolygon(
            int cellX,
            int cellZ,
            NavMeshPolygon polygon) {

        long key =
                createKey(
                        cellX,
                        cellZ
                );

        cells.computeIfAbsent(
                key,
                ignored -> new ArrayList<>()
        ).add(polygon);
    }

    // =========================================================
    // QUERY
    // =========================================================

    public List<NavMeshPolygon> getPolygonsAt(
            float x,
            float z) {

        int cellX =
                worldToCell(x);

        int cellZ =
                worldToCell(z);

        long key =
                createKey(
                        cellX,
                        cellZ
                );

        List<NavMeshPolygon> result =
                cells.get(key);

        if (result == null)
            return Collections.emptyList();

        return result;
    }

    // =========================================================
    // GRID
    // =========================================================

    private int worldToCell(float value) {

        return (int)Math.floor(
                value / CELL_SIZE
        );
    }

    private long createKey(
            int cellX,
            int cellZ) {

        return
                ((long)cellX << 32)
                        ^
                (cellZ & 0xffffffffL);
    }

    // =========================================================
    // DEBUG
    // =========================================================

    public float getCellSize() {
        return CELL_SIZE;
    }

    public int getCellCount() {
        return cells.size();
    }
}
