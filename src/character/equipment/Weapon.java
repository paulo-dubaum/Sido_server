package character.equipment;

/** Instancia de arma equipada. O ID identifica o item persistido no inventario. */
public final class Weapon {
    private final long itemId;
    private final int definitionId;
    private final WeaponType type;

    public Weapon(long itemId, int definitionId, WeaponType type) {
        if (itemId <= 0) throw new IllegalArgumentException("itemId invalido");
        if (definitionId <= 0) throw new IllegalArgumentException("definitionId invalido");
        if (type == null) throw new IllegalArgumentException("type obrigatorio");
        this.itemId = itemId;
        this.definitionId = definitionId;
        this.type = type;
    }

    public long getItemId() { return itemId; }
    public int getDefinitionId() { return definitionId; }
    public WeaponType getType() { return type; }
}
