package com.devst.verservidores.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {
    /** * Constructor de la clase AdminSQLiteOpenHelper. *
     * @param context El contexto de la aplicación.
     * @param name El nombre del archivo de la base de datos (ej: "mi_base_de_datos.db").
     * @param factory Se usa para crear objetos Cursor (normalmente es null).
     * @param version La versión actual de la base de datos (para controlar actualizaciones). */
    // Constructor
    public static final String DB_NAME = "AppDB.db";
    public static final int DB_VERSION = 1;
    public AdminSQLiteOpenHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    // Se ejecuta para crear las tablas por primera vez
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creamos la tabla de usuarios
        db.execSQL("CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT," +
                "correo TEXT UNIQUE," +
                "password TEXT," +
                "fecha_registro TEXT," +
                "foto_perfil TEXT)");

        db.execSQL("CREATE TABLE comentarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "comentario TEXT," +
                "tipo TEXT," +
                "fecha TEXT," +
                "FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE)");
        // Podriamos insertar datos de pruebas
        // db.execSQL("INSERT INTO usuarios (nombre, correo, password) VALUES ('Felipe', 'felipe123@gmail.com', '123456')");
    }
    // Se ejecuta cuando se necesita actualizar la estructura de la BD
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // En un caso simple, podemos solo borrar la tabla y recrearla
        // En un caso real, esto requeriría una migración de datos
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        db.execSQL("DROP TABLE IF EXISTS comentarios");
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Habilita el uso de claves foráneas en SQLite.
        // SQLite las soporta, pero no vienen activadas por defecto.
        // Esto permite que ON DELETE CASCADE y otras reglas funcionen correctamente.
        db.setForeignKeyConstraintsEnabled(true);
    }
}