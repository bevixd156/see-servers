package com.devst.verservidores.vista;

//Importamos las librerías necesarias
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.verservidores.R;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;
import com.bumptech.glide.Glide;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

//Clase auxiliar para rotar imágenes


public class EditarPerfilActivity extends AppCompatActivity {

    //Objetos de la UI
    private EditText edtNombre, edtCorreo;
    private ImageView imgPerfil;
    private Button btnGuardar;

    //Variables del usuario
    private int userId;
    private Uri imagenTemporal = null;
    private String fotoPerfilActual = "";

    //Launcher para seleccionar imagen
    private ActivityResultLauncher<Intent> seleccionarImagenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        //Referencias UI
        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnGuardar = findViewById(R.id.btnGuardar);

        //Obtener ID usuario recibido
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) finish();

        //Cargar datos del usuario
        cargarDatosUsuario(userId);

        //Launcher para abrir galería
        seleccionarImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            imagenTemporal = uri;
                            Glide.with(this).load(uri).circleCrop().into(imgPerfil);
                        }
                    }
                }
        );

        //Click abrir galería
        imgPerfil.setOnClickListener(v -> abrirGaleria());

        //Click guardar perfil
        btnGuardar.setOnClickListener(v -> actualizarPerfil());
    }

    //Cargar datos del usuario desde SQLite
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
                try {
                    byte[] imageBytes = Base64.decode(fotoPerfilActual, Base64.DEFAULT);
                    Glide.with(this)
                            .load(imageBytes)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .circleCrop()
                            .into(imgPerfil);

                } catch (IllegalArgumentException e) {
                    Log.e("EditarPerfil", "Error Base64: " + e.getMessage());
                    imgPerfil.setImageResource(R.drawable.user);
                }
            }
        }

        cursor.close();
        db.close();
    }

    //Abrir galería
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenLauncher.launch(intent);
    }

    //Codificar bitmap en Base64
    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, false);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] byteArray = baos.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (Exception e) {
            Log.e("Base64Encode", "Error al codificar imagen: " + e.getMessage());
            return null;
        }
    }

    //Actualizar perfil (con rotación EXIF)
    private void actualizarPerfil() {
        String nuevoNombre = edtNombre.getText().toString().trim();
        String nuevoCorreo = edtCorreo.getText().toString().trim();

        //Validación simple
        if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
            Toast.makeText(this, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        String fotoFinal = fotoPerfilActual;

        //Procesar nueva imagen
        if (imagenTemporal != null) {
            try {
                Bitmap bitmapOriginal = MediaStore.Images.Media.getBitmap(getContentResolver(), imagenTemporal);

                Bitmap rotatedBitmap = RotacionDeImagen.rotarBitmap(this, bitmapOriginal, imagenTemporal);

                String base64String = encodeBitmapToBase64(rotatedBitmap);

                if (base64String != null && !base64String.isEmpty()) {
                    fotoFinal = base64String;
                    Toast.makeText(this, "Imagen lista para guardar.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al codificar imagen.", Toast.LENGTH_LONG).show();
                }

            } catch (IOException e) {
                Log.e("EditarPerfil", "Error Bitmap: " + e.getMessage());
                Toast.makeText(this, "Error al leer imagen.", Toast.LENGTH_LONG).show();
            }
        }

        //Guardar datos en SQLite y Firestore
        guardarDatosLocalesYRemotos(fotoFinal);
    }

    //Guardar cambios en SQLite y Firestore
    private void guardarDatosLocalesYRemotos(String fotoFinal) {
        String nuevoNombre = edtNombre.getText().toString().trim();
        String nuevoCorreo = edtCorreo.getText().toString().trim();

        //Actualizar SQLite
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("nombre", nuevoNombre);
        cv.put("correo", nuevoCorreo);
        cv.put("foto_perfil", fotoFinal);

        int rows = db.update("usuarios", cv, "id = ?", new String[]{String.valueOf(userId)});
        db.close();

        if (rows > 0) {

            //Obtener fecha de registro para Firestore
            String fechaRegistro = obtenerFechaRegistro(userId);

            //Actualizar Firestore
            FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();

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
            Toast.makeText(this, "Error al actualizar datos locales.", Toast.LENGTH_SHORT).show();
        }
    }

    //Obtener fecha de registro desde SQLite
    private String obtenerFechaRegistro(int userId) {
        String fechaRegistro = "";
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase dbRead = admin.getReadableDatabase();

        Cursor cursor = dbRead.rawQuery(
                "SELECT fecha_registro FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (cursor.moveToFirst()) fechaRegistro = cursor.getString(0);

        cursor.close();
        dbRead.close();
        return fechaRegistro;
    }
}
