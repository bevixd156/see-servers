package com.devst.verservidores.vista;

// Importaciones necesarias para trabajar con imágenes, EXIF y logs
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import androidx.exifinterface.media.ExifInterface;
import java.io.IOException;
import java.io.InputStream;

// Clase encargada de leer los metadatos EXIF y corregir la rotación de una imagen
public class RotacionDeImagen {

    // Metodo principal que rota un Bitmap según la orientación obtenida desde los metadatos EXIF del archivo
    public static Bitmap rotarBitmap(Context context, Bitmap bitmap, Uri uri) {
        if (uri == null) return bitmap;

        // Lectura del archivo para obtener los metadatos EXIF (try con autocierre)
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return bitmap;

            ExifInterface ei = new ExifInterface(inputStream);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            // Verificación de orientación y aplicación de rotación según corresponda
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotar(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotar(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotar(bitmap, 270);
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    // Aquí podrías manejar un volteo horizontal si lo necesitas
                    break;
                default:
                    return bitmap;
            }

            // Manejo de errores al intentar leer metadatos EXIF
        } catch (IOException e) {
            Log.e("RotacionDeImagen", "Error leyendo EXIF: " + e.getMessage());
        }

        return bitmap;
    }

    // Metodo auxiliar que aplica la rotación al Bitmap usando Matrix
    private static Bitmap rotar(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true
        );
    }
}
