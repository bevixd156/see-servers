package com.devst.verservidores.repositorio;

import android.util.Log;
import com.devst.verservidores.Comentario;
import com.devst.verservidores.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Necesario para obtener la referencia de la colección
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

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

    public void eliminarUsuarioYComentariosFirestore(String userId, final OnCompleteListener<Void> listener) {
        // 1. Iniciar la eliminación del documento principal del usuario
        WriteBatch batch = db.batch();
        batch.delete(db.collection("usuarios").document(String.valueOf(userId))); // Elimina el documento de la colección "usuarios"

        // 2. Consultar y eliminar comentarios de Discord
        db.collection(COLECCION_COMENTARIOS)
                .document("discord").collection("lista")
                .whereEqualTo("userId", userId) // Campo necesario en tus documentos de comentario
                .get()
                .addOnSuccessListener(queryDiscord -> {

                    // Agregar eliminación de Discord al Batch
                    for (DocumentSnapshot document : queryDiscord.getDocuments()) {
                        batch.delete(document.getReference());
                    }

                    // 3. Consultar y agregar eliminación de comentarios de Epic
                    db.collection(COLECCION_COMENTARIOS)
                            .document("epic").collection("lista")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(queryEpic -> {

                                // Agregar eliminación de Epic al Batch
                                for (DocumentSnapshot document : queryEpic.getDocuments()) {
                                    batch.delete(document.getReference());
                                }

                                // 4. Ejecutar todas las eliminaciones en una sola transacción
                                batch.commit()
                                        .addOnCompleteListener(listener) // Notifica el resultado final
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Fallo al ejecutar batch de eliminación en Firestore", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Fallo al buscar comentarios de Epic", e);
                                // Notificar error si el listener lo soporta
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallo al buscar comentarios de Discord", e);
                    // Notificar error si el listener lo soporta
                });
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