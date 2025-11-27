package com.devst.verservidores;

// Clase modelo para mostrar datos del comentario en la interfaz
public class ComentarioViewData {
    // Objetos de la clase
    public int id;
    public int userId;
    public String username;
    public String mensaje;
    public String fecha;
    public String fotoPerfil;

    // Constructor para inicializar los valores
    public ComentarioViewData(int id, int userId, String username, String mensaje, String fecha, String fotoPerfil) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.fotoPerfil = fotoPerfil;
    }
}
