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
import com.google.firebase.FirebaseApp;

public class LoginActivity extends AppCompatActivity {

    //Objetos de la clase
    private EditText edtEmail, edtPass;
    private Button btnLogin;
    private TextView tvCrear;
    private AdminSQLiteOpenHelper adminDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login);

        adminDB = new AdminSQLiteOpenHelper(this);

        // // Referencias UI
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvCrear = findViewById(R.id.tvCrear);

        // // Mostrar mensaje si la cuenta fue eliminada
        if (getIntent().getBooleanExtra("deleted", false)) {
            Toast.makeText(this, "Cuenta eliminada con éxito", Toast.LENGTH_LONG).show();
        }

        // // Botón para iniciar sesión
        btnLogin.setOnClickListener(v -> attemptLogin());

        // // Abrir registro
        tvCrear.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(i);
        });
    }

    // // Intento de inicio de sesión
    private void attemptLogin() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        // // Validación simple
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = adminDB.getReadableDatabase();

        // // Buscar usuario en SQLite
        Cursor cursor = db.rawQuery(
                "SELECT id, password, nombre FROM usuarios WHERE correo = ?",
                new String[]{email}
        );

        if (cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            String storedPass = cursor.getString(1);
            String nombre = cursor.getString(2);

            // // Comparar contraseñas
            if (storedPass != null && storedPass.equals(pass)) {

                // // Guardar sesión en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("USER_PREFS", MODE_PRIVATE);
                prefs.edit()
                        .putInt("user_id", id)
                        .putString("user_name", nombre)
                        .apply();

                // // Ir a HomeActivity
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
