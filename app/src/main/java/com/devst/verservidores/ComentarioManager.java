package com.devst.verservidores;

// Importaciones necesarias para el funcionamiento
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;
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

// Clase encargada de gestionar comentarios (cargar, mostrar, crear, editar, eliminar)
public class ComentarioManager {

    // Objetos necesarios para manejar UI, BD y Firebase
    private final Context context;
    private final LinearLayout commentsContainer;
    private final AdminSQLiteOpenHelper dbHelper;
    private final FirebaseRepositorio firebaseRepo;
    private final int currentUserId;
    private final ScrollView scrollView;

    // Constructor
    public ComentarioManager(
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

    // Cargar comentarios desde SQLite
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

                ComentarioViewData comentario = new ComentarioViewData(id, userId, username, mensaje, fecha, foto);
                addCommentView(comentario, tipoServicio);

            } while (cursor.moveToNext());

            cursor.close();
            scrollToBottom();
        }
    }

    // Crear visualmente un comentario dentro del contenedor
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

        try {
            if (c.fotoPerfil != null && !c.fotoPerfil.isEmpty()) {
                byte[] bytes = Base64.decode(c.fotoPerfil, Base64.DEFAULT);
                Glide.with(context).load(bytes).circleCrop().placeholder(R.drawable.user).error(R.drawable.user).into(img);
            } else {
                img.setImageResource(R.drawable.user);
            }
        } catch (Exception e) {
            Log.e("ComentarioManager", "Error Base64: " + e.getMessage());
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
                    if (item.getTitle().equals("Modificar")) showEditDialog(c, tipoServicio);
                    else deleteComment(c, tipoServicio);
                    return true;
                });

                popup.show();
                return true;
            });
        }
    }

    // Enviar un comentario (crear)
    public void enviarComentario(String tipoServicio, String texto) {
        if (texto.isEmpty()) return;

        long timestamp = System.currentTimeMillis();
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));

        long id = dbHelper.insertComment(currentUserId, texto, tipoServicio, fecha);

        if (id > 0) {
            Comentario comentarioRTDB = new Comentario(
                    String.valueOf(currentUserId),
                    texto,
                    tipoServicio,
                    timestamp
            );

            firebaseRepo.agregarComentario(tipoServicio, (int) id, comentarioRTDB);
            loadComments(tipoServicio);
        } else {
            Toast.makeText(context, "Error al guardar comentario localmente", Toast.LENGTH_SHORT).show();
        }
    }

    // Editar comentario existente
    private void showEditDialog(ComentarioViewData c, String tipoServicio) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Editar Comentario");

        EditText input = new EditText(context);
        input.setText(c.mensaje);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevo = input.getText().toString().trim();
            if (nuevo.isEmpty()) {
                Toast.makeText(context, "El comentario no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            long timestamp = System.currentTimeMillis();
            String nuevaFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));

            dbHelper.getWritableDatabase().execSQL(
                    "UPDATE comentarios SET comentario = ?, fecha = ? WHERE id = ?",
                    new Object[]{nuevo, nuevaFecha, c.id}
            );

            Comentario comentarioRTDB = new Comentario(
                    String.valueOf(c.userId),
                    nuevo,
                    tipoServicio,
                    timestamp
            );

            firebaseRepo.actualizarComentario(tipoServicio, c.id, comentarioRTDB);
            loadComments(tipoServicio);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // Eliminar comentario
    private void deleteComment(ComentarioViewData c, String tipoServicio) {
        int rows = dbHelper.getWritableDatabase().delete(
                "comentarios",
                "id = ?",
                new String[]{String.valueOf(c.id)}
        );

        if (rows > 0) {
            firebaseRepo.eliminarComentario(tipoServicio, c.id);
            Toast.makeText(context, "Comentario eliminado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Error al eliminar comentario", Toast.LENGTH_SHORT).show();
        }

        loadComments(tipoServicio);
    }

    // Hacer scroll hasta el final
    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
}
