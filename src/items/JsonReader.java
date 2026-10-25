package items;

import java.util.*;

/** Parser JSON pequeno, autocontido e estrito, para catalogos de configuracao. */
final class JsonReader {
    private final String s;
    private int p;
    private JsonReader(String s) { this.s = s; }
    static Object parse(String s) {
        JsonReader r = new JsonReader(s);
        Object value = r.value(); r.ws();
        if (r.p != s.length()) throw r.error("Conteudo apos JSON");
        return value;
    }
    private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " (posicao " + p + ")"); }
    private void ws() { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; }
    private boolean eat(char c) { ws(); if (p < s.length() && s.charAt(p) == c) { p++; return true; } return false; }
    private void expect(char c) { if (!eat(c)) throw error("Esperado '" + c + "'"); }
    private Object value() {
        ws(); if (p >= s.length()) throw error("JSON incompleto");
        char c = s.charAt(p);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (c == 't' && literal("true")) return Boolean.TRUE;
        if (c == 'f' && literal("false")) return Boolean.FALSE;
        if (c == 'n' && literal("null")) return null;
        if (c == '-' || (c >= '0' && c <= '9')) return number();
        throw error("Valor JSON invalido");
    }
    private boolean literal(String token) { if (!s.startsWith(token, p)) return false; p += token.length(); return true; }
    private Map<String,Object> object() {
        expect('{'); Map<String,Object> map = new LinkedHashMap<>();
        if (eat('}')) return map;
        do {
            ws(); if (p >= s.length() || s.charAt(p) != '"') throw error("Chave nao e string");
            String key = string(); expect(':'); Object v = value();
            if (map.containsKey(key)) throw error("Chave duplicada: " + key);
            map.put(key, v);
            if (eat('}')) return map;
            expect(',');
        } while (true);
    }
    private List<Object> array() {
        expect('['); List<Object> out = new ArrayList<>();
        if (eat(']')) return out;
        do { out.add(value()); if (eat(']')) return out; expect(','); } while (true);
    }
    private String string() {
        expect('"'); StringBuilder b = new StringBuilder();
        while (p < s.length()) {
            char c = s.charAt(p++);
            if (c == '"') return b.toString();
            if (c < 0x20) throw error("Caractere de controle em string");
            if (c != '\\') { b.append(c); continue; }
            if (p >= s.length()) throw error("Escape incompleto");
            char e = s.charAt(p++);
            switch (e) {
                case '"': case '\\': case '/': b.append(e); break;
                case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break;
                case 'n': b.append('\n'); break;
                case 'r': b.append('\r'); break;
                case 't': b.append('\t'); break;
                case 'u':
                    if (p + 4 > s.length()) throw error("Unicode incompleto");
                    try { b.append((char) Integer.parseInt(s.substring(p,p+4),16)); }
                    catch (NumberFormatException ex) { throw error("Unicode invalido"); }
                    p += 4; break;
                default: throw error("Escape invalido");
            }
        }
        throw error("String nao terminada");
    }
    private Number number() {
        int start = p;
        if (s.charAt(p) == '-') p++;
        if (p >= s.length()) throw error("Numero incompleto");
        if (s.charAt(p) == '0') p++;
        else {
            if (s.charAt(p) < '1' || s.charAt(p) > '9') throw error("Numero invalido");
            while (p < s.length() && Character.isDigit(s.charAt(p))) p++;
        }
        if (p < s.length() && s.charAt(p) == '.') {
            p++; int startDigits = p;
            while (p < s.length() && Character.isDigit(s.charAt(p))) p++;
            if (p == startDigits) throw error("Fracao invalida");
        }
        if (p < s.length() && (s.charAt(p) == 'e' || s.charAt(p) == 'E')) {
            p++; if (p < s.length() && (s.charAt(p) == '+' || s.charAt(p) == '-')) p++;
            int startDigits = p;
            while (p < s.length() && Character.isDigit(s.charAt(p))) p++;
            if (p == startDigits) throw error("Expoente invalido");
        }
        try { return new java.math.BigDecimal(s.substring(start,p)); }
        catch (NumberFormatException ex) { throw error("Numero invalido"); }
    }
}
