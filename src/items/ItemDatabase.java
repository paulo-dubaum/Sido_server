package items;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Carregamento atomico: um JSON invalido nunca substitui o catalogo atual. */
public final class ItemDatabase {
    private volatile Map<Integer, ItemData> items = Collections.emptyMap();

    public ItemDatabase(Path path) throws IOException { reload(path); }
    public synchronized void reload(Path path) throws IOException {
        if (path == null) throw new IllegalArgumentException("Caminho obrigatorio");
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        Object root = JsonReader.parse(json);
        Map<String,Object> document = obj(root, "raiz");
        keys(document, "raiz", "items");
        Object raw = required(document, "items");
        if (!(raw instanceof List)) throw new IllegalArgumentException("items deve ser array");
        Map<Integer, ItemData> loaded = new LinkedHashMap<>();
        int index = 0;
        for (Object element : (List<?>)raw) {
            String location = "items[" + index++ + "]";
            try {
                Map<String,Object> m = obj(element, location);
                keys(m, location, "id", "name", "type", "requiredLevel", "allowedClassIds",
                        "maxRefinement", "minAttack", "maxAttack", "defense", "attackSpeed", "attackReachBonusMeters");
                int id = integer(required(m,"id"),"id");
                ItemType type;
                try { type = ItemType.valueOf(str(required(m,"type"),"type")); }
                catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Tipo desconhecido: " + m.get("type")); }
                Set<Integer> classes = new HashSet<>();
                Object cls = m.get("allowedClassIds");
                if (cls != null) {
                    if (!(cls instanceof List)) throw new IllegalArgumentException("allowedClassIds deve ser array");
                    for (Object c : (List<?>)cls) if (!classes.add(integer(c,"allowedClassIds")))
                        throw new IllegalArgumentException("Classe duplicada");
                }
                ItemData item = new ItemData(id, str(required(m,"name"),"name"), type,
                        integer(m.getOrDefault("requiredLevel",java.math.BigDecimal.ZERO),"requiredLevel"), classes,
                        integer(m.getOrDefault("maxRefinement",java.math.BigDecimal.ZERO),"maxRefinement"),
                        decimal(m.getOrDefault("minAttack",0),"minAttack"),
                        decimal(m.getOrDefault("maxAttack",0),"maxAttack"),
                        decimal(m.getOrDefault("defense",0),"defense"),
                        decimal(m.getOrDefault("attackSpeed",0),"attackSpeed"),
                        decimal(m.getOrDefault("attackReachBonusMeters",0),"attackReachBonusMeters"));
                if (loaded.putIfAbsent(id,item) != null) throw new IllegalArgumentException("ID duplicado: " + id);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(location + ": " + ex.getMessage(), ex);
            }
        }
        items = Collections.unmodifiableMap(loaded);
    }
    public ItemData getItem(int id) { return items.get(id); }
    public ItemData requireItem(int id) {
        ItemData item = getItem(id);
        if (item == null) throw new IllegalArgumentException("Item inexistente: " + id);
        return item;
    }
    public int size() { return items.size(); }
    public Collection<ItemData> getAll() { return items.values(); }
    private static Object required(Map<String,Object> m, String key) {
        Object v = m.get(key); if (v == null) throw new IllegalArgumentException("Campo obrigatorio: " + key); return v;
    }
    private static void keys(Map<String,Object> m, String where, String... allowed) {
        Set<String> valid = new HashSet<>(Arrays.asList(allowed));
        for (String k : m.keySet()) if (!valid.contains(k)) throw new IllegalArgumentException(where + ": campo desconhecido " + k);
    }
    private static Map<String,Object> obj(Object v, String where) {
        if (!(v instanceof Map)) throw new IllegalArgumentException(where + " deve ser objeto");
        @SuppressWarnings("unchecked") Map<String,Object> out = (Map<String,Object>)v; return out;
    }
    private static String str(Object v, String key) {
        if (!(v instanceof String)) throw new IllegalArgumentException(key + " deve ser string"); return (String)v;
    }
    private static int integer(Object v, String key) {
        if (!(v instanceof java.math.BigDecimal)) throw new IllegalArgumentException(key + " deve ser inteiro");
        try { return ((java.math.BigDecimal)v).intValueExact(); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException(key + " fora do intervalo inteiro"); }
    }
    private static double decimal(Object v, String key) {
        if (!(v instanceof Number)) throw new IllegalArgumentException(key + " deve ser numero");
        double n = ((Number)v).doubleValue();
        if (!Double.isFinite(n)) throw new IllegalArgumentException(key + " nao finito");
        return n;
    }
}
