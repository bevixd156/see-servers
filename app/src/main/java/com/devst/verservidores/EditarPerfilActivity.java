package com.devst.verservidores;
//Imporatmos las librerias necesarias
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.verservidores.db.AdminSQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EditarPerfilActivity extends AppCompatActivity {
    //Instanciamos los objetos de la clase Java
    private EditText edtNombre, edtCorreo;
    private ImageView imgPerfil;
    private Button btnGuardar;
    private int userId;
    private Uri imagenTemporal = null;
    //SharedPreferences para traer el perfil y la imagen del usuario
    private static final String PREFS_NAME = "perfil_prefs";
    private static final String KEY_IMAGEN = "imagen_uri";
    //Abrir la galeria
    private ActivityResultLauncher<Intent> seleccionarImagenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);
        //Referencia a los elementos del layout
        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnGuardar = findViewById(R.id.btnGuardar);
        //ID del usuario desde la clase PerfilActivity
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) finish();
        //Cargar datos del usuario
        cargarDatosUsuario(userId);

        // ActivityResult para seleccionar imagen
        seleccionarImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    //Si el usuario selecciono una imagen
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            imagenTemporal = uri;
                            imgPerfil.setImageURI(uri);
                        }
                    }
                }
        );

        // Abrir galería al hacer click
        imgPerfil.setOnClickListener(v -> abrirGaleria());
        // Guardar cambios
        btnGuardar.setOnClickListener(v -> actualizarPerfil());
    }
    //Función cargar los datos del usuario
    private void cargarDatosUsuario(int userId) {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getReadableDatabase();

        // Imagen por defecto
        imgPerfil.setImageResource(R.drawable.user);

        //Consulta para traer el nombre, corre y foto de perfil del usuario
        Cursor cursor = db.rawQuery(
                "SELECT nombre, correo, foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        //Si existen los registros
        if (cursor.moveToFirst()) {
            edtNombre.setText(cursor.getString(0));
            edtCorreo.setText(cursor.getString(1));
            String foto = cursor.getString(2);
            if (foto != null && !foto.isEmpty()) {
                imgPerfil.setImageURI(Uri.parse(foto));
            }
        }

        cursor.close();
        db.close();
    }
    //Función abrir galeria
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenLauncher.launch(intent);
    }
    //Función para actualizar el perfil
    private void actualizarPerfil() {
        String nuevoNombre = edtNombre.getText().toString().trim();
        String nuevoCorreo = edtCorreo.getText().toString().trim();
        //Validamos si es que el nombre y el correo estan vacios
        if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
            Toast.makeText(this, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }
        //Abrir DB para modificar el contenido
        SQLiteDatabase db = new AdminSQLiteOpenHelper(this).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nuevoNombre);
        cv.put("correo", nuevoCorreo);

        //Si el usuario selecciono una imagen
        if (imagenTemporal != null) {
            Uri uriInterna = guardarImagenInterna(imagenTemporal, userId);
            if (uriInterna != null) {
                //Actualiza la foto de perfil
                cv.put("foto_perfil", uriInterna.toString());
                //Guardamos la nueva imagen el SharedPreferences
                SharedPreferences imgPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                imgPrefs.edit().putString(KEY_IMAGEN + "_" + userId, uriInterna.toString()).apply();
            }
        }
        // Update para el usuario
        int rows = db.update("usuarios", cv, "id = ?", new String[]{String.valueOf(userId)});
        db.close();
        //Si funciona
        if (rows > 0) {
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            //Si no funciona
            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
        }
    }
    // Funcion Guardar Imagen en la memoria interna de la app
    private Uri guardarImagenInterna(Uri sourceUri, int userId) {
        //Intenta leer la imagen seleccionada de la galeria
        try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
            //Si no es posible leer la imagen retorna null
            if (in == null) return null;
            //Crea un dir llamado perfil
            File dir = new File(getFilesDir(), "perfil");
            //Si el dir no existe se crea el dir
            if (!dir.exists()) dir.mkdirs();
            //Sobreescribe para definir en donde quedará guardada la foto
            File outFile = new File(dir, "perfil_" + userId + ".jpg");
            //Intenta un OutPutStream para escribir los bytes correspondientes
            try (OutputStream out = new FileOutputStream(outFile)) {
                //Crear
                byte[] buffer = new byte[1024];
                //Leer
                int read;
                //Escribir
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                //Guardamos
                out.flush();
            }
            //Devolver la ruta del archivo
            return Uri.fromFile(outFile);
        } catch (Exception e) {
            //Si hay un error se captura un registro del error
            Log.e("EditarPerfilActivity", "Error guardando imagen", e);
            //Retornar null para indicar el fallo
            return null;
        }
    }
}
