package com.devst.verservidores;

//Importamos las librerias necesarias
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;

//Inicializamos la clase para las peticiones HTTP y procesar JSON
public class ApiFetcher {
    //Cliente HTTP que se reutiliza para todas las peticiones
    private OkHttpClient client = new OkHttpClient();
    //Gson permite convertir JSON ⇄ objetos Java
    private Gson gson = new Gson();

    //Metodo fetchJson para devolver cualquier tipo de objeto
    //y convertirlo automáticamente a un objeto de la clase indicada.
    public <R> R fetchJson(String url, Class<R> clase) throws Exception {
        //Construimos una petición GET a la URL
        Request request = new Request.Builder().url(url).build();
        //Ejecutamos la petición y recibimos la respuesta
        try (Response response = client.newCall(request).execute()){
            // Si la respuesta no fue exitosa, lanzamos un error
            if (!response.isSuccessful()) throw new Exception("HTTP error: " + response.code());
            // Convertimos el JSON recibido a la clase especificada
            return gson.fromJson(response.body().string(), clase);
        }
    }
    //Metodo auxiliar: obtiene solo el texto JSON desde una URL.
    //No convierte a objetos, solo devuelve el texto tal cual.
    public static String getJson(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response resp = client.newCall(request).execute();
            return resp.body().string();
        } catch (Exception e) {
            return null;
        }
    }
}
