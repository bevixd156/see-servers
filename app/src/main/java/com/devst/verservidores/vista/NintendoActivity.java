package com.devst.verservidores.vista;

//Librerias necesarias
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

import com.devst.verservidores.R;
import com.devst.verservidores.api.ApiFetcher;
import com.devst.verservidores.comment.ComentarioManager;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray; // Necesario para arrays JSON

public class NintendoActivity extends AppCompatActivity {

    //Views principales
    private LinearLayout servicesContainer, commentsContainer;
    private ComentarioManager comentarioManager;
    private AdminSQLiteOpenHelper dbHelper;
    private FirebaseRepositorio firebaseRepo;

    //Datos del usuario
    private int currentUserId;

    //Constantes
    private static final String TIPO_SERVICIO = "nintendo";
    private static final String URL = "https://www.nintendo.co.jp/netinfo/en_US/status.json";
    private static final String TAG = "NintendoActivity";

    //Firestore
    private ListenerRegistration firestoreRegistration;
    private Query firestoreQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nintendo);

        //Repositorios y DB
        firebaseRepo = new FirebaseRepositorio();
        dbHelper = new AdminSQLiteOpenHelper(this);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estado de Nintendo Network");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        //Referencias UI
        ScrollView scrollComments = findViewById(R.id.scrollComments);
        servicesContainer = findViewById(R.id.servicesContainer);
        commentsContainer = findViewById(R.id.commentsContainer);

        //Cargar usuario actual
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);
        if (currentUserId == -1) {
            finish();
            return;
        }

        //Crear manejador de comentarios
        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scrollComments,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        //Enviar nuevo comentario
        EditText edtNewComment = findViewById(R.id.edtNewComment);
        findViewById(R.id.btnSendComment).setOnClickListener(v -> {
            String texto = edtNewComment.getText().toString().trim();
            if (!texto.isEmpty()) {
                comentarioManager.enviarComentario(TIPO_SERVICIO, texto);
                edtNewComment.setText("");
            }
        });

        //Cargar servicios
        loadNintendoStatus();

        //Cargar comentarios
        comentarioManager.loadComments(TIPO_SERVICIO);

        //Iniciar escucha Firestore
        startFirebaseListener(TIPO_SERVICIO);
    }

    //Iniciar escucha en Firestore
    private void startFirebaseListener(String tipoServicio) {
        firestoreQuery = firebaseRepo.getComentariosQuery(tipoServicio);

        firestoreRegistration = firestoreQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.w(TAG, "Error de escucha en Firestore:", error);
                return;
            }

            Log.d(TAG, "Cambios detectados en Firestore, recargando UI.");

            comentarioManager.loadComments(tipoServicio);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Detener listener Firestore
        if (firestoreRegistration != null) {
            firestoreRegistration.remove();
            Log.d(TAG, "Listener de Firestore desconectado.");
        }
    }

    //Cargar estado del servicio Nintendo
    private void loadNintendoStatus() {
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            runOnUiThread(() -> populateServices(json));
        }).start();
    }

    //Procesar JSON y mostrar servicios (ADAPTADO AL FORMATO DE NINTENDO)
    // Archivo: NintendoActivity.java (Metodo populateServices COMPLETO)
    private void populateServices(String json) {
        if (json == null) {
            // Error de conexión como lo vemos en las otras vistas
            servicesContainer.addView(createServiceBlock("Error de conexión", "error"));
            return;
        }

        Gson g = new Gson();
        servicesContainer.removeAllViews();
        boolean dataFound = false;

        try {
            JsonObject obj = g.fromJson(json, JsonObject.class);

            // --- INICIO: LISTA ESTÁTICA SI NO HAY PROBLEMAS (Sección 1) ---
            // Verificamos si el JSON está vacío o si los arrays de problemas están vacíos.
            boolean jsonIsEmpty = (obj == null || obj.size() == 0);
            boolean arraysAreEmpty = obj != null &&
                    (!obj.has("operational_statuses") || obj.getAsJsonArray("operational_statuses").size() == 0) &&
                    (!obj.has("maintenance_info") || obj.getAsJsonArray("maintenance_info").size() == 0);

            if (jsonIsEmpty || arraysAreEmpty)
            {
                // MOSTRAR LISTA ESTÁTICA: Servicios principales en estado "operational"
                // Servicios de Nintendo Switch
                servicesContainer.addView(createServiceBlock("Nintendo Switch Network Services", "operational"));
                servicesContainer.addView(createServiceBlock("Nintendo eShop (Switch)", "operational"));
                servicesContainer.addView(createServiceBlock("Juego en Línea (Switch)", "operational"));
                servicesContainer.addView(createServiceBlock("Cuenta Nintendo / Nintendo Account", "operational"));
                servicesContainer.addView(createServiceBlock("Aplicación Nintendo Switch Online", "operational"));
                servicesContainer.addView(createServiceBlock("Actualizaciones de Software/Sistema", "operational"));

                // Servicios de otras consolas (Wii U / 3DS)
                servicesContainer.addView(createServiceBlock("Wii U Network Services", "operational"));
                servicesContainer.addView(createServiceBlock("Nintendo 3DS Network Services", "operational"));
                servicesContainer.addView(createServiceBlock("Tienda Nintendo eShop (Wii U/3DS)", "operational"));

                // Servicios Web/Otros
                servicesContainer.addView(createServiceBlock("Mi Nintendo (My Nintendo)", "operational"));
                servicesContainer.addView(createServiceBlock("Servicio de Amigos y Mensajería", "operational"));
                servicesContainer.addView(createServiceBlock("Servicios de Navegación Web", "operational"));
                return; // Detenemos el metodo aquí.
            }
            // --- FIN: LISTA ESTÁTICA SI NO HAY PROBLEMAS ---


            // 2. Procesar ESTADOS OPERACIONALES (Operational_statuses)
            if (obj.has("operational_statuses") && obj.get("operational_statuses").isJsonArray()) {
                JsonArray operationalArray = obj.getAsJsonArray("operational_statuses");
                if (operationalArray.size() > 0) {
                    servicesContainer.addView(createHeader("--- ESTADO DE SERVICIOS ---"));
                    for (JsonElement el : operationalArray) {
                        JsonObject service = el.getAsJsonObject();
                        String name = service.has("name") ? service.get("name").getAsString() :
                                (service.has("title") ? service.get("title").getAsString() : "Servicio Desconocido");
                        String status = service.has("status") ? service.get("status").getAsString() : "unknown";

                        servicesContainer.addView(createServiceBlock(name, status));
                        dataFound = true;
                    }
                }
            }

            // 3. Procesar MANTENIMIENTO (maintenance_info)
            if (obj.has("maintenance_info") && obj.get("maintenance_info").isJsonArray()) {
                JsonArray maintenanceArray = obj.getAsJsonArray("maintenance_info");
                if (maintenanceArray.size() > 0) {
                    servicesContainer.addView(createHeader("--- MANTENIMIENTO PROGRAMADO ---"));

                    for (JsonElement el : maintenanceArray) {
                        JsonObject service = el.getAsJsonObject();
                        String title = service.has("title") ? service.get("title").getAsString() : "Servicio en Mantenimiento";

                        // Mostramos el título y el estado de mantenimiento.
                        servicesContainer.addView(createServiceBlock(title, "maintenance"));
                        dataFound = true;
                    }
                }
            }

            // El Fallback (punto 4) ya no es necesario aquí, ya que la Sección 1 maneja el caso de "no data found".
            // Si dataFound sigue siendo false, fue capturado por la Sección 1.

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar JSON de Nintendo: " + e.getMessage());
            servicesContainer.addView(createServiceBlock("Error de formato JSON (Catch)", "error"));
        }
    }

    // Metodo auxiliar para crear un encabezado
    private TextView createHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
        header.setPadding(0, 16, 0, 8);
        return header;
    }

    //Crear bloque visual de servicio
    private LinearLayout createServiceBlock(String name, String status) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setPadding(0, 8, 0, 8);

        TextView tv = new TextView(this);
        tv.setTextSize(18);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(name + " : " + status);

        View circle = new View(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(40, 40);
        cp.setMarginStart(8);
        circle.setLayoutParams(cp);

        int drawable;
        // Mapeo basado en términos de Nintendo
        switch (status.toLowerCase().split(":")[0].trim()) { // Split para manejar "MAINTENANCE: tiempo"
            case "available":
            case "operational":
                drawable = R.drawable.circle_green;
                break;

            case "scheduled maintenance":
            case "maintenance":
                drawable = R.drawable.circle_yellow;
                break;

            case "unavailable":
            case "down":
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