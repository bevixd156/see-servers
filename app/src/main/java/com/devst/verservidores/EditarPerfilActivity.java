package com.devst.verservidores;
//Importamos las librerias necesarias
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri; // <-- Necesario para el callback de Firebase
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
import com.bumptech.glide.Glide; // Necesario para la vista previa de la imagen

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditarPerfilActivity extends AppCompatActivity {
    // ... [Mantener la declaración de objetos] ...
    private EditText edtNombre, edtCorreo;
    private ImageView imgPerfil;
    private Button btnGuardar;
    private int userId;
    private Uri imagenTemporal = null;
    private String fotoPerfilActual = ""; // <-- Nuevo campo para guardar la URL de Firebase actual

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

        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) finish();

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
                            // 🚀 Cargar la nueva imagen seleccionada inmediatamente en la vista previa con Glide
                            Glide.with(this).load(uri).circleCrop().into(imgPerfil);
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

        imgPerfil.setImageResource(R.drawable.user);

        Cursor cursor = db.rawQuery(
                "SELECT nombre, correo, foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (cursor.moveToFirst()) {
            edtNombre.setText(cursor.getString(0));
            edtCorreo.setText(cursor.getString(1));

            fotoPerfilActual = cursor.getString(2);
            if (fotoPerfilActual != null && !fotoPerfilActual.isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(fotoPerfilActual)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(imgPerfil);
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

    private String saveBitmapToInternalStorage(Bitmap bitmap, int userId) {
        // 1. Definir el archivo en la carpeta interna de la aplicación
        File file = new File(getFilesDir(), "profile_" + userId + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 2. Comprimir y escribir el Bitmap en el archivo
            // 90 es la calidad de compresión JPEG
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
        } catch (Exception e) {
            Log.e("EditarPerfil", "Error al guardar la imagen localmente: " + e.getMessage());
            return null; // Falló el guardado
        }

        // 3. Devolver la RUTA ABSOLUTA (String) que se guardará en SQLite/Firestore
        return file.getAbsolutePath();
    }

    //Función para actualizar el perfil (CORREGIDA - Implementación explícita del Listener)
    private void actualizarPerfil() {
        String nuevoNombre = edtNombre.getText().toString().trim();
        String nuevoCorreo = edtCorreo.getText().toString().trim();

        if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
            Toast.makeText(this, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usamos la ruta actual por defecto
        String rutaFinalDeFoto = fotoPerfilActual;

        if (imagenTemporal != null) {
            // 1. Hay una nueva imagen, convertir a Bitmap
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imagenTemporal);

                // 2. Guardar el Bitmap localmente y obtener la nueva ruta
                // Se asume que YA AÑADISTE el metodo saveBitmapToInternalStorage() a esta clase.
                String rutaGuardada = saveBitmapToInternalStorage(bitmap, userId);

                if (rutaGuardada != null) {
                    rutaFinalDeFoto = rutaGuardada;
                    Toast.makeText(this, "Imagen guardada localmente.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Fallo al guardar la imagen localmente, usando foto anterior.", Toast.LENGTH_LONG).show();
                }

            } catch (IOException e) {
                Log.e("EditarPerfil", "Error al procesar el Bitmap de la galería: " + e.getMessage());
                Toast.makeText(this, "Error al leer imagen, usando foto anterior.", Toast.LENGTH_LONG).show();
            }
        }

        // La función ahora es síncrona. Llamamos directamente con la ruta final.
        guardarDatosLocalesYRemotos(rutaFinalDeFoto);
    }

    // NUEVO METODO CENTRALIZADO PARA GUARDAR (SÍNCRONO)
    // Dentro de EditarPerfilActivity.java

    private void guardarDatosLocalesYRemotos(String fotoFinal) {
        String nuevoNombre = edtNombre.getText().toString().trim();
        String nuevoCorreo = edtCorreo.getText().toString().trim();

        // 1. ACTUALIZAR SQLITE
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nuevoNombre);
        cv.put("correo", nuevoCorreo);
        cv.put("foto_perfil", fotoFinal); // <-- URL de Firebase o URL antigua

        int rows = db.update("usuarios", cv, "id = ?", new String[]{String.valueOf(userId)});
        db.close();

        if (rows > 0) {
            // 2. OBTENER FECHA DE REGISTRO
            String fechaRegistro = obtenerFechaRegistro(userId);

            // 3. ACTUALIZAR FIRESTORE
            FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();

            // 🛑 CORRECCIÓN: Usar el nombre simple de la clase Usuario (sin el paquete)
            Usuario usuario = new Usuario(
                    nuevoNombre,
                    nuevoCorreo,
                    fechaRegistro,
                    fotoFinal
            );
            firebaseRepo.actualizarUsuario(userId, usuario);

            Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar la base de datos local.", Toast.LENGTH_SHORT).show();
        }
    }

    // Metodo auxiliar para obtener la fecha_registro de SQLite
    private String obtenerFechaRegistro(int userId) {
        String fechaRegistro = "";
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase dbRead = admin.getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT fecha_registro FROM usuarios WHERE id = ?", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            fechaRegistro = cursor.getString(0);
        }
        cursor.close();
        dbRead.close();
        return fechaRegistro;
    }
}