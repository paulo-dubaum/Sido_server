package navmesh;

import java.util.ArrayList;
import java.util.List;

public final class Funnel {

    private static final float EPSILON = 0.0001f;

    private Funnel() {
    }

    public static List<NavMeshPoint> findPath(
            NavMeshPoint start,
            NavMeshPoint end,
            List<NavMeshPortal> portals) {

        List<NavMeshPoint> result =
                new ArrayList<>();

        result.add(start);

        // Mesmo polígono:
        // não existe nenhum portal.
        if (portals == null || portals.isEmpty()) {

            if (!samePoint(start, end)) {
                result.add(end);
            }

            return result;
        }

        NavMeshPoint portalApex = start;

        NavMeshPoint portalLeft =
                new NavMeshPoint(
                        portals.get(0).left
                );

        NavMeshPoint portalRight =
                new NavMeshPoint(
                        portals.get(0).right
                );

        int apexIndex = 0;
        int leftIndex = 0;
        int rightIndex = 0;

        /*
         * Começamos no segundo portal porque
         * left/right já foram inicializados
         * usando o portal 0.
         *
         * O último passo artificial é o próprio
         * destino: left = right = end.
         */
        for (int i = 1;
             i <= portals.size();
             i++) {

            NavMeshPoint left;
            NavMeshPoint right;

            if (i == portals.size()) {

                left = end;
                right = end;

            } else {

                NavMeshPortal portal =
                        portals.get(i);

                left =
                        new NavMeshPoint(
                                portal.left
                        );

                right =
                        new NavMeshPoint(
                                portal.right
                        );
            }

            // =================================================
            // RIGHT
            // =================================================

            if (triarea2(
                    portalApex,
                    portalRight,
                    right) <= 0.0f) {

                if (samePoint(
                        portalApex,
                        portalRight)
                        || triarea2(
                        portalApex,
                        portalLeft,
                        right) > 0.0f) {

                    portalRight = right;
                    rightIndex = i;

                } else {

                    // Right cruzou Left.
                    // Left vira waypoint.

                    result.add(portalLeft);

                    portalApex = portalLeft;
                    apexIndex = leftIndex;

                    portalLeft = portalApex;
                    portalRight = portalApex;

                    leftIndex = apexIndex;
                    rightIndex = apexIndex;

                    i = apexIndex;

                    continue;
                }
            }

            // =================================================
            // LEFT
            // =================================================

            if (triarea2(
                    portalApex,
                    portalLeft,
                    left) >= 0.0f) {

                if (samePoint(
                        portalApex,
                        portalLeft)
                        || triarea2(
                        portalApex,
                        portalRight,
                        left) < 0.0f) {

                    portalLeft = left;
                    leftIndex = i;

                } else {

                    // Left cruzou Right.
                    // Right vira waypoint.

                    result.add(portalRight);

                    portalApex = portalRight;
                    apexIndex = rightIndex;

                    portalLeft = portalApex;
                    portalRight = portalApex;

                    leftIndex = apexIndex;
                    rightIndex = apexIndex;

                    i = apexIndex;
                }
            }
        }

        if (!samePoint(
                result.get(result.size() - 1),
                end)) {

            result.add(end);
        }

        return result;
    }

    // =========================================================
    // XZ TRIANGLE AREA
    // =========================================================

    private static float triarea2(
            NavMeshPoint a,
            NavMeshPoint b,
            NavMeshPoint c) {

        float abX = b.x - a.x;
        float abZ = b.z - a.z;

        float acX = c.x - a.x;
        float acZ = c.z - a.z;

        return acX * abZ
                - abX * acZ;
    }

    // =========================================================
    // POINT COMPARISON
    // =========================================================

    private static boolean samePoint(
            NavMeshPoint a,
            NavMeshPoint b) {

        float dx = a.x - b.x;
        float dz = a.z - b.z;

        return dx * dx + dz * dz
                <= EPSILON * EPSILON;
    }
}