package com.devst.verservidores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.activity.EdgeToEdge;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.devst.verservidores.db.AdminSQLiteOpenHelper;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Referencias a los elementos de UI
        CardView cardEpic = findViewById(R.id.cardEpic);
        CardView cardDiscord = findViewById(R.id.cardDiscord);
        ImageView iconConfig = findViewById(R.id.iconConfig);
        ImageView iconPerfil = findViewById(R.id.iconPerfil);

        // Animación para los CardView
        Animation zoom = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        // Navegación a ConfigActivity
        iconConfig.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ConfigActivity.class)));

        // Navegación a PerfilActivity
        iconPerfil.setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));

        // Navegación a EpicActivity con animación
        cardEpic.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, EpicActivity.class));
        });

        // Navegación a DiscordActivity con animación
        cardDiscord.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, DiscordActivity.class));
        });
    }

    private void cargarFotoPerfil() {
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return;

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        // Imagen por defecto
        ImageView iconPerfil = findViewById(R.id.iconPerfil);
        iconPerfil.setImageResource(R.drawable.user);

        if (cursor.moveToFirst()) {
            String foto = cursor.getString(0);
            if (foto != null && !foto.isEmpty()) {
                iconPerfil.setImageURI(Uri.parse(foto));
            }
        }

        cursor.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualiza la foto si se cambió
        cargarFotoPerfil();
    }

}
