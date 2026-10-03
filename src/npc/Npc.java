package npc;

import com.smartfoxserver.mmo.MMOItem;

public class Npc {

    private final int id;
    private final MMOItem mmoItem;

    private float x;
    private float y;
    private float z;

    private int currentPolygonId = -1;

    private float detectionRange;

    private NpcState state = NpcState.IDLE;

    public Npc(
            int id,
            MMOItem mmoItem,
            float x,
            float y,
            float z,
            float detectionRange) {

        this.id = id;
        this.mmoItem = mmoItem;

        this.x = x;
        this.y = y;
        this.z = z;

        this.detectionRange =
                detectionRange;
    }

    public int getId() {
        return id;
    }

    public MMOItem getMmoItem() {
        return mmoItem;
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

    public void setPosition(
            float x,
            float y,
            float z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getCurrentPolygonId() {
        return currentPolygonId;
    }

    public void setCurrentPolygonId(
            int currentPolygonId) {

        this.currentPolygonId =
                currentPolygonId;
    }

    public float getDetectionRange() {
        return detectionRange;
    }

    public NpcState getState() {
        return state;
    }

    public void setState(NpcState state) {
        this.state = state;
    }
}