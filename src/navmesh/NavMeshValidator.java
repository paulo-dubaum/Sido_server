package navmesh;


import java.util.HashSet;
import java.util.Set;

import world.ServerNavMesh;

public class NavMeshValidator {

    public static void validate(
            ServerNavMesh navMesh) {

        System.out.println(
                "======================================"
        );

        System.out.println(
                " NAVMESH SERVER VALIDATION"
        );

        System.out.println(
                "======================================"
        );

        // ----------------------------------------------------
        // TOTAL
        // ----------------------------------------------------

        int vertexCount =
                navMesh.getVertexCount();

        int polygonCount =
                navMesh.getPolygonCount();

        System.out.println(
                "Vertices:          "
                        + vertexCount
        );

        System.out.println(
                "Polygons:          "
                        + polygonCount
        );

        // ----------------------------------------------------
        // CONTADORES
        // ----------------------------------------------------

        int isolated = 0;
        int oneNeighbor = 0;
        int twoNeighbors = 0;
        int threeNeighbors = 0;
        int fourOrMore = 0;

        int invalidNeighborReferences = 0;

        int invalidVertexReferences = 0;

        int totalNeighborReferences = 0;

        // ----------------------------------------------------
        // POLYGONS
        // ----------------------------------------------------

        for (NavMeshPolygon polygon :
                navMesh.getPolygons().values()) {

            // ----------------------------------------------
            // VIZINHOS
            // ----------------------------------------------

            int neighborCount =
                    polygon.neighbors.size();

            totalNeighborReferences +=
                    neighborCount;

            if (neighborCount == 0) {

                isolated++;

            } else if (neighborCount == 1) {

                oneNeighbor++;

            } else if (neighborCount == 2) {

                twoNeighbors++;

            } else if (neighborCount == 3) {

                threeNeighbors++;

            } else {

                fourOrMore++;
            }

            // ----------------------------------------------
            // REFERÊNCIAS DOS VIZINHOS
            // ----------------------------------------------

            for (Integer neighborId :
                    polygon.neighbors) {

                if (neighborId == null ||
                    navMesh.getPolygon(neighborId) == null) {

                    invalidNeighborReferences++;
                }
            }

            // ----------------------------------------------
            // REFERÊNCIAS DOS VÉRTICES
            // ----------------------------------------------

            if (!validVertex(
                    polygon.vertexA,
                    vertexCount)) {

                invalidVertexReferences++;
            }

            if (!validVertex(
                    polygon.vertexB,
                    vertexCount)) {

                invalidVertexReferences++;
            }

            if (!validVertex(
                    polygon.vertexC,
                    vertexCount)) {

                invalidVertexReferences++;
            }
        }

        // ----------------------------------------------------
        // CONEXÕES ÚNICAS
        // ----------------------------------------------------

        int uniqueConnections =
                countUniqueConnections(navMesh);

        // ----------------------------------------------------
        // RESULTADO
        // ----------------------------------------------------

        System.out.println(
                "--------------------------------------"
        );

        System.out.println(
                "Conexões únicas:   "
                        + uniqueConnections
        );

        System.out.println(
                "Referências viz.:  "
                        + totalNeighborReferences
        );

        System.out.println(
                "Polígonos isolados:"
                        + isolated
        );

        System.out.println(
                "1 vizinho:         "
                        + oneNeighbor
        );

        System.out.println(
                "2 vizinhos:        "
                        + twoNeighbors
        );

        System.out.println(
                "3 vizinhos:        "
                        + threeNeighbors
        );

        System.out.println(
                "4+ vizinhos:       "
                        + fourOrMore
        );

        System.out.println(
                "Refs. viz. inválidas:"
                        + invalidNeighborReferences
        );

        System.out.println(
                "Refs. vértices inválidas:"
                        + invalidVertexReferences
        );

        System.out.println(
                "--------------------------------------"
        );

        // ----------------------------------------------------
        // CENTRO DOS POLÍGONOS
        // ----------------------------------------------------

        validateCenters(navMesh);

        // ----------------------------------------------------
        // AMOSTRA
        // ----------------------------------------------------

        printPolygon(
                navMesh,
                0
        );

        System.out.println(
                "======================================"
        );
        
        int asymmetricConnections =
                countAsymmetricConnections(
                        navMesh
                );

        System.out.println(
                "Conexões assimétricas: "
                        + asymmetricConnections);
        
        int duplicateNeighbors =
                countDuplicateNeighbors(navMesh);

        System.out.println(
                "Vizinhos duplicados: "
                        + duplicateNeighbors
        );
    }

