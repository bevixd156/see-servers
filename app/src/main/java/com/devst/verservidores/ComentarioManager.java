package com.devst.verservidores;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ComentarioManager {
    private final Context context;
    private final LinearLayout commentsContainer;
    private final AdminSQLiteOpenHelper dbHelper;
    private final FirebaseRepositorio firebaseRepo;
    private final int currentUserId;
    private final ScrollView scrollView;

    public ComentarioManager (
            Context context,
            LinearLayout commentsContainer,
            ScrollView scrollView,
            AdminSQLiteOpenHelper dbHelper,
            FirebaseRepositorio firebaseRepo,
            int currentUserId
    ) {
        this.context = context;
        this.commentsContainer = commentsContainer;
        this.scrollView = scrollView;
        this.dbHelper = dbHelper;
        this.firebaseRepo = firebaseRepo;
        this.currentUserId = currentUserId;
    }

    // ===========================
    // Cargar comentarios desde SQLite
    // ===========================
    public void loadComments(String tipoServicio) {
        commentsContainer.removeAllViews();

        Cursor cursor = dbHelper.getComments(tipoServicio);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String username = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String mensaje = cursor.getString(cursor.getColumnIndexOrThrow("comentario"));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                String foto = cursor.getString(cursor.getColumnIndexOrThrow("foto_perfil"));

                // ⬅️ CORRECCIÓN: Se crea un objeto auxiliar para la vista que tiene más campos que la versión de Firebase.
                ComentarioViewData comentario = new ComentarioViewData(id, userId, username, mensaje, fecha, foto);
                addCommentView(comentario, tipoServicio);

            } while (cursor.moveToNext());

            cursor.close();
            // Asegurarse de ir al final después de la carga
            scrollToBottom();
        }
    }

    // ===========================
    // Crear vista de comentario
    // ===========================
    // ⬅️ CORRECCIÓN: El argumento se cambia al nuevo objeto auxiliar ComentarioViewData
    private void addCommentView(ComentarioViewData c, String tipoServicio) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setPadding(8, 8, 8, 8);
        block.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_card));
        block.setGravity(Gravity.CENTER_VERTICAL);

        ImageView img = new ImageView(context);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(80, 80);
        imgParams.setMarginEnd(8);
        img.setLayoutParams(imgParams);

        if (c.fotoPerfil != null && !c.fotoPerfil.isEmpty()) {
            Glide.with(context).load(c.fotoPerfil).circleCrop().into(img);
        } else {
            img.setImageResource(R.drawable.user);
        }

        img.setOnClickListener(v -> {
            Intent i = new Intent(context, PerfilPublicoActivity.class);
            i.putExtra("user_id", c.userId);
            if (!(context instanceof Activity)) i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        });

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView tvUser = new TextView(context);
        tvUser.setText(c.username);
        tvUser.setTextSize(16);
        tvUser.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvMsg = new TextView(context);
        tvMsg.setText(c.mensaje);
        tvMsg.setTextSize(16);

        TextView tvFecha = new TextView(context);
        // ⬅️ CORRECCIÓN: La fecha se llama 'fecha' en el objeto auxiliar
        tvFecha.setText(c.fecha);
        tvFecha.setTextSize(12);
        tvFecha.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));

        textContainer.addView(tvUser);
        textContainer.addView(tvMsg);
        textContainer.addView(tvFecha);

        block.addView(img);
        block.addView(textContainer);

        commentsContainer.addView(block);

        if (c.userId == currentUserId) {
            block.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, v);
                popup.getMenu().add("Modificar");
                popup.getMenu().add("Eliminar");

                popup.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    if (title.equals("Modificar")) {
                        showEditDialog(c, tipoServicio);
                    } else if (title.equals("Eliminar")) {
                        deleteComment(c, tipoServicio);
                    }
                    return true;
                });

                popup.show();
                return true;
            });
        }
    }

    // ===========================
    // Enviar comentario (CREATE)
    // ===========================
    public void enviarComentario(String tipoServicio, String texto) {
        if (texto.isEmpty()) return;

        // 1. Obtener datos y timestamp
        long timestampMs = System.currentTimeMillis();
        String fechaFormateada = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestampMs));

        // 2. Insertar en SQLite y obtener el ID
        long idComentario = dbHelper.insertComment(currentUserId, texto, tipoServicio, fechaFormateada);

        if (idComentario > 0) {
            String userIdString = String.valueOf(this.currentUserId);
            // 3. Crear el objeto Comentario compatible con Firebase (solo los 4 campos)
            // ⬅️ CORRECCIÓN: El constructor de Comentario solo lleva 4 campos (userId, texto, tipo, timestamp)
            Comentario comentarioRTDB = new Comentario(
                    userIdString,
                    texto,
                    tipoServicio,
                    timestampMs
            );

            // 4. Enviar a Firebase Realtime Database
            firebaseRepo.agregarComentario(tipoServicio, (int) idComentario, comentarioRTDB);

            // 5. Recargar y hacer scroll
            loadComments(tipoServicio);
        } else {
            Toast.makeText(context, "Error al guardar comentario localmente.", Toast.LENGTH_SHORT).show();
        }
    }

    // ===========================
    // Editar comentario
    // ===========================
    // ⬅️ CORRECCIÓN: El argumento del diálogo se cambia al nuevo objeto auxiliar ComentarioViewData
    private void showEditDialog(ComentarioViewData c, String tipoServicio) {
        // 1. Declarar e inicializar el Builder y el Input
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Editar Comentario");

        // Configurar el EditText para la entrada de texto
        final EditText input = new EditText(context);
        input.setText(c.mensaje); // Carga el mensaje actual
        input.setPadding(32, 32, 32, 32);

        // Configurar el Layout para el EditText
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50); // Ajuste de padding
        layout.addView(input);
        builder.setView(layout);

        // 2. Definir el comportamiento del botón "Guardar"
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevo = input.getText().toString().trim();
            if (nuevo.isEmpty()) {
                Toast.makeText(context, "El comentario no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generar nuevo timestamp y fecha
            long nuevoTimestampMs = System.currentTimeMillis();
            String nuevaFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(nuevoTimestampMs));

            // ** 2.1. UPDATE en SQLite **
            dbHelper.getWritableDatabase().execSQL(
                    "UPDATE comentarios SET comentario = ?, fecha = ? WHERE id = ?",
                    new Object[]{nuevo, nuevaFecha, c.id} // ⬅️ CORRECCIÓN: Actualizar la fecha/timestamp también
            );

            // ** 2.2. Actualizar Firebase **
            Comentario comentarioRTDB = new Comentario(
                    String.valueOf(c.userId),
                    nuevo,
                    tipoServicio,
                    nuevoTimestampMs // Usamos el nuevo long timestamp
            );

            firebaseRepo.actualizarComentario(tipoServicio, c.id, comentarioRTDB);

            Toast.makeText(context, "Comentario actualizado", Toast.LENGTH_SHORT).show();
            loadComments(tipoServicio);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // ===========================
    // Eliminar comentario
    // ===========================
    // ⬅️ CORRECCIÓN: El argumento se cambia al nuevo objeto auxiliar ComentarioViewData
    private void deleteComment(ComentarioViewData c, String tipoServicio) {
        // 1. ** DELETE en SQLite **
        int rowsDeleted = dbHelper.getWritableDatabase().delete(
                "comentarios",
                "id = ?",
                new String[]{String.valueOf(c.id)}
        );

        if (rowsDeleted > 0) {
            // 2. Eliminar en Firebase usando c.id
            firebaseRepo.eliminarComentario(tipoServicio, c.id);
            Toast.makeText(context, "Comentario eliminado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Error al eliminar comentario local", Toast.LENGTH_SHORT).show();
        }

        loadComments(tipoServicio);
    }

    // ===========================
    // Scroll al final
    // ===========================
    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
}