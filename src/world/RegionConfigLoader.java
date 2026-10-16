package world;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.google.gson.Gson;

public final class RegionConfigLoader {
    private RegionConfigLoader() { }
    private static final class Config { RegionDefinition[] regions; }

    public static RegionDefinition[] load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Config config = new Gson().fromJson(reader, Config.class);
            if (config == null || config.regions == null || config.regions.length == 0)
                throw new IOException("Nenhuma regiao configurada: " + path);
            Set<String> ids = new HashSet<>();
            for (RegionDefinition region : config.regions) {
                if (region == null || blank(region.id) || blank(region.navMesh) || blank(region.spawns))
                    throw new IOException("Regiao incompleta em " + path);
                if (!ids.add(region.id)) throw new IOException("Regiao duplicada: " + region.id);
                if (region.maxPlayersPerInstance <= 0)
                    throw new IOException("Capacidade invalida: " + region.id);
            }
            return config.regions;
        } catch (com.google.gson.JsonParseException e) {
            throw new IOException("JSON de regioes invalido: " + path, e);
        }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
