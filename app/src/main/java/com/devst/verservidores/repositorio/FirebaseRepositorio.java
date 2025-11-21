package com.devst.verservidores.repositorio;

import android.util.Log;
import com.devst.verservidores.Comentario;
import com.devst.verservidores.Usuario;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseRepositorio {

    private static final String COLLECTION_NAME = "usuarios";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ** IMPLEMENTACIÓN FIREBASE REALTIME DATABASE (Para comentarios) **
    private DatabaseReference comentariosRef = FirebaseDatabase.getInstance().getReference("comentarios");

    // ---------- USUARIOS (Firestore) ----------
    public void agregarUsuario(int id, Usuario usuario) {
        db.collection(COLLECTION_NAME)
                .document(String.valueOf(id))
                .set(usuario)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseRepo", "Usuario agregado a Firestore: " + id))
                .addOnFailureListener(e -> Log.e("FirebaseRepo", "Error al agregar usuario a Firestore", e));
    }

    public void actualizarUsuario(int id, Usuario usuario) {
        db.collection(COLLECTION_NAME)
                .document(String.valueOf(id))
                .set(usuario);
    }

    public void eliminarUsuario(int id) {
        db.collection(COLLECTION_NAME)
                .document(String.valueOf(id))
                .delete();
    }

    // ---------- COMENTARIOS (Realtime Database) ----------
    public void agregarComentario(String tipoServicio, int commentId, Comentario comentario) {
        // Path: comentarios/tipoServicio/commentId (ej: comentarios/discord/42)
        comentariosRef.child(tipoServicio).child(String.valueOf(commentId)).setValue(comentario);
    }

    public void actualizarComentario(String tipoServicio, int commentId, Comentario comentario) {
        comentariosRef.child(tipoServicio).child(String.valueOf(commentId)).setValue(comentario);
    }

    public void eliminarComentario(String tipoServicio, int commentId) {
        comentariosRef.child(tipoServicio).child(String.valueOf(commentId)).removeValue();
    }
}