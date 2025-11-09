package com.devst.verservidores;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Importamos las
        CardView cardEpic = findViewById(R.id.cardEpic);
        CardView cardDiscord = findViewById(R.id.cardDiscord);
        ImageView iconConfig = findViewById(R.id.iconConfig);

        Animation zoom = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        //Evento imagen configuración
        iconConfig.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
            startActivity(intent);
        });
        //Evento apartado Epic Games
        cardEpic.setOnClickListener(View -> {
            View.startAnimation(zoom);
            startActivity(new Intent(this, EpicActivity.class));
        });
        //Evento apartado Discord
        cardDiscord.setOnClickListener(View -> {
            View.startAnimation(zoom);
            startActivity(new Intent(this, DiscordActivity.class));
        });
    }
}