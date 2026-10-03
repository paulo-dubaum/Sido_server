package navmesh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import world.ServerNavMesh;

public class NavMeshLoader {

	private static final String MAGIC = "MMONAV";
    private static final int VERSION = 4;

    public static ServerNavMesh load(Path path)
            throws IOException {

        byte[] data = Files.readAllBytes(path);

        ByteBuffer buffer =
                ByteBuffer.wrap(data)
                        .order(ByteOrder.LITTLE_ENDIAN);

        // -------------------------------------------------
        // MAGIC
        // -------------------------------------------------

        byte[] magicBytes = new byte[6];

        buffer.get(magicBytes);

        String magic =
                new String(
                        magicBytes,
                        StandardCharsets.US_ASCII
                );

        if (!MAGIC.equals(magic)) {

            throw new IOException(
                    "NavMesh inválido. Magic encontrado: "
                            + magic
            );
        }

        // -------------------------------------------------
        // VERSION
        // -------------------------------------------------

        int version = buffer.getInt();

        if (version != VERSION) {

            throw new IOException(
                    "Versão de NavMesh incompatível. "
                            + "Esperado: "
                            + VERSION
                            + " Encontrado: "
                            + version
            );
        }

        // -------------------------------------------------
        // MAP NAME
        // -------------------------------------------------

        String mapName =
                readUnityString(buffer);

        // -------------------------------------------------
        // VERTICES
        // -------------------------------------------------

        int vertexCount =
                buffer.getInt();

        List<NavMeshVertex> vertices =
                new ArrayList<>(vertexCount);

        for (int i = 0; i < vertexCount; i++) {

            float x = buffer.getFloat();
            float y = buffer.getFloat();
            float z = buffer.getFloat();

            vertices.add(
                    new NavMeshVertex(
                            x,
                            y,
                            z
                    )
            );
        }

        // -------------------------------------------------
        // POLYGONS
        // -------------------------------------------------

        int polygonCount =
                buffer.getInt();

        List<NavMeshPolygon> polygons =
                new ArrayList<>(polygonCount);

        for (int i = 0; i < polygonCount; i++) {

            int id =
                    buffer.getInt();

            int a =
                    buffer.getInt();

            int b =
                    buffer.getInt();

            int c =
                    buffer.getInt();

            float centerX =
                    buffer.getFloat();

            float centerY =
                    buffer.getFloat();

            float centerZ =
                    buffer.getFloat();

            int area =
                    buffer.getInt();

            NavMeshPolygon polygon =
                    new NavMeshPolygon(
                            id,
                            a,
                            b,
                            c,
                            centerX,
                            centerY,
                            centerZ,
                            area
                    );

            int neighborCount =
                    buffer.getInt();

            for (int n = 0;
                 n < neighborCount;
                 n++) {

                polygon.neighbors.add(
                        buffer.getInt()
                );
            }

            polygons.add(polygon);
        }

        System.out.println(
                "NavMesh carregado: "
                        + mapName
        );

        System.out.println(
                "Vertices: "
                        + vertexCount
        );

        System.out.println(
                "Polygons: "
                        + polygonCount
        );

        return new ServerNavMesh(
                vertices,
                polygons
        );
    }

    // =====================================================
    // Unity BinaryWriter.Write(string)
    // =====================================================

    private static String readUnityString(
            ByteBuffer buffer) {

        int length =
                read7BitEncodedInt(buffer);

        byte[] bytes =
                new byte[length];

        buffer.get(bytes);

        return new String(
                bytes,
                StandardCharsets.UTF_8
        );
    }

    private static int read7BitEncodedInt(
            ByteBuffer buffer) {

        int result = 0;
        int shift = 0;

        while (true) {

            byte b =
                    buffer.get();

            result |=
                    (b & 0x7F) << shift;

            if ((b & 0x80) == 0)
                break;

            shift += 7;

            if (shift > 35) {

                throw new IllegalStateException(
                        "Invalid 7-bit encoded integer"
                );
            }
        }

        return result;
    }
    
}
