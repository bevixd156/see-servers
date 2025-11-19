package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.util.Date;

public class EpicActivity extends AppCompatActivity {

    private LinearLayout servicesContainer, commentsContainer;
    private EditText edtNewComment;
    private Button btnSendComment;

    private AdminSQLiteOpenHelper dbHelper;

    private int currentUserId = -1;
    private static final String TIPO_SERVICIO = "epic"; // Identificador en BD
    private static final String URL = "https://status.epicgames.com/api/v2/summary.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epic);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estado de Epic Games");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Views
        servicesContainer = findViewById(R.id.servicesContainer);
        commentsContainer = findViewById(R.id.commentsContainer);

        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);

        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener usuario logueado
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);
        if (currentUserId == -1) {
            finish();
            return;
        }

        loadEpicStatus();
        loadComments();

        btnSendComment.setOnClickListener(v -> {
            String msg = edtNewComment.getText().toString().trim();
            if (!msg.isEmpty()) {

                String timestamp = DateFormat.getDateTimeInstance().format(new Date());

                dbHelper.insertComment(currentUserId, msg, TIPO_SERVICIO, timestamp);

                edtNewComment.setText("");
                loadComments();

                ScrollView scrollView = findViewById(R.id.scrollEpic);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    // Cargar servicios
    private void loadEpicStatus() {
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            runOnUiThread(() -> populateServices(json));
        }).start();
    }

    private void populateServices(String json) {
        if (json == null) return;

        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);

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

    // Crear bloques de servicios (igual que Discord)
    private LinearLayout createServiceBlock(String name, String status) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setPadding(0, 8, 0, 8);

        TextView tv = new TextView(this);
        tv.setTextSize(20);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(name + " : " + status);

        View circle = new View(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(40, 40);
        cp.setMarginStart(8);
        circle.setLayoutParams(cp);

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

    // ===========================
    // CARGAR COMENTARIOS
    // ===========================
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

    private void addComment(String username, String message, String timestamp, String profileUrl, int userIdDelComentario) {

        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setPadding(8, 8, 8, 8);
        block.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_card));

        // Foto
        ImageView img = new ImageView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(80, 80);
        p.setMarginEnd(8);
        img.setLayoutParams(p);

        if (profileUrl != null && !profileUrl.isEmpty())
            Glide.with(this).load(profileUrl).circleCrop().into(img);
        else img.setImageResource(R.drawable.user);

        img.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilPublicoActivity.class);
            intent.putExtra("user_id", userIdDelComentario);
            startActivity(intent);
        });

        // Textos
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView tvUser = new TextView(this);
        tvUser.setText(username);
        tvUser.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);

        TextView tvTime = new TextView(this);
        tvTime.setText(timestamp);
        tvTime.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        tvTime.setTextSize(12);

        textContainer.addView(tvUser);
        textContainer.addView(tvMsg);
        textContainer.addView(tvTime);

        block.addView(img);
        block.addView(textContainer);

        commentsContainer.addView(block);

        // Menú para editar/eliminar si es tu comentario
        if (userIdDelComentario == currentUserId) {
            block.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenu().add("Modificar");
                popup.getMenu().add("Eliminar");

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Modificar")) {
                        showEditDialog(userIdDelComentario, message);
                    }
                    if (item.getTitle().equals("Eliminar")) {
                        deleteComment(userIdDelComentario, message);
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

    private void deleteComment(int userId, String message) {
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
