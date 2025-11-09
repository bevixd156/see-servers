package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DiscordActivity extends AppCompatActivity {
    private TextView tvDiscord;
    private View statusCircle;
    private static final String URL = "https://discordstatus.com/api/v2/summary.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discord);
        tvDiscord = findViewById(R.id.tvDiscord);
        statusCircle = findViewById(R.id.statusCircle);

        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            final Spannable out = parseDiscord(json);

            runOnUiThread(() -> {
                tvDiscord.setText(out);
                updateStatusCircle(json);
            });
        }).start();
    }

    // Metodo para retornar si no existe conexión con Discord
    private Spannable parseDiscord(String json) {
        if (json == null) return new SpannableString("No se pudo obtener información.");

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(json, JsonObject.class);

        JsonObject status = root.getAsJsonObject("status");
        String indicator = status.get("indicator").getAsString();
        String description = status.get("description").getAsString();

        // Crear el texto completo
        String text = "Estado general: " + description + "\nIndicador: " + indicator;
        SpannableString spannable = new SpannableString(text);

        // Aplicar negrita a “Estado general:”
        int startEstado = text.indexOf("Estado general:");
        int endEstado = startEstado + "Estado general:".length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startEstado, endEstado, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Aplicar negrita a “Indicador:”
        int startIndicador = text.indexOf("Indicador:");
        int endIndicador = startIndicador + "Indicador:".length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startIndicador, endIndicador, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }
    private void updateStatusCircle(String json) {
        if (json == null) return;

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        String indicator = root.getAsJsonObject("status").get("indicator").getAsString();

        int colorDrawable;
        switch (indicator.toLowerCase()) {
            case "none":        //Perfectas condiciones
                colorDrawable = R.drawable.circle_green;
                break;
            case "minor":       //Problemas menores
                colorDrawable = R.drawable.circle_yellow;
                break;
            case "major":       //Problemas graves
                colorDrawable = R.drawable.circle_red;
                break;
            default:
                colorDrawable = R.drawable.circle_gray;
                break;
        }

        Drawable drawable = ContextCompat.getDrawable(this, colorDrawable);
        statusCircle.setBackground(drawable);
    }
}
