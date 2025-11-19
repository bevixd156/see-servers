package com.devst.verservidores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.util.Date;

public class DiscordActivity extends AppCompatActivity {

    private LinearLayout commentsContainer;
    private EditText edtNewComment;
    private Button btnSendComment;
    private AdminSQLiteOpenHelper dbHelper;

    private int currentUserId = 1; // Id del usuario actual (simulado)
    private static final String TIPO_SERVICIO = "discord";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discord);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estado de Discord");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        commentsContainer = findViewById(R.id.commentsContainer);
        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);

        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener usuario logueado desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1); // -1 si no hay usuario logueado
        if(currentUserId == -1){
            // No hay usuario logueado, podrías cerrar la actividad o redirigir al login
            finish();
            return;
        }

        loadDiscordServices();

        loadComments(); // cargar comentarios existentes

        btnSendComment.setOnClickListener(v -> {
            String message = edtNewComment.getText().toString().trim();
            if (!message.isEmpty()) {
                String timestamp = DateFormat.getDateTimeInstance().format(new Date());

                // Guardar en DB
                dbHelper.insertComment(currentUserId, message, TIPO_SERVICIO, timestamp);

                // Limpiar EditText
                edtNewComment.setText("");
                // Recargar lista de comentarios
                loadComments();

                // Scroll al final
                ScrollView scrollView = findViewById(R.id.scrollComments);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void loadDiscordServices() {
        LinearLayout servicesContainer = findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews();

        // URL del estado oficial de Discord (JSON)
        final String URL = "https://discordstatus.com/api/v2/components.json";

        // Hilo para descargar el JSON
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL); // tu método para obtener JSON
            runOnUiThread(() -> populateDiscordServices(json));
        }).start();
    }

    private void populateDiscordServices(String json) {
        if (json == null) return;

        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);

        LinearLayout servicesContainer = findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews();

        if (obj.has("components")) {
            for (JsonElement el : obj.getAsJsonArray("components")) {
                JsonObject comp = el.getAsJsonObject();
                String name = comp.has("name") ? comp.get("name").getAsString() : "Desconocido";
                String status = comp.has("status") ? comp.get("status").getAsString() : "unknown";

                servicesContainer.addView(createServiceBlock(name, status));
            }
        }
    }

    //Caja para contener los distintos servicios en operacion
    private LinearLayout createServiceBlock(String name, String status) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        block.setPadding(0, 8, 0, 8);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        tv.setTextSize(18);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setText(name + " : " + status);

        View circle = new View(this);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(40, 40);
        circleParams.setMarginStart(8);
        circle.setLayoutParams(circleParams);

        // Colores según estado
        int drawable;
        switch (status.toLowerCase()) {
            case "operational":
                drawable = R.drawable.circle_green;
                break;
            case "degraded_performance":
                drawable = R.drawable.circle_yellow;
                break;
            case "partial_outage":
            case "major_outage":
                drawable = R.drawable.circle_red;
                break;
            default:
                drawable = R.drawable.circle_gray;
                break;
        }
        circle.setBackground(ContextCompat.getDrawable(this, drawable));

        block.addView(tv);
        block.addView(circle);

        return block;
    }


    private void loadComments() {
        commentsContainer.removeAllViews();

        Cursor cursor = dbHelper.getComments(TIPO_SERVICIO);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int userIdDelComentario = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String username = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("comentario"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                String profileUrl = cursor.getString(cursor.getColumnIndexOrThrow("foto_perfil"));

                addComment(username, message, timestamp, profileUrl, userIdDelComentario);

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void addComment(String username, String message, String timestamp, String profileUrl, int useridDelComentario) {
        LinearLayout commentBlock = new LinearLayout(this);
        commentBlock.setOrientation(LinearLayout.HORIZONTAL);
        commentBlock.setPadding(8, 8, 8, 8);
        commentBlock.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_card));

        ImageView profile = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(80, 80);
        imgParams.setMarginEnd(8);
        profile.setLayoutParams(imgParams);

        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .circleCrop()
                    .into(profile);
        } else {
            profile.setImageResource(R.drawable.user);
        }

        // Click en la imagen para abrir PerfilPublicoActivity
        profile.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilPublicoActivity.class);
            intent.putExtra("user_id", useridDelComentario); // enviamos el ID simulado
            startActivity(intent);
        });

        // Contenedor de texto
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView usernameTv = new TextView(this);
        usernameTv.setText(username);
        usernameTv.setTextSize(16);
        usernameTv.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView messageTv = new TextView(this);
        messageTv.setText(message);
        messageTv.setTextSize(16);

        TextView timestampTv = new TextView(this);
        timestampTv.setText(timestamp);
        timestampTv.setTextSize(12);
        timestampTv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        textContainer.addView(usernameTv);
        textContainer.addView(messageTv);
        textContainer.addView(timestampTv);

        commentBlock.addView(profile);
        commentBlock.addView(textContainer);

        commentsContainer.addView(commentBlock);

        // Mantener presionado para mostrar menú (solo si el comentario pertenece al usuario actual)
        if (useridDelComentario == currentUserId) {
            commentBlock.setOnLongClickListener(v -> {

                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenu().add("Modificar");
                popup.getMenu().add("Eliminar");

                popup.setOnMenuItemClickListener(item -> {

                    if (item.getTitle().equals("Modificar")) {
                        showEditDialog(useridDelComentario, message);
                    }

                    if (item.getTitle().equals("Eliminar")) {
                        deleteCommentAndReload(useridDelComentario, message);
                    }

                    return true;
                });

                popup.show();

                return true;
            });
        }
    }

    private void showEditDialog(int userId, String oldText) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Modificar comentario");

        final EditText input = new EditText(this);
        input.setText(oldText);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newText = input.getText().toString();

            dbHelper.getWritableDatabase().execSQL(
                    "UPDATE comentarios SET comentario = ? WHERE user_id = ? AND comentario = ?",
                    new Object[]{newText, userId, oldText}
            );

            loadComments();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void deleteCommentAndReload(int userId, String message) {
        dbHelper.getWritableDatabase().execSQL(
                "DELETE FROM comentarios WHERE user_id = ? AND comentario = ?",
                new Object[]{userId, message}
        );
        loadComments();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
