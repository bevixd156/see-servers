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
        txtFechaRegistro = findViewById(R.id.txtFechaRegistro); // 🔹 Nuevo TextView
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Función flecha atras
        if (toolbar != null && getSupportActionBar() != null) {
            // Activar flecha "Atrás"
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Título del activity
            getSupportActionBar().setTitle("Ajustes");
            // Color blanco para la flecha
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) return;

        cargarDatosUsuario(userId);

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, EditarPerfilActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        btnCerrarSesion.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
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
                File file = new File(Uri.parse(foto).getPath());
                if (file.exists()) {
                    imgPerfil.setImageURI(Uri.fromFile(file));
                }
            }
        }

        cursor.close();
        db.close();
    }

    //Acción botón "Atrás"
    @Override
    public boolean onSupportNavigateUp(){
        //Cerrar la actividad y retorna atrás
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosUsuario(userId);
    }
}
