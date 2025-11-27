package com.devst.verservidores.vista;
// Importaciones necesarias
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.devst.verservidores.R;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;

public class PerfilActivity extends AppCompatActivity {
    // Objetos de la clase
    private ImageView imgPerfil;
    private TextView txtNombre, txtCorreo, txtFechaRegistro;
    private Button btnEditarPerfil, btnCerrarSesion;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // // Referencias UI
        imgPerfil = findViewById(R.id.imgPerfil);
        txtNombre = findViewById(R.id.txtNombrePerfil);
        txtCorreo = findViewById(R.id.txtCorreoPerfil);
        txtFechaRegistro = findViewById(R.id.txtFechaRegistro);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // // Configuración del Toolbar
        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mi Perfil");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // // Obtener sesión guardada
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        // // Si no hay usuario, volver al login
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // // Cargar información del usuario
        cargarDatosUsuario(userId);

        // // Abrir editor de perfil
        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, EditarPerfilActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        // // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Toast.makeText(PerfilActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // // Cargar datos desde SQLite
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

            // // Cargar imagen guardada en Base64
            String base64Foto = cursor.getString(2);

            if (base64Foto != null && !base64Foto.isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decode(base64Foto, Base64.DEFAULT);

                    com.bumptech.glide.Glide.with(this)
                            .load(imageBytes)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .circleCrop()
                            .into(imgPerfil);

                } catch (IllegalArgumentException e) {
                    Log.e("PerfilActivity", "Error al decodificar Base64: " + e.getMessage());
                    imgPerfil.setImageResource(R.drawable.user);
                }
            } else {
                imgPerfil.setImageResource(R.drawable.user);
            }
        }

        cursor.close();
        db.close();
    }

    // // Acción botón atrás
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // // Refrescar datos al volver a la pantalla
    @Override
    protected void onResume() {
        super.onResume();
        if (userId != -1) {
            cargarDatosUsuario(userId);
        }
    }
}
