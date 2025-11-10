package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class EpicActivity extends AppCompatActivity {

    private LinearLayout servicesContainer;
    private static final String URL = "https://status.epicgames.com/api/v2/summary.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epic);

        servicesContainer = findViewById(R.id.servicesContainer);
        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            runOnUiThread(() -> populateServices(json));
        }).start();
    }

    // Aquí va tu metodo createServiceBlock
    private LinearLayout createServiceBlock(String name, String status) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.HORIZONTAL);
        block.setGravity(Gravity.CENTER_VERTICAL);
        block.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        block.setPadding(0, 8, 0, 8);

        // TextView con nombre y estado
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        tv.setTextSize(22);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setText(name + " : " + status);

        // Círculo de estado
        View circle = new View(this);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(40, 40);
        circleParams.setMarginStart(8);
        circle.setLayoutParams(circleParams);

        // Elegir color según status
        int drawable;
        switch (status.toLowerCase()) {
            case "operational":
                drawable = R.drawable.circle_green;
                break;
            case "degraded":
                drawable = R.drawable.circle_yellow;
                break;
            case "outage":
                drawable = R.drawable.circle_red;
                break;
            default:
                drawable = R.drawable.circle_gray;
                break;
        }
        circle.setBackground(ContextCompat.getDrawable(this, drawable));

        block.addView(tv);
        block.addView(circle);

        return block;
    }

    // Metodo para recorrer JSON y agregar los bloques
    private void populateServices(String json) {
        if (json == null) return;

        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);

        servicesContainer.removeAllViews();

        if (obj.has("components")) {
            for (JsonElement el : obj.getAsJsonArray("components")) {
                JsonObject comp = el.getAsJsonObject();
                String name = comp.has("name") ? comp.get("name").getAsString() : "Desconocido";
                String status = comp.has("status") ? comp.get("status").getAsString() : "unknown";

                servicesContainer.addView(createServiceBlock(name, status));
            }
        }
    }
}
