package com.devst.verservidores;

public class ComentarioViewData {
    public int id;
    public int userId;
    public String username;
    public String mensaje;
    public String fecha; // Formato de fecha legible (ej: "2025-10-26 15:30:00")
    public String fotoPerfil; // URI/URL de la foto de perfil

    public ComentarioViewData(int id, int userId, String username, String mensaje, String fecha, String fotoPerfil) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.fotoPerfil = fotoPerfil;
    }
}