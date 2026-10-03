package utils;

public class CharacterModel {
    private int id;
    private String nome;
    private String classe;
    private int exp;
    private String mapa;

    public CharacterModel(int id, String nome, String classe, int exp, String mapa) {
        this.id = id;
        this.nome = nome;
        this.classe = classe;
        this.exp = exp;
        this.mapa = mapa;
    }

    // Getters e Setters rápidos
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getClasse() { return classe; }
    public int getExp() { return exp; }
    public String getMapa() { return mapa; }
    
    public void setNome(String nome) { this.nome = nome; }
    public void setClasse(String classe) { this.classe = classe; }
    public void setExp(int exp) { this.exp = exp; }
    public void setMapa(String mapa) { this.mapa = mapa; }
}