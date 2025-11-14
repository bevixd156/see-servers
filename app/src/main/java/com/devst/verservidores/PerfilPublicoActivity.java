package com.devst.verservidores;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;

public class PerfilPublicoActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvJoinDate;
    private AdminSQLiteOpenHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_publico);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Perfil");
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvJoinDate = findViewById(R.id.tvJoinDate);

        dbHelper = new AdminSQLiteOpenHelper(this);

        // Obtener user_id desde Intent
        userId = getIntent().getIntExtra("user_id", -1);
        if(userId != -1){
            loadUserData(userId);
        }
    }

    private void loadUserData(int userId){
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT nombre, fecha_registro, foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        if(cursor != null && cursor.moveToFirst()){
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha_registro"));
            String profileUrl = cursor.getString(cursor.getColumnIndexOrThrow("foto_perfil"));

            tvName.setText(nombre);
            tvJoinDate.setText("Se unió: " + fecha);

            // Mostrar foto o imagen por defecto
            if(profileUrl != null && !profileUrl.isEmpty()){
                Glide.with(this).load(profileUrl).circleCrop().into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.user);
            }

            // Click sobre la imagen
            ivProfile.setOnClickListener(v -> {
                Toast.makeText(this, "Imagen de perfil tocada", Toast.LENGTH_SHORT).show();
                // Si quieres abrir otro perfil, asegúrate de pasar su userId
                Intent intent = new Intent(this, PerfilPublicoActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
            });

            cursor.close();
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
