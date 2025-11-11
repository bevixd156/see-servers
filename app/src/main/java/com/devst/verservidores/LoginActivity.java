package com.devst.verservidores;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPass;
    private Button btnLogin;
    private TextView tvCrear;
    private AdminSQLiteOpenHelper adminDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        adminDB = new AdminSQLiteOpenHelper(this);

        // Si quieres recuperar vistas (si las eliminaste, crea los ids)
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvCrear = findViewById(R.id.tvCrear); // si es TextView con id tvCrear

        // Mostrar toast si venimos de eliminación
        if (getIntent().getBooleanExtra("deleted", false)) {
            Toast.makeText(this, "Cuenta eliminada con éxito", Toast.LENGTH_LONG).show();
        }

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvCrear.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(i);
        });
    }

    private void attemptLogin() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = adminDB.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, password, nombre FROM usuarios WHERE correo = ?", new String[]{email});
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            String storedPass = cursor.getString(1);
            String nombre = cursor.getString(2);
            if (storedPass != null && storedPass.equals(pass)) {
                // Guardar sesión (user_id y nombre) en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                prefs.edit().putInt("user_id", id).putString("user_name", nombre).apply();

                // Ir a MainActivity
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        db.close();
    }
}
