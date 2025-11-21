package com.devst.verservidores.repositorio;

import android.util.Log;
import com.devst.verservidores.Comentario;
import com.devst.verservidores.Usuario;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Necesario para obtener la referencia de la colección
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRepositorio {

    private final FirebaseFirestore db;
    // Eliminamos la referencia a Realtime Database: private final DatabaseReference rtdb;
    private static final String TAG = "FirebaseRepo";
    private static final String COLECCION_COMENTARIOS = "comentarios_fs"; // Nuevo nombre para evitar conflictos

    public FirebaseRepositorio() {
        // Mantenemos la inicialización de Firestore
        db = FirebaseFirestore.getInstance();
        // Eliminamos la inicialización de RTDB
    }

    // MÉTODOS DE USUARIO
    //(El código de agregarUsuario, actualizarUsuario, eliminarUsuario se mantiene)
    public void agregarUsuario(int userId, Usuario usuario) {
        db.collection("usuarios")
                .document(String.valueOf(userId))
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseRepo", "Usuario agregado con ID: " + userId))
                .addOnFailureListener(e -> Log.w("FirebaseRepo", "Error al agregar usuario", e));
    }

    public void actualizarUsuario(int userId, Usuario usuario) {
        // Usamos set con SetOptions.merge() para actualizar solo los campos existentes
        // en el objeto 'usuario', sin eliminar los demás campos del documento.
        db.collection("usuarios")
                .document(String.valueOf(userId))
                .set(usuario, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario actualizado con ID: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Error al actualizar usuario", e));
    }

    public void eliminarUsuario(int userId) {
        db.collection("usuarios")
                .document(String.valueOf(userId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario eliminado con ID: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Error al eliminar usuario", e));
    }

    // MÉTODOS DE COMENTARIOS
    public void agregarComentario(String tipoServicio, int idComentario, Comentario comentario) {
        // En Firestore, usamos el ID de SQLite como ID del documento dentro de la subcolección.
        db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio) // Documento padre para agrupar por servicio (Discord o Epic)
                .collection("lista")    // Subcolección de comentarios
                .document(String.valueOf(idComentario))
                .set(comentario)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comentario agregado a Firestore, ID: " + idComentario))
                .addOnFailureListener(e -> Log.w(TAG, "Error al agregar comentario a Firestore", e));
    }

    public void actualizarComentario(String tipoServicio, int idComentario, Comentario comentarioActualizado) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("texto", comentarioActualizado.getTexto());
        updates.put("timestamp", System.currentTimeMillis());

        db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .document(String.valueOf(idComentario))
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comentario actualizado en Firestore, ID: " + idComentario))
                .addOnFailureListener(e -> Log.w(TAG, "Error al actualizar comentario en Firestore", e));
    }

    public void eliminarComentario(String tipoServicio, int idComentario) {
        db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .document(String.valueOf(idComentario))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comentario eliminado de Firestore, ID: " + idComentario))
                .addOnFailureListener(e -> Log.w(TAG, "Error al eliminar comentario de Firestore", e));
    }

    // NUEVO METODO PARA OBTENER LA REFERENCIA DE CONSULTA DE FIRESTORE
    public Query getComentariosQuery(String tipoServicio) {
        // Devolvemos la referencia a la subcolección 'lista'
        return db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .orderBy("timestamp", Query.Direction.ASCENDING); // Asumiendo que Comentario tiene un campo 'timestamp'
    }
}