package com.devst.verservidores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.devst.verservidores.db.AdminSQLiteOpenHelper;

import java.io.File;

public class PerfilActivity extends AppCompatActivity {

    private ImageView imgPerfil;
    private TextView txtNombre, txtCorreo, txtFechaRegistro;
    private Button btnEditarPerfil, btnCerrarSesion;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        imgPerfil = findViewById(R.id.imgPerfil);
        txtNombre = findViewById(R.id.txtNombrePerfil);
        txtCorreo = findViewById(R.id.txtCorreoPerfil);
        txtFechaRegistro = findViewById(R.id.txtFechaRegistro);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configuración del Toolbar (mantener código original)
        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil"); // Título más apropiado
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            // Si no hay sesión, redirigir al Login y terminar.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        cargarDatosUsuario(userId);

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, EditarPerfilActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        // Lógica de CERRAR SESIÓN (CORREGIDA)
        btnCerrarSesion.setOnClickListener(v -> {
            // 1. Limpiar SharedPreferences para eliminar la sesión
            prefs.edit().clear().apply();

            Toast.makeText(PerfilActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // 2. Redirigir a LoginActivity y limpiar la pila de actividades
            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            // Flags para evitar que el usuario vuelva a Home/Perfil con el botón de retroceso
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void cargarDatosUsuario(int userId) {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT nombre, correo, foto_perfil, fecha_registro FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        imgPerfil.setImageResource(R.drawable.user);

        if (cursor.moveToFirst()) {
            txtNombre.setText(cursor.getString(0));
            txtCorreo.setText(cursor.getString(1));
            txtFechaRegistro.setText("Se unió el: " + cursor.getString(3));

            String foto = cursor.getString(2);
            if (foto != null && !foto.isEmpty()) {
                // Se puede simplificar a Uri.parse(foto)
                try {
                    Uri fotoUri = Uri.parse(foto);
                    // El File check es importante para URIs de archivo internas
                    File file = new File(fotoUri.getPath());
                    if (file.exists()) {
                        imgPerfil.setImageURI(fotoUri);
                    } else {
                        // Si el archivo no existe (error de ruta), vuelve al default
                        imgPerfil.setImageResource(R.drawable.user);
                    }
                } catch (Exception e) {
                    // Si el parseo falla
                    imgPerfil.setImageResource(R.drawable.user);
                }
            }
        }

        cursor.close();
        db.close();
    }

    // Acción botón "Atrás" (MANTENER CÓDIGO ORIGINAL)
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cargar datos del usuario cada vez que la actividad vuelve al frente
        if (userId != -1) {
            cargarDatosUsuario(userId);
        }
    }
}