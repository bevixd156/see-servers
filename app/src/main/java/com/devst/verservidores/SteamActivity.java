package com.devst.verservidores;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SteamActivity extends AppCompatActivity {

    private TextView tvSteam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steam);

        tvSteam = findViewById(R.id.tvSteam);
    }
}
