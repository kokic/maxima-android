package org.maximandroid.cas;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Maxima {
    private final Activity activity;

    Semaphore sem = new Semaphore(1);
    CommandExec maximaProcess;

    public Maxima(Activity activity) {
        this.activity = activity;
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public void onCreate() {
        if (!StorageUtil.maximaBinaryExists()
                || !(StorageUtil.maximaInternalFile("additions").exists())
                || !(StorageUtil.maximaInitFile().exists())
                || !(StorageUtil.maximaPackageFile().exists())
        ) {
            Intent intent = new Intent(activity, MOAInstallerActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            activity.startActivityForResult(intent, StorageUtil.MAXIMA_INSTALL_CODE);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startMaxima();
                }
            }).start();
        }
    }

    public String executeCodeNullable(String code) {
        try {
            maximaProcess.maximaCmd(code);
            String result = maximaProcess.getProcessResult();
            maximaProcess.clearStringBuilder();
            return result;
        } catch (Exception e) {
            Log.e("MA", "exec failed ...");
            e.printStackTrace();
        }
        return null;
    }

    private void exitMOA() {
        try {
            maximaProcess.maximaCmd("quit();\n");
            activity.finish();
        } catch (IOException e) {
            Log.d("MoA", "exception7 :: " + e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d("MoA", "exception8 :: " + e.toString());
            e.printStackTrace();
        }
        activity.finish();
    }

    public void startMaxima() {
        Log.d("MoA", "startMaxima()");
        try {
            sem.acquire();
        } catch (InterruptedException e1) {
            Log.d("MoA", "exception1 :: " + e1.toString());
            e1.printStackTrace();
        }

        CpuArchitecture.initCpuArchitecture();

        if (!StorageUtil.maximaPackageFile().exists()) {
            activity.finish();
        }

        List<String> list = new ArrayList<>();

        File maximaBinary = StorageUtil.maximaBinaryFile();
        Log.v("MoA", "exec: " + maximaBinary.setExecutable(true));
        Log.v("MoA", "read: " + maximaBinary.setReadable(true));

        list.add(maximaBinary.getAbsolutePath());
        list.add("--init-lisp=" + StorageUtil.maximaInitFile().getAbsolutePath());

        maximaProcess = new CommandExec();
        try {
            if (maximaBinary.canExecute()) {
                maximaProcess.execCommand(list);
            }
        } catch (Exception e) {
            Log.d("MoA", "exception2 ::" + e.toString());
            e.printStackTrace();
            exitMOA();
        }

        maximaProcess.clearStringBuilder();
        sem.release();
        Log.v("MoA", "sem released.");
    }

}
