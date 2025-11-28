package com.devst.verservidores.vista;

// Importaciones necesarias
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.devst.verservidores.R;
import com.devst.verservidores.api.ApiFetcher;
import com.devst.verservidores.comment.ComentarioManager;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GoogleCloudActivity extends AppCompatActivity {

    // Contenedores de servicios y comentarios
    private LinearLayout servicesContainer, commentsContainer;

    // EditText y botón para enviar comentarios
    private EditText edtNewComment;
    private Button btnSendComment;

    // Manager de comentarios
    private ComentarioManager comentarioManager;

    // Firebase y DB
    private FirebaseRepositorio firebaseRepo;
    private AdminSQLiteOpenHelper dbHelper;

    // Listener y query de Firestore
    private ListenerRegistration firestoreRegistration;
    private Query firestoreQuery;

    // ID del usuario actual
    private int currentUserId;

    // Constantes de la clase
    private static final String TIPO_SERVICIO = "googlecloud";
    private static final String STATUS_URL = "https://status.cloud.google.com/incidents.json";
    private static final String TAG = "GoogleCloudActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_googlecloud);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estado de Google Cloud");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Referencias de vistas
        servicesContainer = findViewById(R.id.servicesContainer);
        commentsContainer = findViewById(R.id.commentsContainer);
        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        ScrollView scrollComments = findViewById(R.id.scrollComments);

        // Inicializar Firebase y DB
        firebaseRepo = new FirebaseRepositorio();
        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener usuario logueado
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);
        if(currentUserId==-1){
            // Si no hay usuario, cerrar actividad
            finish();
            return;
        }

        // Inicializar ComentarioManager
        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scrollComments,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        // Configurar botón enviar comentario
        btnSendComment.setOnClickListener(v -> {
            String texto = edtNewComment.getText().toString().trim();
            if(!texto.isEmpty()){
                comentarioManager.enviarComentario(TIPO_SERVICIO, texto);
                edtNewComment.setText("");
            }
        });

        // Cargar estado de Google Cloud y comentarios
        loadGoogleCloudStatus();
        comentarioManager.loadComments(TIPO_SERVICIO);
        startFirebaseListener(TIPO_SERVICIO);
    }

    // Inicializar listener de Firebase para comentarios
    private void startFirebaseListener(String tipoServicio){
        firestoreQuery = firebaseRepo.getComentariosQuery(tipoServicio);
        firestoreRegistration = firestoreQuery.addSnapshotListener((snapshots, error) -> {
            if(error!=null){
                Log.w(TAG,"Error Firestore:", error);
                return;
            }
            // Recargar comentarios
            comentarioManager.loadComments(tipoServicio);
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        // Quitar listener de Firestore al cerrar actividad
        if(firestoreRegistration!=null){
            firestoreRegistration.remove();
        }
    }

    // Cargar estado de Google Cloud desde JSON
    private void loadGoogleCloudStatus(){
        new Thread(() -> {
            try {
                String json = ApiFetcher.getJson(STATUS_URL);
                if(json != null){
                    Gson gson = new Gson();
                    JsonArray incidents = gson.fromJson(json, JsonArray.class);
                    // Mostrar servicios en UI
                    runOnUiThread(() -> populateServices(incidents));
                }
            } catch(Exception e){
                Log.e(TAG,"Error cargando Google Cloud", e);
            }
        }).start();
    }

    // Procesar JSON y mostrar bloques de servicios
    private void populateServices(JsonArray incidents){
        servicesContainer.removeAllViews();

        // Listado estático de servicios de Google Cloud
        String[] services = {
                "Compute Engine", "App Engine", "Cloud Storage",
                "BigQuery", "Cloud Functions", "Cloud SQL",
                "Cloud Run", "Kubernetes Engine", "Pub/Sub"
        };

        for(String service : services){
            String status = "operational";

            // Revisar incidentes activos para actualizar estado
            for(int i=0;i<incidents.size();i++){
                JsonObject incident = incidents.get(i).getAsJsonObject();
                String incidentService = incident.has("service") ? incident.get("service").getAsString() : "";
                String incidentStatus = incident.has("status") ? incident.get("status").getAsString() : "";

                if(service.equalsIgnoreCase(incidentService)){
                    status = incidentStatus;
                    break;
                }
            }

            // Crear bloque visual del servicio
            servicesContainer.addView(createServiceBlock(service, status));
        }
    }

    // Crear bloque visual de servicio con indicador de estado
    private LinearLayout createServiceBlock(String name, String status){
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setPadding(0,8,0,8);

        // TextView con nombre y estado del servicio
        TextView tv = new TextView(this);
        tv.setTextSize(18);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        tv.setText(name + " : " + status.toUpperCase());

        // Indicador de estado
        View circle = new View(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(40,40);
        cp.setMarginStart(8);
        circle.setLayoutParams(cp);

        // Selección de color según estado
        int drawable;
        switch(status.toLowerCase()){
            case "operational":
                drawable = R.drawable.circle_green; break;
            case "service disruption":
            case "partial outage":
            case "major outage":
                drawable = R.drawable.circle_red; break;
            default:
                drawable = R.drawable.circle_yellow; break;
        }
        circle.setBackground(ContextCompat.getDrawable(this, drawable));

        // Agregar TextView y círculo al bloque
        block.addView(tv);
        block.addView(circle);
        return block;
    }

    // Acción de la flecha atrás
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
