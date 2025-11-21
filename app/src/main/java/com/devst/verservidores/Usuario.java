package com.devst.verservidores;

public class Usuario {
    private String nombre;
    private String correo;
    private String fechaRegistro;
    private String fotoPerfil;

    public Usuario() {} // Constructor vacío requerido por Firebase

    public Usuario(String nombre, String correo, String fechaRegistro, String fotoPerfil) {
        this.nombre = nombre;
        this.correo = correo;
        this.fechaRegistro = fechaRegistro;
        this.fotoPerfil = fotoPerfil == null ? "" : fotoPerfil;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
}

