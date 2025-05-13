package org.maximandroid.cas;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

// AppCompatActivity
public final class MOAInstallerActivity extends Activity {
    File installedDir;
    File internalDir;
    TextView msg;
    long intStorageAvail;

    private long internalFlashAvail() {
        StatFs fs = new StatFs(internalDir.getAbsolutePath());
        return (fs.getAvailableBytes() / (1024 * 1024));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.moainstallerview);

        internalDir = this.getFilesDir();
        msg = findViewById(R.id.checkedTextView1);

        intStorageAvail = Math.abs(internalFlashAvail() - 5);

        long minimumStorageSize = 85L;
        if (intStorageAvail < minimumStorageSize) {
            msg.setText(R.string.storage_insufficient_for_maxima_data);
            install(10);
        }
        installedDir = internalDir;
        install(0); // at the UnzipAsyncTask, install(1), install(2) and install(3)
    }


    public void install(int stage) {
        // Where to Install
        // maxima, init.lisp : internalDir
        // maxima-5.X.0 : installedDir
        final String version = Globals.maximaVersion;

        try {
            switch (stage) {
                case 0: {
                    UnzipAsyncTask uzt = new UnzipAsyncTask(this);
                    uzt.setParams(this.getAssets().open("additions.zip"),
                            internalDir.getAbsolutePath(), getString(R.string.install_additions),
                            "Additions installed");
                    uzt.execute(0);
                    break;
                }
                case 1: {
                    if (!(chmod755(internalDir.getAbsolutePath() + "/additions/gnuplot/bin/gnuplot") &&
                            chmod755(internalDir.getAbsolutePath() + "/additions/gnuplot/bin/gnuplot.x86") &&
                            chmod755(internalDir.getAbsolutePath() + "/additions/qepcad/bin/qepcad") &&
                            chmod755(internalDir.getAbsolutePath() + "/additions/qepcad/bin/qepcad.x86") &&
                            chmod755(internalDir.getAbsolutePath() + "/additions/qepcad/qepcad.sh") &&
                            chmod755(internalDir.getAbsolutePath() + "/additions/cpuarch.sh"))) {
                        Log.v("MoA", "chmod755 failed.");
                        install(10);
                        finish();
                    }

                    CpuArchitecture.initCpuArchitecture();
                    if (CpuArchitecture.getCpuArchitecture().startsWith("not")) {
                        Log.v("MoA", "Install of additions failed.");
                        install(10);
                        finish();
                    }

                    // Existence of file x86 is used in qepcad.sh
                    if (CpuArchitecture.getCpuArchitecture().equals(CpuArchitecture.X86)) {
                        File x86File = new File(internalDir.getAbsolutePath() + "/x86");
                        if (!x86File.exists()) {
                            boolean result = x86File.createNewFile();
                            Log.v("MA", "x86: " + x86File.getAbsolutePath() + ", " + result);
                        }
                    }

                    String maximaFile = CpuArchitecture.getMaximaFile();
                    if (maximaFile.startsWith("not")) {
                        Log.v("MoA", "Install of additions failed.");
                        install(10);
                        finish();
                    }

                    String initlispPath = internalDir.getAbsolutePath()
                            + "/init.lisp";
                    String firstLine = "(setq *maxima-dir* \""
                            + installedDir.getAbsolutePath() + "/maxima-" + version
                            + "\")\n";
                    copyFileFromAssetsToLocal("init.lisp", initlispPath, firstLine);
                    Log.d("My Test", "Clicked!1.1");

                    UnzipAsyncTask uzt = new UnzipAsyncTask(this);
                    uzt.setParams(this.getAssets().open(maximaFile + ".zip"),
                            internalDir.getAbsolutePath(), getString(R.string.install_maxima_binary),
                            "maxima binary installed");
                    uzt.execute(1);
                    break;
                }
                case 2: {
                    chmod755(internalDir.getAbsolutePath() + "/" + CpuArchitecture.getMaximaFile());
                    UnzipAsyncTask uzt = new UnzipAsyncTask(this);
                    uzt.setParams(this.getAssets().open("maxima-" + version + ".zip"),
                            installedDir.getAbsolutePath(), getString(R.string.install_maxima_data),
                            "maxima data installed");
                    uzt.execute(2);
                    break;
                }
                case 3: {
                    Intent data = new Intent();
                    data.putExtra("sender", "MOAInstallerActivity");
                    setResult(RESULT_OK, data);

                    finish();
                    break;
                }
                case 10: {
                    // Error indicated
                    Intent data = new Intent();
                    data.putExtra("sender", "MOAInstallerActivity");
                    setResult(RESULT_CANCELED, data);

                    finish();
                    break;
                }
                default:
                    break;
            }
        } catch (IOException e1) {
            Log.d("MoA", "exception8");
            e1.printStackTrace();
            finish();
        } catch (Exception e) {
            Log.d("MoA", "exception9");
            e.printStackTrace();
            finish();
        }
    }

    private void copyFileFromAssetsToLocal(String src, String dest, String line)
            throws Exception {
        InputStream fileInputStream = getApplicationContext().getAssets().open(
                src);
        BufferedOutputStream buf = new BufferedOutputStream(
                new FileOutputStream(dest));
        int read;
        byte[] buffer = new byte[4096 * 128];
        buf.write(line.getBytes());
        while ((read = fileInputStream.read(buffer)) > 0) {
            buf.write(buffer, 0, read);
        }
        buf.close();
        fileInputStream.close();
    }

    private boolean chmod755(String filename) {
        return (new File(filename).setExecutable(true, true));
    }

    private boolean recursiveRemoveFileDirectory(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            for (File node : file.listFiles()) {
                boolean res = recursiveRemoveFileDirectory(node.getAbsolutePath());
                if (!res) return false;
            }
        }
        return file.delete();
    }

}
