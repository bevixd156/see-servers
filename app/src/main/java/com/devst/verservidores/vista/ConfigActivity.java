package com.devst.verservidores.vista;

// Librerías necesarias
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.devst.verservidores.R;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config);

        // Icono para abrir el repositorio en GitHub
        ImageView gitHubWeb = findViewById(R.id.gitHubWeb);
        // Abre el navegador con el proyecto
        gitHubWeb.setOnClickListener(v -> {
            String url = "https://github.com/bevixd156/see-servers";
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            startActivity(intent);
        });

        // Configuración del toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Flecha atrás y título
        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajustes");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Switch para cambiar entre modo claro y oscuro
        SwitchMaterial switchTema = findViewById(R.id.switchTema);

        // Leer preferencia del tema
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);

        // Estado actual del switch
        switchTema.setChecked(modoOscuro);

        // Aplicar tema al iniciar
        AppCompatDelegate.setDefaultNightMode(
                modoOscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Guardar y aplicar cuando el usuario cambia el tema
        switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("modo_oscuro", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Botón para eliminar la cuenta
        Button btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        // Abrir diálogo de confirmación
        btnEliminarCuenta.setOnClickListener(v -> mostrarDialogoEliminarCuenta());
    }

    // Ventana emergente para confirmar eliminación
    private void mostrarDialogoEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que quieres eliminar la cuenta? Esta acción es irreversible y eliminará sus comentarios.")
                .setPositiveButton("Sí", (dialog, which) -> eliminarCuenta())
                .setNegativeButton("No", null)
                .show();
    }

    // Eliminar cuenta local + Firebase
    private void eliminarCuenta() {
        // Obtener ID guardado del usuario
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        int userIdInt = prefs.getInt("user_id", -1);
        final String userId = String.valueOf(userIdInt);

        // Si no hay usuario, cancelar
        if (userIdInt == -1) return;

        // 1. Eliminar datos locales en SQLite
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        android.database.sqlite.SQLiteDatabase db = admin.getWritableDatabase();

        // Eliminar usuario (los comentarios se eliminan por ON DELETE CASCADE)
        int rows = db.delete("usuarios", "id = ?", new String[]{String.valueOf(userIdInt)});
        db.close();

        if (rows > 0) {
            // 2. Eliminar datos del usuario en Firebase
            FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();

            firebaseRepo.eliminarUsuarioYComentariosFirestore(userId, task -> {
                if (task.isSuccessful()) {
                    // Eliminación completa en Firebase

                    // Limpiar preferencias locales
                    prefs.edit().clear().apply();

                    // Mensaje de éxito
                    android.widget.Toast.makeText(this, "Cuenta eliminada con éxito", android.widget.Toast.LENGTH_LONG).show();

                    // Redirigir al login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    // Error al eliminar en Firebase
                    Log.e("ConfigActivity", "Error al eliminar datos de Firebase: ", task.getException());

                    android.widget.Toast.makeText(this, "Error: La cuenta local se eliminó, pero los datos de la nube persisten.", android.widget.Toast.LENGTH_LONG).show();

                    // Aun así limpiamos lo local
                    prefs.edit().clear().apply();

                    // Redirigir al login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });

        } else {
            // Error eliminando localmente
            android.widget.Toast.makeText(this, "Error al eliminar la cuenta localmente", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // Acción de botón atrás
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
