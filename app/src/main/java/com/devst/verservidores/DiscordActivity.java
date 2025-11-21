package com.devst.verservidores;
//Librerias necesarias
import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.text.DateFormat;
import java.util.Date;

public class DiscordActivity extends AppCompatActivity {
    //Implementamos Objetos
    // Contenedor donde se agregan los comentarios cargados
    private LinearLayout commentsContainer;
    // Campo para escribir un nuevo comentario
    private EditText edtNewComment;
    //Referencia a la clase ComentarioManager
    private ComentarioManager comentarioManager;
    // Botón de enviar comentario
    private Button btnSendComment;
    private FirebaseRepositorio firebaseRepo;
    // Acceso a la base de datos SQLite
    private AdminSQLiteOpenHelper dbHelper;
    // ID del usuario logueado (se obtiene desde SharedPreferences)
    private int currentUserId; // Id del usuario actual (simulado)
    // Tipo de servicio para filtrar comentarios (Discord)
    private static final String TIPO_SERVICIO = "discord";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseRepo = new FirebaseRepositorio();
        setContentView(R.layout.activity_discord);

        //Configuración del Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Flecha atrás
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Título
            getSupportActionBar().setTitle("Estado de Discord");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        //Referencias a las vistas
        commentsContainer = findViewById(R.id.commentsContainer);
        edtNewComment = findViewById(R.id.edtNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);

        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener usuario logueado desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1); // -1 si no hay usuario logueado
        // Si no hay usuario logueado → cerrar actividad
        if(currentUserId == -1){
            finish();
            return;
        }
        // Crear ComentarioManager DESPUÉS de tener el id del usuario
        ScrollView scroll = findViewById(R.id.scrollComments);

        // Inicializar FirebaseRepositorio
        firebaseRepo = new FirebaseRepositorio();
        comentarioManager = new ComentarioManager(
                this,
                commentsContainer,
                scroll,
                dbHelper,
                firebaseRepo,
                currentUserId
        );

        // Cargar servicios del estado oficial de Discord a traves del endpoint
        loadDiscordServices();

        comentarioManager.loadComments(TIPO_SERVICIO); // cargar comentarios existentes de la base de datos

        //Función boton enviar comentarios
        //Función boton enviar comentarios
        btnSendComment.setOnClickListener(v -> {
            String message = edtNewComment.getText().toString().trim();
            if (!message.isEmpty()) {

                // ** IMPLEMENTACIÓN FIREBASE: El Manager lo hace todo **
                comentarioManager.enviarComentario(TIPO_SERVICIO, message);

                // Limpiar EditText
                edtNewComment.setText("");
            }
        });
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
