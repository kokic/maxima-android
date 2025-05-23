package org.maximandroid.cas;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public final class UnzipAsyncTask extends AsyncTask<Integer, Integer, Integer> {
    InputStream inst;
    String directory;
    @SuppressLint("StaticFieldLeak")
    private final Activity activity;
    private ProgressDialog dialog;
    private String msg1, msg2;

    public UnzipAsyncTask(Activity anActivity) {
        this.activity = anActivity;
    }

    public void setParams(InputStream in, String dir, String msg1, String msg2) {
        inst = in;
        directory = dir;
        this.msg1 = msg1;
        this.msg2 = msg2;
    }

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(activity);
        dialog.setTitle(R.string.install_in_progress);
        dialog.setMessage(msg1);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMax(100);
        dialog.setProgress(0);

        dialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... arg) {
        dialog.setProgress(arg[0]);
    }

    @Override
    protected void onPostExecute(Integer stage) {
        // close the progress dialog
        if (stage == -1) {
            ((MOAInstallerActivity) activity).install(10); // indication of error
            return;
        }
        dialog.setMessage(msg2);
        dialog.show();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }
        dialog.dismiss();
        ((MOAInstallerActivity) activity).install(stage + 1);
    }

    @Override
    protected Integer doInBackground(Integer... arg) {
        Integer stage = arg[0];
        if (inst == null || directory == null) {
            return (-1);
        }
        ZipInputStream zin = new ZipInputStream(inst);
        ZipEntry ze = null;
        int c = 0;
        BufferedOutputStream fos = null;
        File file = null;
        byte[] buf = new byte[1024 * 1024];
        try {
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName();
                file = new File(directory, name);
                if (ze.isDirectory()) {
                    // case of directory
                    if (!file.mkdirs()) {
                        return (-1);
                    }
                } else {
                    // case of file
                    if (file.exists()) {
                        file.delete();
                    }
                    fos = new BufferedOutputStream(new FileOutputStream(file),
                            64 * 1024);
                    int numread = 0;
                    while ((numread = zin.read(buf)) != -1) {
                        fos.write(buf, 0, numread);
                        publishProgress(c++);
                    }
                    fos.close();
                }
            }
        } catch (IOException e) {
            Log.d("MoA", "exception12");
            e.printStackTrace();
            try {
                fos.close();
            } catch (IOException e1) {
                Log.d("MoA", "exception13");
                e1.printStackTrace();
                return (-1);
            }
            file.delete();
            return (-1);
        }
        return stage;
    }

}
