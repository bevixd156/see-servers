package com.devst.verservidores.vista;
// Importaciones necesarias
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.Glide;
import com.devst.verservidores.R;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Referencias UI
        Toolbar toolbar = findViewById(R.id.toolbar);
        CardView cardEpic = findViewById(R.id.cardEpic);
        CardView cardDiscord = findViewById(R.id.cardDiscord);
        CardView cardNintendo = findViewById(R.id.cardNintendo);
        CardView cardCloudflare = findViewById(R.id.cardCloudflare);
        CardView cardGithub = findViewById(R.id.cardGithub);
        CardView cardGooglecloud = findViewById(R.id.cardGooglecloud);
        ImageView iconConfig = findViewById(R.id.iconConfig);
        ImageView iconPerfil = findViewById(R.id.iconPerfil);

        // Animación para los cardviews
        Animation zoom = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        // Abrir configuración
        iconConfig.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ConfigActivity.class))
        );

        // Abrir perfil del usuario
        iconPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class))
        );

        // Ir a EpicActivity con animación
        cardEpic.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, EpicActivity.class));
        });

        // Ir a DiscordActivity con animación
        cardDiscord.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, DiscordActivity.class));
        });

        // Ir a NintendoActivity con animación
        cardNintendo.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, NintendoActivity.class));
        });

        // Ir a Cloudflare con animación
        cardCloudflare.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, CloudflareActivity.class));
        });

        // Ir a Github con animación
        cardGithub.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, GithubActivity.class));
        });

        // Ir a Google Cloud con animación
        cardGooglecloud.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, GoogleCloudActivity.class));
        });

        // Activar toolbar
        setSupportActionBar(toolbar);
    }

    // Cargar foto de perfil del usuario desde SQLite
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

        ImageView iconPerfil = findViewById(R.id.iconPerfil);
        iconPerfil.setImageResource(R.drawable.user); // placeholder por defecto

        if (cursor.moveToFirst()) {
            String base64Foto = cursor.getString(0);

            if (base64Foto != null && !base64Foto.isEmpty()) {
                try {
                    // // Decodificar BASE64 y cargar con Glide
                    byte[] imageBytes = Base64.decode(base64Foto, Base64.DEFAULT);

                    Glide.with(this)
                            .load(imageBytes)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .circleCrop()
                            .into(iconPerfil);

                } catch (IllegalArgumentException e) {
                    Log.e("HomeActivity", "Error Base64: " + e.getMessage());
                    iconPerfil.setImageResource(R.drawable.user);
                }
            }
        }

        cursor.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar foto al volver
        cargarFotoPerfil();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar menú superior
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Eventos del menú
        if (item.getItemId() == R.id.cerrar_sesion) {
            startActivity(new Intent(this, LoginActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
