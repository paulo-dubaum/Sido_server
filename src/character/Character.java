package character;

import com.smartfoxserver.entities.User;

public abstract class Character {

    // =========================================================
    // IDENTITY
    // =========================================================

    private final long characterId;
    private final User user;

    private String name;

    // =========================================================
    // WORLD POSITION
    // =========================================================

    private float x;
    private float y;
    private float z;

    // =========================================================
    // NAVIGATION
    // =========================================================

    private int currentPolygonId = -1;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    protected Character(
            long characterId,
            User user,
            String name,
            float x,
            float y,
            float z) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Character precisa estar associado a um User."
            );
        }

        this.characterId = characterId;
        this.user = user;
        this.name = name;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    // =========================================================
    // IDENTITY
    // =========================================================

    public long getCharacterId() {
        return characterId;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // =========================================================
    // POSITION
    // =========================================================

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

    // =========================================================
    // NAVIGATION
    // =========================================================

    public int getCurrentPolygonId() {
        return currentPolygonId;
    }

    public void setCurrentPolygonId(
            int currentPolygonId) {

        this.currentPolygonId =
                currentPolygonId;
    }

    // =========================================================
    // DEBUG
    // =========================================================

    @Override
    public String toString() {

        return getClass().getSimpleName()
                + "{"
                + "characterId=" + characterId
                + ", name='" + name + '\''
                + ", userId=" + user.getId()
                + ", position=("
                + x + ", "
                + y + ", "
                + z + ")"
                + ", polygon="
                + currentPolygonId
                + '}';
    }
}
