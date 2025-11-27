package com.devst.verservidores;

public class Usuario {

    // Datos básicos del usuario sincronizados con Firebase
    private String nombre;
    private String correo;
    private String fechaRegistro;
    private String fotoPerfil;

    // Constructor vacío requerido por Firebase
    public Usuario() {}

    // Constructor principal
    public Usuario(String nombre, String correo, String fechaRegistro, String fotoPerfil) {
        this.nombre = nombre;
        this.correo = correo;
        this.fechaRegistro = fechaRegistro;
        this.fotoPerfil = fotoPerfil == null ? "" : fotoPerfil; // foto vacía si viene null
    }

    // Getter y Setter de nombre
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // Getter y Setter de correo
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    // Getter y Setter de fecha de registro
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    // Getter y Setter de foto de perfil
    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
}
