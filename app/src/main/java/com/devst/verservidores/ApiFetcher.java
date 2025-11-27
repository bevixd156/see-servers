package com.devst.verservidores;

// Clase para realizar peticiones HTTP y procesar JSON
public class ApiFetcher {

    // Objetos de la clase
    private okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
    private com.google.gson.Gson gson = new com.google.gson.Gson();

    // Metodo para obtener JSON y convertirlo en un objeto
    public <R> R fetchJson(String url, Class<R> clase) throws Exception {
        // Construimos una petición GET a la URL
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();

        // Ejecutamos la petición y recibimos la respuesta
        try (okhttp3.Response response = client.newCall(request).execute()) {

            // Si la respuesta no fue exitosa, lanzamos un error
            if (!response.isSuccessful()) throw new Exception("HTTP error: " + response.code());

            // Convertimos el JSON recibido a la clase especificada
            return gson.fromJson(response.body().string(), clase);
        }
    }

    // Metodo auxiliar para obtener texto JSON sin convertirlo a objeto
    public static String getJson(String url) {
        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
            okhttp3.Response resp = client.newCall(request).execute();
            return resp.body().string();
        } catch (Exception e) {
            return null;
        }
    }
}
