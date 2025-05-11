package org.maximandroid.cas;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.Manifest;
import android.util.Log;
import android.widget.Toast;

public class StorageUtil {

    public static final int REQUEST_CODE_READ_PERMISSION = 1;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 2;

    public static final int FILE_SELECT_CODE = 4;
    public static final int MAXIMA_INSTALL_CODE = 5;

    @SuppressLint("StaticFieldLeak")
    protected static StorageUtil instance;

    private final Activity activity;
    private final File internalDir;

    public StorageUtil(Activity activity) {
        this.activity = activity;
        this.internalDir = activity.getFilesDir();
    }

    public static boolean createMaximaAndroidDir() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return false;
        }

        File sdcardDir = Environment.getExternalStorageDirectory();
        File maximaDir = new File(sdcardDir, "maxima-android");
        if (maximaDir.exists()) {
            return maximaDir.isDirectory();
        }
        return maximaDir.mkdirs();
    }

    public static boolean checkAndCreateWithPermission() {
        // Android 6.0
        if (Build.VERSION.SDK_INT < 23) {
            return createMaximaAndroidDir();
        }

        if (instance.activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return createMaximaAndroidDir();
        } else {
            instance.activity.requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE_PERMISSION);
            return false;
        }
    }

    public static void checkAndOpenFileWithPermission() {
        // Android 6.0
        if (Build.VERSION.SDK_INT < 23) {
            displayFileChooser();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (instance.activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                instance.activity.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        StorageUtil.REQUEST_CODE_READ_PERMISSION);
            } else {
                displayFileChooser();
            }
        }
    }

    public static void displayFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            instance.activity.startActivityForResult(
                    Intent.createChooser(intent, "choose .json file"),
                    StorageUtil.FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(instance.activity, "File explorer not found!", Toast.LENGTH_SHORT).show();
        }
    }

    public static String timestampName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                .format(new Date());
        return timestamp + ".json";
    }

    public static String saveJson(String json, String fileName) {
        File sdcardDir = Environment.getExternalStorageDirectory();
        File maximaDir = new File(sdcardDir, "maxima-android");
        File file = new File(maximaDir, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            Log.e("MoA", "Failed to save JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return file.getAbsolutePath();
    }

    public static boolean maximaBinaryExists() {
        CpuArchitecture.initCpuArchitecture();
        String res = CpuArchitecture.getMaximaFile();
        if (res.startsWith("not")) {
            return false;
        }
        return maximaInternalFile(res).exists();
    }

    public static File maximaBinaryFile() {
        return maximaInternalFile(CpuArchitecture.getMaximaFile());
    }

    public static File maximaPackageFile() {
        return maximaInternalFile("maxima-" + Globals.maximaVersion);
    }

    public static File maximaInitFile() {
        return StorageUtil.maximaInternalFile("init.lisp");
    }

    public static File maximaInternalFile(String name) {
        return new File(instance.internalDir.getAbsolutePath() + "/" + name);
    }
}
