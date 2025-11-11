package com.devst.verservidores;

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

    private EditText edtNombre, edtCorreo;
    private ImageView imgPerfil;
    private Button btnGuardar;
    private int userId;
    private Uri imagenTemporal = null;

    private static final String PREFS_NAME = "perfil_prefs";
    private static final String KEY_IMAGEN = "imagen_uri";

    private ActivityResultLauncher<Intent> seleccionarImagenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

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

        btnGuardar.setOnClickListener(v -> actualizarPerfil());
    }

    private void cargarDatosUsuario(int userId) {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getReadableDatabase();

        // Imagen por defecto
        imgPerfil.setImageResource(R.drawable.user);

        Cursor cursor = db.rawQuery(
                "SELECT nombre, correo, foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

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

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenLauncher.launch(intent);
    }

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

        if (imagenTemporal != null) {
            Uri uriInterna = guardarImagenInterna(imagenTemporal, userId);
            if (uriInterna != null) {
                cv.put("foto_perfil", uriInterna.toString());
                SharedPreferences imgPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                imgPrefs.edit().putString(KEY_IMAGEN + "_" + userId, uriInterna.toString()).apply();
            }
        }

        int rows = db.update("usuarios", cv, "id = ?", new String[]{String.valueOf(userId)});
        db.close();

        if (rows > 0) {
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri guardarImagenInterna(Uri sourceUri, int userId) {
        try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
            if (in == null) return null;

            File dir = new File(getFilesDir(), "perfil");
            if (!dir.exists()) dir.mkdirs();

            File outFile = new File(dir, "perfil_" + userId + ".jpg");
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                out.flush();
            }
            return Uri.fromFile(outFile);
        } catch (Exception e) {
            Log.e("EditarPerfilActivity", "Error guardando imagen", e);
            return null;
        }
    }
}
