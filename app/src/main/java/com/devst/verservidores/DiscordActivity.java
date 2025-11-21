package com.devst.verservidores;
//Librerias necesarias
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class DiscordActivity extends AppCompatActivity {
    //Implementamos Objetos
    private LinearLayout commentsContainer;
    private EditText edtNewComment;
    private ComentarioManager comentarioManager;
    private Button btnSendComment;
    private FirebaseRepositorio firebaseRepo;
    private AdminSQLiteOpenHelper dbHelper;
    private int currentUserId;
    private static final String TIPO_SERVICIO = "discord";
    private static final String TAG = "DiscordActivity";
    private ListenerRegistration firestoreRegistration; // El objeto para desconectar el listener
    private Query firestoreQuery; // La referencia de la consulta

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // La inicialización del repositorio y la base de datos se mueve al inicio.
        dbHelper = new AdminSQLiteOpenHelper(this);
        firebaseRepo = new FirebaseRepositorio();

        setContentView(R.layout.activity_discord);

        // Configuración del Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estado de Discord");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Referencias a las vistas
        commentsContainer = findViewById(R.id.commentsContainer);
        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        ScrollView scroll = findViewById(R.id.scrollComments);


        // Obtener usuario logueado desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        // Si no hay usuario logueado → cerrar actividad
        if(currentUserId == -1){
            finish();
            return;
        }

        // ✅ ÚNICA Y CORRECTA INICIALIZACIÓN DEL ComentarioManager
        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scroll,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        // Cargar servicios del estado oficial de Discord
        loadDiscordServices();

        // Cargar comentarios existentes de la base de datos local
        comentarioManager.loadComments(TIPO_SERVICIO);

        // ✅ INICIAR ESCUCHA DE FIREBASE AHORA
        startFirebaseListener(TIPO_SERVICIO);

        // Función boton enviar comentarios
        btnSendComment.setOnClickListener(v -> {
            String message = edtNewComment.getText().toString().trim();
            if (!message.isEmpty()) {
                comentarioManager.enviarComentario(TIPO_SERVICIO, message);
                edtNewComment.setText("");
            }
        });
    }

    // ===========================
    // ✅ METODO PARA INICIAR LA ESCUCHA DE FIREBASE
    // ===========================
    // Este metodo usa el Listener de Firestore para obtener actualizaciones en tiempo real
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

            // La misma llamada para recargar los comentarios de SQLite (que se actualizan con la app)
            comentarioManager.loadComments(tipoServicio);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ❌ LIMPIEZA CRUCIAL: Detener el listener de Firestore
        if (firestoreRegistration != null) {
            firestoreRegistration.remove();
            Log.d(TAG, "Listener de Firestore desconectado.");
        }
    }

    //Obtener el estado del servidor de Discord
    private void loadDiscordServices() {
        LinearLayout servicesContainer = findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews();

        // URL del estado oficial de Discord (JSON)
        final String URL = "https://discordstatus.com/api/v2/components.json";

        // Hilo para el JSON
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL); // Metodo para obtener JSON
            runOnUiThread(() -> populateDiscordServices(json));
        }).start();
    }

    //Procesar la peticion para el JSON recibido del servidor de Discord
    private void populateDiscordServices(String json) {
        if (json == null) return;
        //Convertidor de objetos Java y Json
        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);
        //Referenciamos a la vista
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
        block.setPadding(0, 8, 0, 8);

        //Nombre del servicio
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

        // Colores según estado
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

    //Acción para la flecha atras
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}