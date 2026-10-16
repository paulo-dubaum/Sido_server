package world;

/** Relogio global do mundo. Um minuto de jogo a cada intervalMs reais. */
public final class WorldTimeManager {
    private final long intervalMs;
    private final long startedAtMs;
    private final int initialMinute;

    public WorldTimeManager(int initialMinute, long intervalMs, long startedAtMs) {
        if (initialMinute < 0 || initialMinute >= 1440 || intervalMs <= 0)
            throw new IllegalArgumentException("Horario ou intervalo invalido");
        this.initialMinute = initialMinute;
        this.intervalMs = intervalMs;
        this.startedAtMs = startedAtMs;
    }

    public int getWorldTime(long nowMs) {
        long elapsed = Math.max(0L, nowMs - startedAtMs) / intervalMs;
        return (int) ((initialMinute + elapsed % 1440L) % 1440L);
    }
}
