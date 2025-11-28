package com.devst.verservidores.vista;

// Importaciones necesarias
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.verservidores.R;
import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroActivity extends AppCompatActivity {
    // Objetos de la clase
    private EditText edtNombre, edtCorreo, edtPass, edtPass2; // Campos de entrada del usuario
    private Button btnCrearCuenta; // Botón para crear la cuenta
    private TextView btnVolverLogin; // Texto para volver al login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Habilita diseño edge-to-edge
        setContentView(R.layout.activity_registro); // Asocia el layout

        // Referencias a los elementos de la UI
        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        edtPass = findViewById(R.id.edtPass);
        edtPass2 = findViewById(R.id.edtPass2);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);

        // Configurar acción del botón de crear cuenta
        btnCrearCuenta.setOnClickListener(v -> registrarUsuario());

        // Configurar acción para volver al login
        btnVolverLogin.setOnClickListener(v -> finish());
    }

    // Método para registrar usuario
    private void registrarUsuario() {
        String nombre = edtNombre.getText().toString().trim();
        String correo = edtCorreo.getText().toString().trim();
        String pass1 = edtPass.getText().toString().trim();
        String pass2 = edtPass2.getText().toString().trim();

        // Validación de campos vacíos
        if (nombre.isEmpty() || correo.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación de correo válido
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Correo no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación de longitud mínima de contraseña
        if (pass1.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación de coincidencia de contraseñas
        if (!pass1.equals(pass2)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Abrir base de datos SQLite
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getWritableDatabase();

        // Verificar si el correo ya existe en la base de datos
        Cursor cursor = db.rawQuery("SELECT id FROM usuarios WHERE correo = ?", new String[]{correo});
        if (cursor.moveToFirst()) {
            Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        // Crear fecha de registro en formato legible
        String fechaRegistro = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Preparar datos para insertar en SQLite
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("correo", correo);
        values.put("password", pass1);
        values.put("fecha_registro", fechaRegistro);
        values.put("foto_perfil", ""); // Sin foto inicial

        // Insertar usuario en SQLite
        long resultado = db.insert("usuarios", null, values);
        db.close();

        // Verificar si la inserción fue exitosa
        if (resultado > 0) {
            // Crear objeto Usuario para Firebase
            Usuario usuario = new Usuario(nombre, correo, fechaRegistro, "");
            FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();
            firebaseRepo.agregarUsuario((int) resultado, usuario); // Subir usuario a Firebase usando el ID SQLite

            Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();

            // Redirigir a la pantalla de login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // Mostrar error si la inserción falla
            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show();
        }
    }
}
