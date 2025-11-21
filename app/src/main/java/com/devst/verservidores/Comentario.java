package com.devst.verservidores;
public class Comentario {
    public int id; // ID del comentario en SQLite
    public int userId;
    public String username;
    public String mensaje;
    public String timestamp;
    public String fotoPerfil;

    // ** CORRECCIÓN: Constructor vacío requerido por Firebase **
    public Comentario() {}

    public Comentario(int id, int userId, String username, String mensaje, String timestamp, String fotoPerfil) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
        this.fotoPerfil = fotoPerfil;
    }

    // Constructor simplificado para nuevo comentario antes de insertar en SQLite
    public Comentario(int userId, String username, String mensaje, String timestamp, String fotoPerfil) {
        this.userId = userId;
        this.username = username;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
        this.fotoPerfil = fotoPerfil;
    }
}