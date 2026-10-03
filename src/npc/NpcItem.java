package npc;


import java.util.LinkedList;
import java.util.List;

import com.smartfoxserver.mmo.IMMOItemVariable;
import com.smartfoxserver.mmo.MMOItem;
import com.smartfoxserver.mmo.MMOItemVariable;
import com.smartfoxserver.mmo.Vec3D;

public class NpcItem extends MMOItem {

    private final int npcTypeId;

    private float x;
    private float y;
    private float z;

    private final float spawnX;
    private final float spawnY;
    private final float spawnZ;

    private final float detectionRange;

    private int currentPolygonId = -1;

    private NpcState state =
            NpcState.IDLE;

    public NpcItem(
            int npcTypeId,
            float x,
            float y,
            float z,
            float detectionRange) {

        super(createVariables(npcTypeId));

        this.npcTypeId =
                npcTypeId;

        this.x = x;
        this.y = y;
        this.z = z;

        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;

        this.detectionRange =
                detectionRange;
    }

    private static List<IMMOItemVariable> createVariables(
            int npcTypeId) {

        List<IMMOItemVariable> variables =
                new LinkedList<>();

        /*
         * Informações que o CLIENTE precisa conhecer.
         *
         * "t" = tipo da entidade
         * "n" = npcTypeId
         */

        variables.add(
                new MMOItemVariable(
                        "t",
                        1
                )
        );

        variables.add(
                new MMOItemVariable(
                        "n",
                        npcTypeId
                )
        );

        return variables;
    }

    public int getNpcTypeId() {
        return npcTypeId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Vec3D getPosition() {

        return new Vec3D(
                x,
                y,
                z
        );
    }

    public void setPosition(
            float x,
            float y,
            float z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public float getSpawnZ() {
        return spawnZ;
    }

    public float getDetectionRange() {
        return detectionRange;
    }

    public int getCurrentPolygonId() {
        return currentPolygonId;
    }

    public void setCurrentPolygonId(
            int currentPolygonId) {

        this.currentPolygonId =
                currentPolygonId;
    }

    public NpcState getState() {
        return state;
    }

    public void setState(
            NpcState state) {

        this.state = state;
    }
}

