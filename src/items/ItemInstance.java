package items;

/** Estado persistivel de uma unidade de item; sem efeitos excelentes ou de conjunto. */
public final class ItemInstance {
    private final long instanceId;
    private final int definitionId;
    private int refinementLevel;
    private boolean lucky;

    public ItemInstance(long instanceId, ItemData definition, int refinementLevel, boolean lucky) {
        if (instanceId <= 0 || definition == null) throw new IllegalArgumentException("Item invalido");
        this.instanceId = instanceId;
        this.definitionId = definition.getId();
        setRefinementLevel(refinementLevel, definition);
        this.lucky = lucky;
    }
    public long getInstanceId() { return instanceId; }
    public int getDefinitionId() { return definitionId; }
    public int getRefinementLevel() { return refinementLevel; }
    public boolean isLucky() { return lucky; }
    public void setLucky(boolean lucky) { this.lucky = lucky; }
    public void setRefinementLevel(int value, ItemData definition) {
        if (definition == null || definition.getId() != definitionId || value < 0 || value > definition.getMaxRefinement())
            throw new IllegalArgumentException("Refinamento fora do limite ou definicao divergente");
        refinementLevel = value;
    }
}
