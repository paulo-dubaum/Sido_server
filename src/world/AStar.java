package world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import navmesh.NavMeshPolygon;

public class AStar {

    // ========================================================
    // PUBLIC API
    // ========================================================

    public static List<Integer> findPath(
            ServerNavMesh navMesh,
            int startPolygonId,
            int targetPolygonId) {

        // ----------------------------------------------------
        // VALIDAR POLÍGONOS
        // ----------------------------------------------------

        NavMeshPolygon start =
                navMesh.getPolygon(startPolygonId);

        NavMeshPolygon target =
                navMesh.getPolygon(targetPolygonId);
        
        

        if (start == null) {

            System.out.println(
                    "A*: Start polygon não encontrado: "
                            + startPolygonId
            );

            return Collections.emptyList();
        }

        if (target == null) {

            System.out.println(
                    "A*: Target polygon não encontrado: "
                            + targetPolygonId
            );

            return Collections.emptyList();
        }

        // ----------------------------------------------------
        // MESMO POLÍGONO
        // ----------------------------------------------------

        if (startPolygonId == targetPolygonId) {

            List<Integer> path =
                    new ArrayList<>();

            path.add(startPolygonId);

            return path;
        }

        // ----------------------------------------------------
        // OPEN SET
        // ----------------------------------------------------

        PriorityQueue<Node> openSet =
                new PriorityQueue<>();

        // ----------------------------------------------------
        // G SCORE
        // ----------------------------------------------------

        Map<Integer, Float> gScore =
                new HashMap<>();

        // ----------------------------------------------------
        // F SCORE
        // ----------------------------------------------------

        Map<Integer, Float> fScore =
                new HashMap<>();

        // ----------------------------------------------------
        // PARENT
        // ----------------------------------------------------

        Map<Integer, Integer> cameFrom =
                new HashMap<>();

        // ----------------------------------------------------
        // CLOSED SET
        // ----------------------------------------------------

        Set<Integer> closedSet =
                new HashSet<>();

        // ----------------------------------------------------
        // START
        // ----------------------------------------------------

        gScore.put(
                startPolygonId,
                0.0f
        );

        float startHeuristic =
                distance(
                        start,
                        target
                );

        fScore.put(
                startPolygonId,
                startHeuristic
        );

        openSet.add(
                new Node(
                        startPolygonId,
                        startHeuristic
                )
        );

        // ----------------------------------------------------
        // A*
        // ----------------------------------------------------

        while (!openSet.isEmpty()) {

            Node currentNode =
                    openSet.poll();

            int currentId =
                    currentNode.polygonId;

            // ------------------------------------------------
            // TARGET
            // ------------------------------------------------

            if (currentId == targetPolygonId) {

                return reconstructPath(
                        cameFrom,
                        currentId
                );
            }

            // ------------------------------------------------
            // CLOSED
            // ------------------------------------------------

            if (!closedSet.add(currentId))
                continue;

            NavMeshPolygon current =
                    navMesh.getPolygon(currentId);

            if (current == null)
                continue;

            float currentG =
                    gScore.getOrDefault(
                            currentId,
                            Float.POSITIVE_INFINITY
                    );

            // ------------------------------------------------
            // NEIGHBORS
            // ------------------------------------------------

            for (Integer neighborId :
                    current.neighbors) {

                if (neighborId == null)
                    continue;

                // --------------------------------------------
                // IGNORAR JÁ PROCESSADO
                // --------------------------------------------

                if (closedSet.contains(neighborId))
                    continue;

                NavMeshPolygon neighbor =
                        navMesh.getPolygon(
                                neighborId
                        );

                if (neighbor == null)
                    continue;

                // --------------------------------------------
                // COST
                // --------------------------------------------

                float movementCost =
                        distance(
                                current,
                                neighbor
                        );

                float tentativeG =
                        currentG
                                + movementCost;

                float oldG =
                        gScore.getOrDefault(
                                neighborId,
                                Float.POSITIVE_INFINITY
                        );

                // --------------------------------------------
                // MELHOR CAMINHO
                // --------------------------------------------

                if (tentativeG < oldG) {

                    cameFrom.put(
                            neighborId,
                            currentId
                    );

                    gScore.put(
                            neighborId,
                            tentativeG
                    );

                    float h =
                            distance(
                                    neighbor,
                                    target
                            );

                    float f =
                            tentativeG + h;

                    fScore.put(
                            neighborId,
                            f
                    );

                    openSet.add(
                            new Node(
                                    neighborId,
                                    f
                            )
                    );
                }
            }
        }

        // ----------------------------------------------------
        // PATH NÃO ENCONTRADO
        // ----------------------------------------------------

        System.out.println(
                "A*: Nenhum caminho encontrado entre "
                        + startPolygonId
                        + " e "
                        + targetPolygonId
        );

        return Collections.emptyList();
    }

    // ========================================================
    // DISTANCE
    // ========================================================

    private static float distance(
            NavMeshPolygon a,
            NavMeshPolygon b) {

        float dx =
                a.centerX -
                b.centerX;

        float dy =
                a.centerY -
                b.centerY;

        float dz =
                a.centerZ -
                b.centerZ;

        return (float)Math.sqrt(
                dx * dx +
                dy * dy +
                dz * dz
        );
    }

    // ========================================================
    // RECONSTRUCT PATH
    // ========================================================

    private static List<Integer> reconstructPath(
            Map<Integer, Integer> cameFrom,
            int current) {

        List<Integer> path =
                new ArrayList<>();

        path.add(current);

        while (cameFrom.containsKey(current)) {

            current =
                    cameFrom.get(current);

            path.add(current);
        }

        Collections.reverse(path);

        return path;
    }

    // ========================================================
    // NODE
    // ========================================================

    private static class Node
            implements Comparable<Node> {

        public final int polygonId;

        public final float fScore;

        public Node(
                int polygonId,
                float fScore) {

            this.polygonId =
                    polygonId;

            this.fScore =
                    fScore;
        }

        @Override
        public int compareTo(
                Node other) {

            return Float.compare(
                    this.fScore,
                    other.fScore
            );
        }
    }
    
    public static Set<Integer> getConnectedPolygons(
            ServerNavMesh navMesh,
            int startPolygonId) {

        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        if (navMesh.getPolygon(startPolygonId) == null)
            return visited;

        queue.add(startPolygonId);
        visited.add(startPolygonId);

        while (!queue.isEmpty()) {

            int currentId = queue.poll();

            NavMeshPolygon current =
                    navMesh.getPolygon(currentId);

            if (current == null)
                continue;

            for (Integer neighborId : current.neighbors) {

                if (neighborId == null)
                    continue;

                if (visited.contains(neighborId))
                    continue;

                if (navMesh.getPolygon(neighborId) == null)
                    continue;

                visited.add(neighborId);
                queue.add(neighborId);
            }
        }

        return visited;
    }
    
    public static List<Set<Integer>> findConnectedComponents(
            ServerNavMesh navMesh) {

        List<Set<Integer>> components =
                new ArrayList<>();

        Set<Integer> unvisited =
                new HashSet<>(
                        navMesh.getPolygons().keySet()
                );

        while (!unvisited.isEmpty()) {

            int start =
                    unvisited.iterator().next();

            Set<Integer> component =
                    getConnectedPolygons(
                            navMesh,
                            start
                    );

            components.add(component);

            unvisited.removeAll(component);
        }

        return components;
    }
}
