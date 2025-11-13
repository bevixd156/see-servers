package com.devst.verservidores;

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

public class RegistroActivity extends AppCompatActivity {

    EditText edtNombre, edtCorreo, edtPass, edtPass2;
    Button btnCrearCuenta;
    TextView btnVolverLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        edtPass = findViewById(R.id.edtPass);
        edtPass2 = findViewById(R.id.edtPass2);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);

        btnCrearCuenta.setOnClickListener(v -> registrarUsuario());
        btnVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {

        String nombre = edtNombre.getText().toString().trim();
        String correo = edtCorreo.getText().toString().trim();
        String pass1 = edtPass.getText().toString();
        String pass2 = edtPass2.getText().toString();

        if (nombre.isEmpty() || correo.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass1.equals(pass2)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this);
        SQLiteDatabase db = admin.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT id FROM usuarios WHERE correo = ?", new String[]{correo});
        if (cursor.moveToFirst()) {
            Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("correo", correo);
        values.put("password", pass1);
        values.put("fecha_registro", System.currentTimeMillis() + "");
        values.put("foto_perfil", "");

        long resultado = db.insert("usuarios", null, values);
        db.close();

        if (resultado > 0) {
            Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show();
        }
    }
}
