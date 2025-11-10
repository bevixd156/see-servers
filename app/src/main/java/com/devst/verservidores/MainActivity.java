package com.devst.verservidores;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        CardView cardEpic = findViewById(R.id.cardEpic);
        CardView cardDiscord = findViewById(R.id.cardDiscord);
        ImageView iconConfig = findViewById(R.id.iconConfig);

        Animation zoom = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        // Evento imagen configuración
        iconConfig.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
            startActivity(intent);
        });

        // Evento apartado Epic Games
        cardEpic.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, EpicActivity.class));
        });

        // Evento apartado Discord
        cardDiscord.setOnClickListener(v -> {
            v.startAnimation(zoom);
            startActivity(new Intent(this, DiscordActivity.class));
        });
    }
}
