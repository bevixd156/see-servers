package com.devst.verservidores.api;

// Importaciones necesarias
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;

public class ApiFetcher {

    // Cliente HTTP OkHttp para realizar las peticiones
    private OkHttpClient client = new OkHttpClient();

    // Objeto Gson para parsear JSON
    private Gson gson = new Gson();

    // Método genérico para obtener JSON desde una URL y mapearlo a una clase Java
    public <R> R fetchJson(String url, Class<R> clase) throws Exception {

        // Construcción de la petición HTTP
        Request request = new Request.Builder()
                .url(url)
                // Obligatorio para algunos endpoints como Xbox
                .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Encoding", "identity")
                .build();

        // Ejecutar la petición
        try (Response response = client.newCall(request).execute()) {

            // Verificar éxito de la respuesta
            if (!response.isSuccessful()) {
                throw new Exception("HTTP error: " + response.code());
            }

            // Parsear el cuerpo JSON a la clase indicada
            return gson.fromJson(response.body().string(), clase);
        }
    }

    // Obtener JSON como String usando OkHttp
    // Útil para endpoints donde no necesitamos mapear a clase
    public static String getJson(String urlString) {
        try {
            OkHttpClient client = new OkHttpClient();

            // Construcción de la petición
            Request request = new Request.Builder()
                    .url(urlString)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "identity")
                    .build();

            // Ejecutar la petición
            Response response = client.newCall(request).execute();

            // Verificar éxito de la respuesta
            if (!response.isSuccessful()) {
                Log.e("ApiFetcher", "HTTP ERROR -> " + response.code());
                return null;
            }

            // Obtener el cuerpo como String
            String body = response.body().string();
            Log.d("ApiFetcher", "JSON -> " + body);
            return body;

        } catch (Exception e) {
            Log.e("ApiFetcher", "ERROR -> " + e.getMessage());
            return null;
        }
    }

    // Obtener JSON como String con User-Agent personalizado
    // Útil cuando algunos servidores requieren un User-Agent específico
    public static String getJsonWithUserAgent(String urlString, String userAgent) {
        try {
            OkHttpClient client = new OkHttpClient();

            // Construcción de la petición con User-Agent personalizado
            Request request = new Request.Builder()
                    .url(urlString)
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "identity")
                    .build();

            // Ejecutar la petición
            Response response = client.newCall(request).execute();

            // Verificar éxito de la respuesta
            if (!response.isSuccessful()) return null;

            // Devolver cuerpo JSON
            return response.body().string();

        } catch (Exception e) {
            Log.e("ApiFetcher", "ERROR -> " + e.getMessage());
            return null;
        }
    }
}
