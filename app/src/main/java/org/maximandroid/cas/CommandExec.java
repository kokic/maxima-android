package org.maximandroid.cas;

import android.util.Log;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;

public class CommandExec {
    StringBuilder outputBuffer = new StringBuilder();
    ProcessBuilder processBuilder = null;
    Process process;
    InputStream is;
    OutputStream os;

    // setenforce 0
    public void execCommand(List<String> commandList) throws IOException {
        Log.v("MoA", "commands: " + commandList.size());
        processBuilder = new ProcessBuilder(commandList);
        Log.v("MoA", "pb: " + processBuilder);

        // process starts
        process = processBuilder.start();

        is = process.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            if (c == 0x04) {
                break;
            }
            this.outputBuffer.append((char) c);
        }
    }

    public String trimmedCode(String code) {
        String trimmed = code.trim();
        int last = trimmed.length() - 1;
        return trimmed.charAt(last) == ';' ? trimmed : trimmed + ";";
    }

    public void maximaCmd(String mcmd) throws IOException, Exception {
        mcmd = trimmedCode(mcmd);
        Log.v("MoA", process.toString());

        if (!mcmd.equals("") && process != null) {
            // obtain process standard output stream
            os = process.getOutputStream();
            if (os != null) {
                os.write(mcmd.getBytes("UTF-8"));
                os.flush();
            }
        }

        while (true) {
            int c = is.read();
            if (c == 0x04) {
                /* 0x04 is the prompt indicator */
                /*
                 * if (is.available()==0) { break; }
                 */
                break;
            } else if (c == -1) {
                is.close();
                break;
            } else if (c == 0x5c) { // 0x5c needs to be escaped by 0x5c, the
                // backslash.
                this.outputBuffer.append((char) c);
                this.outputBuffer.append((char) c);
            } else if (c == 0x27) { // 0x27 needs to be escaped as it is q
                // single quote.
                this.outputBuffer.append((char) 0x5c);
                this.outputBuffer.append((char) c);
            } else {
                this.outputBuffer.append((char) c);
            }
        }
    }

    public String getProcessResult() {
        return (new String(this.outputBuffer));
    }

    public void clearStringBuilder() {
        this.outputBuffer.delete(0, this.outputBuffer.length());
    }
}
