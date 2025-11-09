package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DiscordActivity extends AppCompatActivity {
    private TextView tvDiscord;
    private static final String URL = "https://discordstatus.com/api/v2/summary.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discord);
        tvDiscord = findViewById(R.id.tvDiscord);

        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            final String out = parseDiscord(json);

            runOnUiThread(() -> tvDiscord.setText(out));
        }).start();
    }

    // Metodo para retornar si no existe conexión con Discord
    private String parseDiscord(String json) {
        if (json == null) return "No se pudo obtener información.";

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(json, JsonObject.class);

        JsonObject status = root.getAsJsonObject("status");
        String indicator = status.get("indicator").getAsString();
        String description = status.get("description").getAsString();

        return "Estado general: " + description + "\nIndicador: " + indicator;
    }
}
