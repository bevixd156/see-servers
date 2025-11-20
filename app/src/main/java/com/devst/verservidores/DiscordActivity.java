package com.devst.verservidores;
//Librerias necesarias
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
    //Implementamos Objetos
    // Contenedor donde se agregan los comentarios cargados
    private LinearLayout commentsContainer;
    // Campo para escribir un nuevo comentario
    private EditText edtNewComment;
    // Botón de enviar comentario
    private Button btnSendComment;
    // Acceso a la base de datos SQLite
    private AdminSQLiteOpenHelper dbHelper;
    // ID del usuario logueado (se obtiene desde SharedPreferences)
    private int currentUserId; // Id del usuario actual (simulado)
    // Tipo de servicio para filtrar comentarios (Discord)
    private static final String TIPO_SERVICIO = "discord";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Cargar servicios del estado oficial de Discord a traves del endpoint
        loadDiscordServices();

        loadComments(); // cargar comentarios existentes de la base de datos

        //Función boton enviar comentarios
        btnSendComment.setOnClickListener(v -> {
            String message = edtNewComment.getText().toString().trim();
            if (!message.isEmpty()) {
                //Fecha actual
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

    // Cargar Comentarios
    private void loadComments() {
        commentsContainer.removeAllViews();

        Cursor cursor = dbHelper.getComments(TIPO_SERVICIO);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                //Referenciamos los datos de la DB SQLite
                int userIdDelComentario = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String username = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("comentario"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));
                String profileUrl = cursor.getString(cursor.getColumnIndexOrThrow("foto_perfil"));
                //Creamos el bloque visual del comentario
                addComment(username, message, timestamp, profileUrl, userIdDelComentario);

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    //Visualización de cada comentario
    private void addComment(String username, String message, String timestamp, String profileUrl, int useridDelComentario) {
        LinearLayout commentBlock = new LinearLayout(this);
        commentBlock.setOrientation(LinearLayout.HORIZONTAL);
        commentBlock.setPadding(8, 8, 8, 8);
        commentBlock.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_card));

        //Foto de perfil
        ImageView profile = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(80, 80);
        imgParams.setMarginEnd(8);
        profile.setLayoutParams(imgParams);

        //Si el usuario tiene foto de perfil
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .circleCrop()
                    .into(profile);
        } else {
            //Si no existe se pondra la imagen por defecto
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
        //Nombre de usuario
        TextView usernameTv = new TextView(this);
        usernameTv.setText(username);
        usernameTv.setTextSize(16);
        usernameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        //Comentario
        TextView messageTv = new TextView(this);
        messageTv.setText(message);
        messageTv.setTextSize(16);
        //Fecha y hora del comentario
        TextView timestampTv = new TextView(this);
        timestampTv.setText(timestamp);
        timestampTv.setTextSize(12);
        timestampTv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        //Agregamos al contenedor y se asocia el comentario al usuario
        textContainer.addView(usernameTv);
        textContainer.addView(messageTv);
        textContainer.addView(timestampTv);
        //Visualizar el comentario de cada usuario
        commentBlock.addView(profile);
        commentBlock.addView(textContainer);
        //Agregamos el comentario al contenedor
        commentsContainer.addView(commentBlock);

        // Mantener presionado para mostrar las opciones (solo si el comentario pertenece al usuario actual)
        if (useridDelComentario == currentUserId) {
            //Referenciamos al comentario para realizar la funcion
            commentBlock.setOnLongClickListener(v -> {
                //Opciones disponibles
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenu().add("Modificar");
                popup.getMenu().add("Eliminar");
                //Funciones para el UD (Update-Delete)
                popup.setOnMenuItemClickListener(item -> {
                    //Si el usuario quiere modificar
                    if (item.getTitle().equals("Modificar")) {
                        //Se muestra un PopUp donde podrá modificar el comentario
                        showEditDialog(useridDelComentario, message);
                    }
                    //Si el usuario quiere eliminar
                    if (item.getTitle().equals("Eliminar")) {
                        //Se elimina el comentario
                        deleteCommentAndReload(useridDelComentario, message);
                    }
                    //Retornamos true para aplicar los cambios
                    return true;
                });
                popup.show();
                return true;
            });
        }
    }

    //Metodo para mostrar un PopUp de modificar el comentario
    private void showEditDialog(int userId, String oldText) {
        //Título para modificar el comnetario
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Modificar comentario");
        //Final para que no pueda ser reasignada
        final EditText input = new EditText(this);
        input.setText(oldText);
        builder.setView(input);
        //Apartado para guardar el cambio del comentario
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newText = input.getText().toString();
            //Consulta hacia la DB para modificar el comentario por el user_id
            dbHelper.getWritableDatabase().execSQL(
                    "UPDATE comentarios SET comentario = ? WHERE user_id = ? AND comentario = ?",
                    new Object[]{newText, userId, oldText}
            );

            loadComments();
        });
        //Cancelar la acción de modificar
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    //Metodo para eliminar el comentario
    private void deleteCommentAndReload(int userId, String message) {
        dbHelper.getWritableDatabase().execSQL(
                "DELETE FROM comentarios WHERE user_id = ? AND comentario = ?",
                new Object[]{userId, message}
        );
        loadComments();
    }

    //Acción para la flecha atras
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