    // ========================================================
    // VALIDAR VÉRTICE
    // ========================================================

    private static boolean validVertex(
            int vertexId,
            int vertexCount) {

        return
                vertexId >= 0 &&
                vertexId < vertexCount;
    }

    // ========================================================
    // CONEXÕES ÚNICAS
    // ========================================================

    private static int countUniqueConnections(
            ServerNavMesh navMesh) {

        Set<String> connections =
                new HashSet<>();

        for (NavMeshPolygon polygon :
                navMesh.getPolygons().values()) {

            for (Integer neighborId :
                    polygon.neighbors) {

                if (neighborId == null)
                    continue;

                int a =
                        Math.min(
                                polygon.id,
                                neighborId
                        );

                int b =
                        Math.max(
                                polygon.id,
                                neighborId
                        );

                connections.add(
                        a + ":" + b
                );
            }
        }

        return connections.size();
    }

    // ========================================================
    // VALIDAR CENTROS
    // ========================================================

    private static void validateCenters(
            ServerNavMesh navMesh) {

        int invalidCenters = 0;

        for (NavMeshPolygon polygon :
                navMesh.getPolygons().values()) {

            if (Float.isNaN(polygon.centerX) ||
                Float.isNaN(polygon.centerY) ||
                Float.isNaN(polygon.centerZ)) {

                invalidCenters++;
            }

            if (Float.isInfinite(polygon.centerX) ||
                Float.isInfinite(polygon.centerY) ||
                Float.isInfinite(polygon.centerZ)) {

                invalidCenters++;
            }
        }

        System.out.println(
                "Centros inválidos: "
                        + invalidCenters
        );
    }

    // ========================================================
    // MOSTRAR POLÍGONO
    // ========================================================

    private static void printPolygon(
            ServerNavMesh navMesh,
            int polygonId) {

        NavMeshPolygon polygon =
                navMesh.getPolygon(polygonId);

        if (polygon == null) {

            System.out.println(
                    "Polygon "
                            + polygonId
                            + " não encontrado."
            );

            return;
        }

        System.out.println(
                "--------------------------------------"
        );

        System.out.println(
                "Polygon ID: "
                        + polygon.id
        );

        System.out.println(
                "Vertices: "
                        + polygon.vertexA
                        + ", "
                        + polygon.vertexB
                        + ", "
                        + polygon.vertexC
        );

        System.out.println(
                "Center: ("
                        + polygon.centerX
                        + ", "
                        + polygon.centerY
                        + ", "
                        + polygon.centerZ
                        + ")"
        );

        System.out.println(
                "Area: "
                        + polygon.area
        );

        System.out.println(
                "Neighbors: "
                        + polygon.neighbors
        );

        System.out.println(
                "--------------------------------------"
        );
    }
    
    private static int countAsymmetricConnections(
            ServerNavMesh navMesh) {

        int errors = 0;

        for (NavMeshPolygon polygon :
                navMesh.getPolygons().values()) {

            for (Integer neighborId :
                    polygon.neighbors) {

                NavMeshPolygon neighbor =
                        navMesh.getPolygon(neighborId);

                if (neighbor == null)
                    continue;

                if (!neighbor.neighbors.contains(polygon.id)) {

                    errors++;

                    System.out.println(
                            "Conexão assimétrica: "
                                    + polygon.id
                                    + " -> "
                                    + neighborId
                    );
                }
            }
        }

        return errors;
    }
    
    private static int countDuplicateNeighbors(
            ServerNavMesh navMesh) {

        int errors = 0;

        for (NavMeshPolygon polygon :
                navMesh.getPolygons().values()) {

            Set<Integer> unique =
                    new HashSet<>();

            for (Integer neighborId :
                    polygon.neighbors) {

                if (!unique.add(neighborId)) {

                    errors++;

                    System.out.println(
                            "Vizinho duplicado: Polygon "
                                    + polygon.id
                                    + " -> "
                                    + neighborId
                    );
                }
            }
        }

        return errors;
    }
}

