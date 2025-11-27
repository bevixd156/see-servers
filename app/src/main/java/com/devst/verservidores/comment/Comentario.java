package com.devst.verservidores.comment;

// Clase modelo para sincronizar con Firebase
public class Comentario {

    // Objetos de la clase
    private String userId;
    private String texto;
    private String tipo;
    private long timestamp;

    // Constructor vacío requerido por Firebase
    public Comentario() {}

    // Constructor principal
    public Comentario(String userId, String texto, String tipo, long timestamp) {
        this.userId = userId;
        this.texto = texto;
        this.tipo = tipo;
        this.timestamp = timestamp;
    }

    // Getters y Setters requeridos por Firebase
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
