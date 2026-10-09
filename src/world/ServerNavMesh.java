package world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import navmesh.NavMeshPolygon;
import navmesh.NavMeshPortal;
import navmesh.NavMeshSpatialGrid;
import navmesh.NavMeshVertex;



public class ServerNavMesh {

    private final List<NavMeshVertex> vertices;
    private final Map<Integer, NavMeshPolygon> polygons;
    private final NavMeshSpatialGrid spatialGrid;

    public ServerNavMesh(
            List<NavMeshVertex> vertices,
            List<NavMeshPolygon> polygonList) {

        this.vertices = vertices;

        this.polygons = new HashMap<>();

        for (NavMeshPolygon polygon : polygonList) {
            polygons.put(
                    polygon.id,
                    polygon
            );
        }
        
        for (NavMeshPolygon polygon : polygonList) {
            polygons.put(
                    polygon.id,
                    polygon
            );
        }
        
        this.spatialGrid =
                new NavMeshSpatialGrid(this);
    }

    public NavMeshVertex getVertex(int id) {

        if (id < 0 || id >= vertices.size()) {
            return null;
        }

        return vertices.get(id);
    }

    public NavMeshPolygon getPolygon(int id) {

        if (id < 0 || id >= polygons.size()) {
            return null;
        }

        return polygons.get(id);
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getPolygonCount() {
        return polygons.size();
    }

    public Map<Integer, NavMeshPolygon> getPolygons() {
        return polygons;
    }

    // =========================================================
    // FIND CONTAINING POLYGON
    // =========================================================

    public NavMeshPolygon findContainingPolygon(
            float x,
            float y,
            float z) {

        List<NavMeshPolygon> candidates =
                spatialGrid.getPolygonsAt(
                        x,
                        z
                );

        /*System.out.println(
                "SpatialGrid candidates: "
                        + candidates.size()
        );*/

        for (NavMeshPolygon polygon : candidates) {

            NavMeshVertex a =
                    vertices.get(polygon.vertexA);

            NavMeshVertex b =
                    vertices.get(polygon.vertexB);

            NavMeshVertex c =
                    vertices.get(polygon.vertexC);

            if (containsPoint(
                    x,
                    y,
                    z,
                    a,
                    b,
                    c)) {

                return polygon;
            }
        }

        return null;
    }
    // =========================================================
    // POINT / TRIANGLE TEST
    // =========================================================

    private boolean containsPoint(
            float px,
            float py,
            float pz,
            NavMeshVertex a,
            NavMeshVertex b,
            NavMeshVertex c) {

        // -----------------------------------------------------
        // 1. Primeiro fazemos o teste no plano XZ.
        // -----------------------------------------------------

        float v0x = c.x - a.x;
        float v0z = c.z - a.z;

        float v1x = b.x - a.x;
        float v1z = b.z - a.z;

        float v2x = px - a.x;
        float v2z = pz - a.z;

        float dot00 =
                v0x * v0x +
                v0z * v0z;

        float dot01 =
                v0x * v1x +
                v0z * v1z;

        float dot02 =
                v0x * v2x +
                v0z * v2z;

        float dot11 =
                v1x * v1x +
                v1z * v1z;

        float dot12 =
                v1x * v2x +
                v1z * v2z;

        float denominator =
                dot00 * dot11 -
                dot01 * dot01;

        // Triângulo degenerado
        if (Math.abs(denominator) < 0.000001f)
            return false;

        float inverse =
                1.0f / denominator;

        float u =
                (dot11 * dot02 -
                 dot01 * dot12) *
                inverse;

        float v =
                (dot00 * dot12 -
                 dot01 * dot02) *
                inverse;

        final float epsilon = 0.001f;

        if (u < -epsilon)
            return false;

        if (v < -epsilon)
            return false;

        if (u + v > 1.0f + epsilon)
            return false;

        // -----------------------------------------------------
        // 2. Descobrimos a altura Y da superfície do triângulo.
        // -----------------------------------------------------

        float yOnTriangle =
                interpolateY(
                        px,
                        pz,
                        a,
                        b,
                        c
                );

        // -----------------------------------------------------
        // 3. Comparamos com a posição procurada.
        // -----------------------------------------------------

        float yTolerance = 0.3f;

        return Math.abs(
                py - yOnTriangle
        ) <= yTolerance;
    }

    // =========================================================
    // INTERPOLAÇÃO DA ALTURA
    // =========================================================

    private float interpolateY(
            float px,
            float pz,
            NavMeshVertex a,
            NavMeshVertex b,
            NavMeshVertex c) {

        float v0x = b.x - a.x;
        float v0z = b.z - a.z;

        float v1x = c.x - a.x;
        float v1z = c.z - a.z;

        float v2x = px - a.x;
        float v2z = pz - a.z;

        float denominator =
                v0x * v1z -
                v1x * v0z;

        if (Math.abs(denominator) < 0.000001f)
            return a.y;

        float u =
                (v2x * v1z -
                 v1x * v2z) /
                denominator;

        float v =
                (v0x * v2z -
                 v2x * v0z) /
                denominator;

        float w =
                1.0f - u - v;

        return
                a.y * w +
                b.y * u +
                c.y * v;
    }
    
    public NavMeshPortal findPortal(
            int polygonAId,
            int polygonBId) {

        NavMeshPolygon polygonA =
                getPolygon(polygonAId);

        NavMeshPolygon polygonB =
                getPolygon(polygonBId);

        if (polygonA == null || polygonB == null) {
            return null;
        }

        int[] verticesA = {
                polygonA.vertexA,
                polygonA.vertexB,
                polygonA.vertexC
        };

        int[] verticesB = {
                polygonB.vertexA,
                polygonB.vertexB,
                polygonB.vertexC
        };

        int firstShared = -1;
        int secondShared = -1;

        for (int vertexA : verticesA) {

            for (int vertexB : verticesB) {

                if (vertexA != vertexB) {
                    continue;
                }

                if (firstShared == -1) {

                    firstShared = vertexA;

                } else if (vertexA != firstShared) {

                    secondShared = vertexA;
                }
            }
        }

        if (firstShared == -1
                || secondShared == -1) {

            return null;
        }

        NavMeshVertex portalA =
                getVertex(firstShared);

        NavMeshVertex portalB =
                getVertex(secondShared);

        // Direção do corredor A -> B
        float directionX =
                polygonB.centerX - polygonA.centerX;

        float directionZ =
                polygonB.centerZ - polygonA.centerZ;

        // Posição de A em relação ao centro
        // do primeiro polígono.
        float pointX =
                portalA.x - polygonA.centerX;

        float pointZ =
                portalA.z - polygonA.centerZ;

        float cross =
                directionX * pointZ
                - directionZ * pointX;

        if (cross > 0.0f) {

            return new NavMeshPortal(
                    portalA,
                    portalB
            );

        } else {

            return new NavMeshPortal(
                    portalB,
                    portalA
            );
        }
    }
    
    public List<NavMeshPortal> buildPortals(
            List<Integer> polygonPath) {

        List<NavMeshPortal> portals =
                new ArrayList<>();

        if (polygonPath == null
                || polygonPath.size() < 2) {

            return portals;
        }

        for (int i = 0;
             i < polygonPath.size() - 1;
             i++) {

            int polygonA =
                    polygonPath.get(i);

            int polygonB =
                    polygonPath.get(i + 1);

            NavMeshPortal portal =
                    findPortal(
                            polygonA,
                            polygonB
                    );

            if (portal == null) {

                System.out.println(
                        "Portal não encontrado: "
                                + polygonA
                                + " -> "
                                + polygonB
                );

                continue;
            }

            portals.add(portal);
        }

        return portals;
    }
}

