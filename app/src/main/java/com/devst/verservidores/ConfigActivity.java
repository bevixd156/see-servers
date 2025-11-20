package com.devst.verservidores;

//Librerias necesarias
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config);

        //Referencia al icono que abre el repositorio de GitHub
        ImageView gitHubWeb = findViewById(R.id.gitHubWeb);
        //Abre el navegador a la pagina del repositorio de github
        gitHubWeb.setOnClickListener(v -> {
            String url = "https://github.com/bevixd156/see-servers";
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(intent);
        });

        //Configuracion de Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Función flecha atras
        if (toolbar != null && getSupportActionBar() != null) {
            //Activar flecha "Atrás"
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //Título del activity
            getSupportActionBar().setTitle("Ajustes");
            //Color blanco para la flecha
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        //Función para el cambio de Modo Claro y Oscuro
        SwitchMaterial switchTema = findViewById(R.id.switchTema);

        //Leer preferencia guardada
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);
        //Estado del Switch
        switchTema.setChecked(modoOscuro);

        // Aplicar tema actual al iniciar
        AppCompatDelegate.setDefaultNightMode(
                modoOscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        // Listener para cuando el usuario cambia el modo
        switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Guardar elección
            prefs.edit().putBoolean("modo_oscuro", isChecked).apply();

            // Aplicar tema
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Referenciamos el boton de eliminar la cuenta
        Button btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        //Evento de boton de eliminar cuenta
        btnEliminarCuenta.setOnClickListener(v ->
                mostrarDialogoEliminarCuenta());

    }

    //mini PopUp para eliminar la cuenta
    private void mostrarDialogoEliminarCuenta() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que quieres eliminar la cuenta?")
                .setPositiveButton("Sí", (dialog, which) -> eliminarCuenta())
                .setNegativeButton("No", null)
                .show();
    }

    //Función para Delete de eliminar la cuenta
    private void eliminarCuenta() {
        // Obtener ID del usuario desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        // Si no hay usuario logeado, no seguimos
        if (userId == -1) return;

        // Abrir base de datos
        com.devst.verservidores.db.AdminSQLiteOpenHelper admin =
                new com.devst.verservidores.db.AdminSQLiteOpenHelper(this);
        android.database.sqlite.SQLiteDatabase db = admin.getWritableDatabase();

        // Ejecutar DELETE en la tabla usuarios
        int rows = db.delete("usuarios", "id = ?", new String[]{String.valueOf(userId)});
        db.close();

        if (rows > 0) {
            // Limpiar SharedPreferences
            prefs.edit().clear().apply();

            // Mostrar mensaje
            android.widget.Toast.makeText(this, "Cuenta eliminada con éxito", android.widget.Toast.LENGTH_LONG).show();

            // Redirigir al LoginActivity
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            android.widget.Toast.makeText(this, "Error al eliminar la cuenta", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    //Acción botón "Atrás"
    @Override
    public boolean onSupportNavigateUp(){
        //Cerrar la actividad y retorna atrás
        finish();
        return true;
    }
}