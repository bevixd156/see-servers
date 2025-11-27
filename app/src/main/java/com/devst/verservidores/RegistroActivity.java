package com.devst.verservidores;
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

import com.devst.verservidores.db.AdminSQLiteOpenHelper;
import com.devst.verservidores.repositorio.FirebaseRepositorio;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroActivity extends AppCompatActivity {
    //Objetos de la clase
    private EditText edtNombre, edtCorreo, edtPass, edtPass2;
    private Button btnCrearCuenta;
    private TextView btnVolverLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // Referencias UI
        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        edtPass = findViewById(R.id.edtPass);
        edtPass2 = findViewById(R.id.edtPass2);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);

        // Crear cuenta
        btnCrearCuenta.setOnClickListener(v -> registrarUsuario());

        // Volver al login
        btnVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = edtNombre.getText().toString().trim();
        String correo = edtCorreo.getText().toString().trim();
        String pass1 = edtPass.getText().toString().trim();
        String pass2 = edtPass2.getText().toString().trim();

        // Validar campos vacíos
        if (nombre.isEmpty() || correo.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Correo no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar largo contraseña
        if (pass1.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar coincidencia
        if (!pass1.equals(pass2)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Abrir base de datos
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getWritableDatabase();

        // Verificar si el correo ya existe
        Cursor cursor = db.rawQuery("SELECT id FROM usuarios WHERE correo = ?", new String[]{correo});
        if (cursor.moveToFirst()) {
            Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        // Fecha de registro en formato legible
        String fechaRegistro = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Datos para insertar
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("correo", correo);
        values.put("password", pass1);
        values.put("fecha_registro", fechaRegistro);
        values.put("foto_perfil", ""); // sin foto al crear

        // Insertar usuario
        long resultado = db.insert("usuarios", null, values);
        db.close();

        // Verificar resultado
        if (resultado > 0) {
            // Enviar usuario a Firebase
            Usuario usuario = new Usuario(nombre, correo, fechaRegistro, "");
            FirebaseRepositorio firebaseRepo = new FirebaseRepositorio();
            firebaseRepo.agregarUsuario((int) resultado, usuario); // subir usando el ID SQLite

            Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();

            // Volver al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show();
        }
    }
}
