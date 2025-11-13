package com.devst.verservidores;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

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

        //Configuracion de Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajustes");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        //Boton "Atrás"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajustes");

            //Color blanco para el botón superior
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }
        // Función para el cambio de tema
        SwitchMaterial switchTema = findViewById(R.id.switchTema);

        // Leer preferencia guardada
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);
        switchTema.setChecked(modoOscuro);

        // Aplicar tema
        AppCompatDelegate.setDefaultNightMode(
                modoOscuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Guardar elección
            prefs.edit().putBoolean("modo_oscuro", isChecked).apply();

            // Aplicar modo
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        Button btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta);
        btnEliminarCuenta.setOnClickListener(v -> mostrarDialogoEliminarCuenta());

    }

    //Diálogo para eliminar la cuenta
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
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return;

        com.devst.verservidores.db.AdminSQLiteOpenHelper admin = new com.devst.verservidores.db.AdminSQLiteOpenHelper(this);
        android.database.sqlite.SQLiteDatabase db = admin.getWritableDatabase();

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