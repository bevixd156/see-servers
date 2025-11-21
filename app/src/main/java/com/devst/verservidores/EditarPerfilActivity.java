package com.devst.verservidores;
//Importamos las librerias necesarias
import android.content.ContentValues;
import android.content.Intent;
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
import com.devst.verservidores.repositorio.FirebaseRepositorio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EditarPerfilActivity extends AppCompatActivity {
    // ... [Mantener la declaración de objetos] ...

    private EditText edtNombre, edtCorreo;
    private ImageView imgPerfil;
    private Button btnGuardar;
    private int userId;
    private Uri imagenTemporal = null;
    private String fotoPerfilActual = ""; // <-- Nuevo campo para guardar la URI actual
    private static final String PREFS_NAME = "perfil_prefs";
    private static final String KEY_IMAGEN = "imagen_uri";
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

    //Función cargar los datos del usuario (CORREGIDA)
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
            // Guardamos la foto actual en la variable de clase
            fotoPerfilActual = cursor.getString(2); // <-- ALMACENAR FOTO ACTUAL

            if (fotoPerfilActual != null && !fotoPerfilActual.isEmpty()) {
                imgPerfil.setImageURI(Uri.parse(fotoPerfilActual));
            }
        }

        cursor.close();
        db.close();
    }

    //Función abrir galeria (MANTENER CÓDIGO ORIGINAL)
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenLauncher.launch(intent);
    }

    //Función para actualizar el perfil (CORREGIDA)
    private void actualizarPerfil() {
        String nuevoNombre = edtNombre.getText().toString().trim();
        String nuevoCorreo = edtCorreo.getText().toString().trim();
        if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
            Toast.makeText(this, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = new AdminSQLiteOpenHelper(this).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nuevoNombre);
        cv.put("correo", nuevoCorreo);

        String fotoFinal = fotoPerfilActual; // <-- Inicia con la foto que tenía el usuario

        if (imagenTemporal != null) {
            // Si el usuario seleccionó una nueva imagen, la guardamos
            Uri uriInterna = guardarImagenInterna(imagenTemporal, userId);
            if (uriInterna != null) {
                // Actualizamos la URI de la foto en SQL
                cv.put("foto_perfil", uriInterna.toString());
                // Actualizamos la foto final para Firebase
                fotoFinal = uriInterna.toString();
            }
        }

        // El resto de la lógica de actualización en SQLite se mantiene igual
        int rows = db.update("usuarios", cv, "id = ?", new String[]{String.valueOf(userId)});
        db.close();

        if (rows > 0) {
            // Obtener fecha de registro actual de SQLite (MANTENER CÓDIGO ORIGINAL)
            String fechaRegistro = "";
            SQLiteDatabase dbRead = new AdminSQLiteOpenHelper(this).getReadableDatabase();
            Cursor cursor = dbRead.rawQuery("SELECT fecha_registro FROM usuarios WHERE id = ?", new String[]{String.valueOf(userId)});
            if (cursor.moveToFirst()) {
                fechaRegistro = cursor.getString(0);
            }
            cursor.close();
            dbRead.close();

            // Actualizar Firebase (USANDO LA VARIABLE fotoFinal)
            FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();
            com.devst.verservidores.Usuario usuario = new com.devst.verservidores.Usuario(
                    nuevoNombre,
                    nuevoCorreo,
                    fechaRegistro,
                    fotoFinal // <-- Usar fotoFinal que contiene la URI antigua o la URI nueva
            );
            firebaseRepo.actualizarUsuario(userId, usuario);

            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    // Funcion Guardar Imagen en la memoria interna de la app (MANTENER CÓDIGO ORIGINAL)
    private Uri guardarImagenInterna(Uri sourceUri, int userId) {
        // ...
        try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
            // ...
            File dir = new File(getFilesDir(), "perfil");
            // ...
            File outFile = new File(dir, "perfil_" + userId + ".jpg");
            // ...
            try (OutputStream out = new FileOutputStream(outFile)) {
                // ...
            }
            return Uri.fromFile(outFile);
        } catch (Exception e) {
            Log.e("EditarPerfilActivity", "Error guardando imagen", e);
            return null;
        }
    }
}