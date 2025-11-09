package com.devst.verservidores;

//Importamos las librerias
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;

//Inicializamos la clase
public class ApiFetcher {
    // Instanciamos OkHttp para las peticiones hacia los servicios web
    private OkHttpClient client = new OkHttpClient();
    // Instanciamos Gson para convertir objetos Java hacia un formato JSON
    private Gson gson = new Gson();

    //Metodo fetchJson para devolver cualquier tipo de objeto
    public <R> R fetchJson(String url, Class<R> clase) throws Exception {
        //Petición GET hacia la url
        Request request = new Request.Builder().url(url).build();
        //Enviar peticion hacia el servidor y recibir una respuesta
        try (Response response = client.newCall(request).execute()){

            if (!response.isSuccessful()) throw new Exception("HTTP error: " + response.code());
            return gson.fromJson(response.body().string(), clase);
        }
    }
    // Metodo adicional: si solo quieres obtener texto JSON sin convertirlo
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
