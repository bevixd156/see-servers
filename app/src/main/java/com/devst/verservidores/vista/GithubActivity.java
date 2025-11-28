package com.devst.verservidores.vista;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.devst.verservidores.R;
import com.devst.verservidores.api.ApiFetcher;
import com.devst.verservidores.comment.ComentarioManager;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class GithubActivity extends AppCompatActivity {

    private LinearLayout commentsContainer;
    private LinearLayout servicesContainer;
    private EditText edtNewComment;
    private Button btnSendComment;

    private ComentarioManager comentarioManager;
    private FirebaseRepositorio firebaseRepo;
    private AdminSQLiteOpenHelper dbHelper;
    private ListenerRegistration firestoreRegistration;

    private int currentUserId;

    private static final String TIPO_SERVICIO = "github";
    private static final String URL = "https://www.githubstatus.com/api/v2/components.json";
    private static final String TAG = "GithubActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_github);

        firebaseRepo = new FirebaseRepositorio();
        dbHelper = new AdminSQLiteOpenHelper(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Estado de GitHub");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Referencias XML
        servicesContainer = findViewById(R.id.servicesContainer);
        commentsContainer = findViewById(R.id.commentsContainer);

        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        ScrollView scrollComments = findViewById(R.id.scrollComments);

        // Usuario
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);
        if (currentUserId == -1) {
            finish();
            return;
        }

        // Manager comentarios
        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scrollComments,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        // Cargar datos
        loadGithubServices();
        comentarioManager.loadComments(TIPO_SERVICIO);
        startFirebaseListener();

        // Botón enviar
        btnSendComment.setOnClickListener(v -> {
            String text = edtNewComment.getText().toString().trim();
            if (!text.isEmpty()) {
                comentarioManager.enviarComentario(TIPO_SERVICIO, text);
                edtNewComment.setText("");
            }
        });
    }

    private void startFirebaseListener() {
        Query query = firebaseRepo.getComentariosQuery(TIPO_SERVICIO);
        firestoreRegistration = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Error firebase:", e);
                return;
            }
            comentarioManager.loadComments(TIPO_SERVICIO);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreRegistration != null) firestoreRegistration.remove();
    }

    // Obtener JSON GitHub
    private void loadGithubServices() {
        servicesContainer.removeAllViews();
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            runOnUiThread(() -> populateGithub(json));
        }).start();
    }

    // Procesar JSON
    private void populateGithub(String json) {
        if (json == null) return;

        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);

        if (!obj.has("components")) return;

        for (JsonElement e : obj.getAsJsonArray("components")) {
            JsonObject comp = e.getAsJsonObject();

            String name = comp.has("name") ? comp.get("name").getAsString() : "Desconocido";
            String status = comp.has("status") ? comp.get("status").getAsString() : "unknown";

            servicesContainer.addView(createServiceBlock(name, status));
        }
    }

    // Crear bloque visual
    private LinearLayout createServiceBlock(String name, String status) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setPadding(0, 10, 0, 10);

        TextView tv = new TextView(this);
        tv.setText(name + " : " + status);
        tv.setTextSize(18);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        View circle = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(40, 40);
        p.setMarginStart(10);
        circle.setLayoutParams(p);

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
