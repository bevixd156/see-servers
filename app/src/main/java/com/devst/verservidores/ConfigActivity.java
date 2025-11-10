package com.devst.verservidores;

import android.content.SharedPreferences;
import android.os.Bundle;

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

    }

    //Acción botón "Atrás"
    @Override
    public boolean onSupportNavigateUp(){
        //Cerrar la actividad y retorna atrás
        finish();
        return true;
    }
}