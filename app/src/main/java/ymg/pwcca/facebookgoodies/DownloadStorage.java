package ymg.pwcca.facebookgoodies;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class DownloadStorage {
    static void publish(Context app, File file, DownloadTransfer transfer) throws IOException {
        String name = "FacebookGoodies_" + System.currentTimeMillis() + ".mp4";
        if (Build.VERSION.SDK_INT >= 29) publishMediaStore(app, file, name, transfer);
        else publishLegacy(app, file, name, transfer);
    }
    @android.annotation.TargetApi(29)
    private static void publishMediaStore(Context app, File file, String name, DownloadTransfer transfer) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FacebookGoodies");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = app.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new DownloadFailure("Cannot create the download file");
        boolean saved = false;
        try {
            try (OutputStream out = app.getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new DownloadFailure("Cannot open the download file");
                copy(file, out, transfer);
            }
            transfer.check();
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (app.getContentResolver().update(uri, values, null, null) != 1) throw new DownloadFailure("Cannot finish the download file");
            saved = true;
        } finally { if (!saved) app.getContentResolver().delete(uri, null, null); }
    }
    private static void publishLegacy(Context app, File file, String name, DownloadTransfer transfer) throws IOException {
        if (app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            throw new DownloadFailure("Allow Facebook storage access to save videos on Android 9");
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FacebookGoodies");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new DownloadFailure("Cannot create Downloads/FacebookGoodies");
        File output = new File(directory, name);
        boolean saved = false;
        try {
            try (OutputStream out = new FileOutputStream(output)) { copy(file, out, transfer); }
            transfer.check();
            MediaScannerConnection.scanFile(app, new String[]{output.getAbsolutePath()}, new String[]{"video/mp4"}, null);
            saved = true;
        } finally { if (!saved) output.delete(); }
    }
    private static void copy(File file, OutputStream out, DownloadTransfer transfer) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) { transfer.check(); out.write(buffer, 0, count); }
        }
    }
}
