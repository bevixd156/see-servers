package com.devst.verservidores.repositorio;
//Importaciones necesarias para el funcionamiento de la clase
import android.util.Log;
import com.devst.verservidores.comment.Comentario;
import com.devst.verservidores.vista.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRepositorio {

    // Instancia principal de Firestore y constantes utilizadas por la clase
    private final FirebaseFirestore db;
    private static final String TAG = "FirebaseRepo";
    private static final String COLECCION_COMENTARIOS = "comentarios_fs";

    // Constructor: inicializa Firestore
    public FirebaseRepositorio() {
        db = FirebaseFirestore.getInstance();
    }

    // Agrega un usuario a Firestore con su ID como documento
    public void agregarUsuario(int userId, Usuario usuario) {
        db.collection("usuarios")
                .document(String.valueOf(userId))
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseRepo", "Usuario agregado con ID: " + userId))
                .addOnFailureListener(e -> Log.w("FirebaseRepo", "Error al agregar usuario", e));
    }

    // Actualiza un usuario existente usando merge() para no eliminar campos previos
    public void actualizarUsuario(int userId, Usuario usuario) {
        db.collection("usuarios")
                .document(String.valueOf(userId))
                .set(usuario, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario actualizado con ID: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Error al actualizar usuario", e));
    }

    // Elimina un usuario y todos sus comentarios asociados usando WriteBatch
    public void eliminarUsuarioYComentariosFirestore(String userId, final OnCompleteListener<Void> listener) {

        // Eliminación del documento principal del usuario
        WriteBatch batch = db.batch();
        batch.delete(db.collection("usuarios").document(String.valueOf(userId)));

        // Buscar y eliminar comentarios de Discord
        db.collection(COLECCION_COMENTARIOS)
                .document("discord").collection("lista")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDiscord -> {

                    for (DocumentSnapshot document : queryDiscord.getDocuments()) {
                        batch.delete(document.getReference());
                    }

                    // Buscar y eliminar comentarios de Epic
                    db.collection(COLECCION_COMENTARIOS)
                            .document("epic").collection("lista")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(queryEpic -> {

                                for (DocumentSnapshot document : queryEpic.getDocuments()) {
                                    batch.delete(document.getReference());
                                }

                                // Buscar y eliminar comentarios de PlayStation
                                db.collection(COLECCION_COMENTARIOS)
                                        .document("playstation").collection("lista")
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(queryPs -> {

                                            for (DocumentSnapshot document : queryPs.getDocuments()) {
                                                batch.delete(document.getReference());
                                            }

                                            // Buscar y eliminar comentarios de Xbox
                                            db.collection(COLECCION_COMENTARIOS)
                                                    .document("xbox").collection("lista")
                                                    .whereEqualTo("userId", userId)
                                                    .get()
                                                    .addOnSuccessListener(queryXbox -> {

                                                        for (DocumentSnapshot document : queryXbox.getDocuments()) {
                                                            batch.delete(document.getReference());
                                                        }

                                                        // Buscar y eliminar comentarios de Nintendo
                                                        db.collection(COLECCION_COMENTARIOS)
                                                                .document("nintendo").collection("lista")
                                                                .whereEqualTo("userId", userId)
                                                                .get()
                                                                .addOnSuccessListener(queryNintendo -> {

                                                                    for (DocumentSnapshot document : queryNintendo.getDocuments()) {
                                                                        batch.delete(document.getReference());
                                                                    }

                                                                    // Ejecutar todas las eliminaciones juntas (COMMIT FINAL)
                                                                    batch.commit()
                                                                            .addOnCompleteListener(listener)
                                                                            .addOnFailureListener(e -> Log.e(TAG, "Error en batch de eliminación (Commit)", e));
                                                                })
                                                                .addOnFailureListener(e ->
                                                                        Log.e(TAG, "Error buscando comentarios de Nintendo", e));
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Log.e(TAG, "Error buscando comentarios de Xbox", e));
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Error buscando comentarios de PlayStation", e));
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error buscando comentarios de Epic", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error buscando comentarios de Discord", e));
    }

    // Agrega un comentario dentro del documento del servicio (discord, epic, etc)
    public void agregarComentario(String tipoServicio, int idComentario, Comentario comentario) {
        db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .document(String.valueOf(idComentario))
                .set(comentario)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comentario agregado, ID: " + idComentario))
                .addOnFailureListener(e -> Log.w(TAG, "Error al agregar comentario", e));
    }

    // Actualiza los campos principales de un comentario específico
    public void actualizarComentario(String tipoServicio, int idComentario, Comentario comentarioActualizado) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("texto", comentarioActualizado.getTexto());
        updates.put("timestamp", System.currentTimeMillis());

        db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .document(String.valueOf(idComentario))
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comentario actualizado, ID: " + idComentario))
                .addOnFailureListener(e -> Log.w(TAG, "Error al actualizar comentario", e));
    }

    // Elimina un comentario específico por ID dentro del servicio correspondiente
    public void eliminarComentario(String tipoServicio, int idComentario) {
        db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .document(String.valueOf(idComentario))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Comentario eliminado, ID: " + idComentario))
                .addOnFailureListener(e -> Log.w(TAG, "Error al eliminar comentario", e));
    }

    // Devuelve una Query ordenada por timestamp para mostrar comentarios en tiempo real
    public Query getComentariosQuery(String tipoServicio) {
        return db.collection(COLECCION_COMENTARIOS)
                .document(tipoServicio)
                .collection("lista")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }
}
