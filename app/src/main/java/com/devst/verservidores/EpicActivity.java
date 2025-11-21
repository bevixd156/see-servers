package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EpicActivity extends AppCompatActivity {

    private LinearLayout servicesContainer, commentsContainer;
    private ComentarioManager comentarioManager;

    private AdminSQLiteOpenHelper dbHelper;

    private int currentUserId;
    private static final String TIPO_SERVICIO = "epic";
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
        ScrollView scrollComments = findViewById(R.id.scrollComments);
        servicesContainer = findViewById(R.id.servicesContainer);
        commentsContainer = findViewById(R.id.commentsContainer);

        ScrollView scroll = findViewById(R.id.scrollEpic);

        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener usuario logueado
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);
        if (currentUserId == -1) {
            finish();
            return;
        }

        // Crear instancia de FirebaseRepositorio
        FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();

        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scrollComments,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        // EditText
        EditText edtNewComment = findViewById(R.id.edtNewComment);

        // Botón enviar comentario
        findViewById(R.id.btnSendComment).setOnClickListener(v -> {
            String texto = edtNewComment.getText().toString().trim();
            comentarioManager.enviarComentario(TIPO_SERVICIO, texto);
            edtNewComment.setText(""); // limpiar campo
        });

        // Cargar servicios y comentarios
        loadEpicStatus();
        comentarioManager.loadComments(TIPO_SERVICIO);
    }

    // ===========================
    // CARGAR ESTADO DEL SERVICIO
    // ===========================
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

    // Crear blocks visuales
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
