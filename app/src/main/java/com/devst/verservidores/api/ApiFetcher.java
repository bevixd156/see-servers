package com.devst.verservidores.api;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiFetcher {

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    // Metodo para obtener JSON y mapearlo a una clase
    public <R> R fetchJson(String url, Class<R> clase) throws Exception {

        Request request = new Request.Builder()
                .url(url)
                // 🔥 OBLIGATORIO PARA XBOX:
                .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Encoding", "identity")
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new Exception("HTTP error: " + response.code());
            }

            return gson.fromJson(response.body().string(), clase);
        }
    }

    // Obtener JSON como String usando OkHttp (MUCHO más estable)
    public static String getJson(String urlString) {
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(urlString)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "identity")
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                Log.e("ApiFetcher", "HTTP ERROR -> " + response.code());
                return null;
            }

            String body = response.body().string();
            Log.d("ApiFetcher", "JSON -> " + body);
            return body;

        } catch (Exception e) {
            Log.e("ApiFetcher", "ERROR -> " + e.getMessage());
            return null;
        }
    }

    public static String getJsonWithUserAgent(String urlString, String userAgent) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlString)
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "identity")
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) return null;
            return response.body().string();
        } catch (Exception e) {
            Log.e("ApiFetcher", "ERROR -> " + e.getMessage());
            return null;
        }
    }


}
