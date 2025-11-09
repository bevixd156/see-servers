package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class EpicActivity extends AppCompatActivity {
    private TextView tvEpic;
    private static final String URL = "https://status.epicgames.com/api/v2/summary.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epic);
        tvEpic = findViewById(R.id.tvEpic);

        new Thread(() -> {
            String json = ApiFetcher.getJson(URL);
            final String out = parseEpic(json);
            runOnUiThread(() -> tvEpic.setText(out));
        }).start();
    }

    private String parseEpic(String json) {
        if (json == null) return "No data";
        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);
        StringBuilder sb = new StringBuilder();

        if (obj.has("status")) {
            JsonObject st = obj.getAsJsonObject("status");
            if (st.has("description")) sb.append("Overall: ").append(st.get("description").getAsString()).append("\n\n");
        }

        if (obj.has("components")) {
            for (JsonElement el : obj.getAsJsonArray("components")) {
                JsonObject comp = el.getAsJsonObject();
                String name = comp.has("name") ? comp.get("name").getAsString() : "";
                String status = comp.has("status") ? comp.get("status").getAsString() : "";
                sb.append(name).append(" : ").append(status).append("\n");
            }
        }

        return sb.toString();
    }
}
