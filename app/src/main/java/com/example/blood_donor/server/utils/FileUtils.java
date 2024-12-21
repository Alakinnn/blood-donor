package com.example.blood_donor.server.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final String REPORTS_DIR = "BloodDonor/Reports";

    public static void saveAndShareReport(Context context, byte[] reportData, String eventId, String format) {
        try {
            File reportFile;

            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

            // Convert EXCEL format to xlsx extension
            String extension = format.equalsIgnoreCase("EXCEL") ? "xlsx" : format.toLowerCase();
            String filename = String.format("report_%s_%s.%s", eventId, timestamp, extension);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use app-specific directory
                File appDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), REPORTS_DIR);
                if (!appDir.exists() && !appDir.mkdirs()) {
                    throw new IOException("Failed to create reports directory");
                }
                reportFile = new File(appDir, filename);
            } else {
                // For older versions, use public directory
                File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File reportsDir = new File(documentsDir, REPORTS_DIR);
                if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                    throw new IOException("Failed to create reports directory");
                }
                reportFile = new File(reportsDir, filename);
            }

            // Write the data
            try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                fos.write(reportData);
                fos.flush();
            }

            // Show success message
            String displayPath = "Documents/" + REPORTS_DIR + "/" + filename;
            Toast.makeText(context,
                    "Report saved to: " + displayPath,
                    Toast.LENGTH_LONG).show();

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider",
                    reportFile);

            shareIntent.setType(getMimeType(format));
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start share activity
            context.startActivity(Intent.createChooser(shareIntent, "Share Report"));

            // Log the file path for debugging
            Log.d(TAG, "Report saved at: " + reportFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "Error saving/sharing report", e);
            Toast.makeText(context,
                    "Error saving report: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static String getMimeType(String format) {
        switch (format.toUpperCase()) {
            case "PDF":
                return "application/pdf";
            case "EXCEL":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV":
                return "text/csv";
            default:
                return "application/octet-stream";
        }
    }
}