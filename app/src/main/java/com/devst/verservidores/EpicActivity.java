package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
// Importaciones de Realtime Database eliminadas o ignoradas
// Importaciones de Firestore añadidas:
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EpicActivity extends AppCompatActivity {
    private LinearLayout servicesContainer, commentsContainer;
    private ComentarioManager comentarioManager;
    private AdminSQLiteOpenHelper dbHelper;
    private FirebaseRepositorio firebaseRepo;
    private int currentUserId;
    private static final String TIPO_SERVICIO = "epic";
    private static final String URL = "https://status.epicgames.com/api/v2/summary.json";
    private static final String TAG = "EpicActivity";

    // Variables de Firestore AÑADIDAS:
    private ListenerRegistration firestoreRegistration;
    private Query firestoreQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epic);

        firebaseRepo = new FirebaseRepositorio();
        dbHelper = new AdminSQLiteOpenHelper(this); // Inicialización movida aquí para claridad

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

        // Obtener usuario logueado
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);
        if (currentUserId == -1) {
            finish();
            return;
        }

        // Creación del ComentarioManager
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
            if (!texto.isEmpty()) {
                comentarioManager.enviarComentario(TIPO_SERVICIO, texto);
                edtNewComment.setText("");
            }
        });

        // Cargar servicios y comentarios
        loadEpicStatus();

        comentarioManager.loadComments(TIPO_SERVICIO);

        // INICIAR ESCUCHA DE FIRESTORE
        startFirebaseListener(TIPO_SERVICIO);
    }

    // ===========================
    // METODO PARA INICIAR LA ESCUCHA DE FIRESTORE
    // ===========================
    private void startFirebaseListener(String tipoServicio) {
        // 1. Obtener la referencia de la consulta de Firestore
        firestoreQuery = firebaseRepo.getComentariosQuery(tipoServicio);

        // 2. Adjuntar el Snapshot Listener (Escucha en tiempo real)
        firestoreRegistration = firestoreQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.w(TAG, "Error de escucha en Firestore:", error);
                return;
            }

            // Se ejecuta cada vez que hay un cambio.
            Log.d(TAG, "Cambios detectados en Firestore, recargando UI.");

            // La misma llamada para recargar los comentarios de SQLite
            comentarioManager.loadComments(tipoServicio);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // LIMPIEZA CRUCIAL: Detener el listener de Firestore
        if (firestoreRegistration != null) {
            firestoreRegistration.remove();
            Log.d(TAG, "Listener de Firestore desconectado.");
        }
    }

    // CARGAR ESTADO DEL SERVICIO (Mantenido)
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

    // Crear blocks visuales (Mantenido)
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