package com.devst.verservidores.db;

// Importamos librerías necesarias para manejar SQLite y contenido de la base de datos
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {

    // Nombre y versión de la base de datos
    public static final String DB_NAME = "AppDB.db";
    public static final int DB_VERSION = 1;

    /**
     * Constructor del helper que gestiona la base de datos SQLite.
     * Se utiliza para crear, abrir y actualizar la base de datos interna.
     */
    public AdminSQLiteOpenHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Metodo ejecutado solo la primera vez que se crea la base de datos.
     * Aquí definimos las tablas necesarias para la app.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Tabla de usuarios
        db.execSQL("CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT," +
                "correo TEXT UNIQUE," +
                "password TEXT," +
                "fecha_registro TEXT," +
                "foto_perfil TEXT)");

        // Tabla de comentarios con relación al usuario
        db.execSQL("CREATE TABLE comentarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "comentario TEXT," +
                "tipo TEXT," +
                "fecha TEXT," +
                "FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE)");
    }

    /**
     * Metodo ejecutado cuando se necesita actualizar la base de datos.
     * En esta versión simplemente elimina las tablas y las recrea.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        db.execSQL("DROP TABLE IF EXISTS comentarios");
        onCreate(db);
    }

    /**
     * Inserta un comentario perteneciente a un usuario específico.
     * Retorna el ID insertado.
     */
    public long insertComment(int userId, String mensaje, String tipo, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("comentario", mensaje);
        values.put("tipo", tipo);
        values.put("fecha", fecha);

        return db.insert("comentarios", null, values);
    }

    /**
     * Obtiene todos los comentarios de un tipo específico (ej: "discord").
     * Retorna un Cursor para recorrer los datos.
     */
    public Cursor getComments(String tipo) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT c.id, c.user_id, c.comentario, c.fecha, u.nombre, u.foto_perfil " +
                        "FROM comentarios c " +
                        "INNER JOIN usuarios u ON c.user_id = u.id " +
                        "WHERE c.tipo = ? " +
                        "ORDER BY c.id ASC",
                new String[]{tipo}
        );
    }

    /**
     * Activa las llaves foráneas en SQLite para permitir ON DELETE CASCADE.
     * Esto asegura que si se borra un usuario, sus comentarios desaparecen automáticamente.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
