package com.devst.verservidores;

// Librerías necesarias
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

import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class DiscordActivity extends AppCompatActivity {

    // Objetos de la clase
    private LinearLayout commentsContainer;
    private EditText edtNewComment;
    private ComentarioManager comentarioManager;
    private Button btnSendComment;
    private FirebaseRepositorio firebaseRepo;
    private AdminSQLiteOpenHelper dbHelper;
    private int currentUserId;
    private static final String TIPO_SERVICIO = "discord";
    private static final String TAG = "DiscordActivity";
    private ListenerRegistration firestoreRegistration; // Listener de Firestore
    private Query firestoreQuery; // Consulta de Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicialización DB y Firebase
        dbHelper = new AdminSQLiteOpenHelper(this);
        firebaseRepo = new FirebaseRepositorio();

        setContentView(R.layout.activity_discord);

        // Configuración del toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estado de Discord");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Referencias de vistas
        commentsContainer = findViewById(R.id.commentsContainer);
        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        ScrollView scroll = findViewById(R.id.scrollComments);

        // Obtener usuario logueado
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        // Si no hay usuario → cerrar
        if (currentUserId == -1) {
            finish();
            return;
        }

        // Inicializar ComentarioManager
        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scroll,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        // Cargar estado del servidor
        loadDiscordServices();

        // Cargar comentarios locales
        comentarioManager.loadComments(TIPO_SERVICIO);

        // Iniciar escucha en Firebase
        startFirebaseListener(TIPO_SERVICIO);

        // Botón enviar comentario
        btnSendComment.setOnClickListener(v -> {
            String message = edtNewComment.getText().toString().trim();
            if (!message.isEmpty()) {
                comentarioManager.enviarComentario(TIPO_SERVICIO, message);
                edtNewComment.setText("");
            }
        });
    }

    // Inicia listener de Firebase para actualizaciones en tiempo real
    private void startFirebaseListener(String tipoServicio) {
        // Obtener consulta
        firestoreQuery = firebaseRepo.getComentariosQuery(tipoServicio);

        // Adjuntar listener
        firestoreRegistration = firestoreQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.w(TAG, "Error de escucha:", error);
                return;
            }

            // Recargar comentarios
            comentarioManager.loadComments(tipoServicio);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Quitar listener al salir
        if (firestoreRegistration != null) {
            firestoreRegistration.remove();
            Log.d(TAG, "Listener desconectado.");
        }
    }

    // Cargar estado del servidor oficial de Discord
    private void loadDiscordServices() {
        LinearLayout servicesContainer = findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews();

        final String URL = "https://discordstatus.com/api/v2/components.json";

        // Hilo para obtener JSON
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            runOnUiThread(() -> populateDiscordServices(json));
        }).start();
    }

    // Procesa el JSON obtenido del servidor
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

    // Crea una caja con un servicio y su estado
    private LinearLayout createServiceBlock(String name, String status) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setPadding(0, 8, 0, 8);

        // Nombre del servicio
        TextView tv = new TextView(this);
        tv.setTextSize(18);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setText(name + " : " + status);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Indicador de color
        View circle = new View(this);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(40, 40);
        circleParams.setMarginStart(8);
        circle.setLayoutParams(circleParams);

        // Color según estado
        int drawable;
        switch (status.toLowerCase()) {
            case "partial_outage":
            case "operational":
                drawable = R.drawable.circle_green;
                break;
            case "degraded_performance":
                drawable = R.drawable.circle_yellow;
                break;
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

    // Acción de la flecha atrás
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
