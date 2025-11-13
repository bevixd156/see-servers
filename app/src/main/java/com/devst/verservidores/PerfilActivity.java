package com.devst.verservidores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.devst.verservidores.db.AdminSQLiteOpenHelper;

import java.io.File;

public class PerfilActivity extends AppCompatActivity {

    private ImageView imgPerfil;
    private TextView txtNombre, txtCorreo, txtFechaRegistro;
    private Button btnEditarPerfil;

    //Variable para que traiga los datos del usuario
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        imgPerfil = findViewById(R.id.imgPerfil);
        txtNombre = findViewById(R.id.txtNombrePerfil);
        txtCorreo = findViewById(R.id.txtCorreoPerfil);
        txtFechaRegistro = findViewById(R.id.txtFechaRegistro);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);

        // Recuperar ID del usuario
        SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) return;

        // Cargar datos
        cargarDatosUsuario(userId);

        // Botón para ir a editar el perfil
        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, EditarPerfilActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }

    //Función para que se carguen los datos del usuario
    private void cargarDatosUsuario(int userId) {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT nombre, correo, fecha_registro, foto_perfil FROM usuarios WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        // Imagen por defecto
        imgPerfil.setImageResource(R.drawable.user);

        if (cursor.moveToFirst()) {
            txtNombre.setText(cursor.getString(0));
            txtCorreo.setText(cursor.getString(1));

            // Mostrar fecha formateada
            String fecha = cursor.getString(2);
            if (fecha != null && !fecha.isEmpty()) {
                txtFechaRegistro.setText("Se unió el: " + fecha);
            } else {
                txtFechaRegistro.setText("Fecha no disponible");
            }

            // Carga de imagend e perfil si existe
            String foto = cursor.getString(3);
            if (foto != null && !foto.isEmpty()) {
                File file = new File(Uri.parse(foto).getPath());
                if (file.exists()) {
                    imgPerfil.setImageURI(Uri.fromFile(file));
                }
            }
        }

        cursor.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar datos al volver de edición
        cargarDatosUsuario(userId);
    }
}
