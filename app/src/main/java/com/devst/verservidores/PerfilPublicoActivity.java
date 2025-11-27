package com.devst.verservidores;
// Importaciones necesarias
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;

public class PerfilPublicoActivity extends AppCompatActivity {
    // Objetos de la clase
    private ImageView ivProfile;
    private TextView tvName, tvJoinDate;
    private AdminSQLiteOpenHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_publico);

        // Configurar toolbar y botón atrás
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // activar botón atrás
            getSupportActionBar().setTitle("Perfil");              // título pantalla
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white)); // color icono
        }

        // Referencias UI
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvJoinDate = findViewById(R.id.tvJoinDate);

        // Inicializar DB
        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener user_id del Intent
        userId = getIntent().getIntExtra("user_id", -1);

        // Cargar datos si es un id válido
        if(userId != -1){
            loadUserData(userId);
        }
    }

    private void loadUserData(int userId){
        // Consultar usuario por id
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT nombre, fecha_registro, foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        // Si existe el usuario, cargar datos
        if(cursor != null && cursor.moveToFirst()){
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha_registro"));
            String profileUrl = cursor.getString(cursor.getColumnIndexOrThrow("foto_perfil"));

            // Mostrar nombre
            tvName.setText(nombre);

            // Mostrar fecha de registro
            tvJoinDate.setText("Se unió: " + fecha);

            // Mostrar foto o por defecto
            if(profileUrl != null && !profileUrl.isEmpty()){
                Glide.with(this).load(profileUrl).circleCrop().into(ivProfile); // cargar imagen
            } else {
                ivProfile.setImageResource(R.drawable.user); // imagen por defecto
            }

            cursor.close();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // cerrar pantalla
        return true;
    }
}
