package org.maximandroid.cas;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Context;

public final class MaximaVersion {
    private int major;
    private int minor;
    private int patch;

    MaximaVersion() {
        major = 5;
        minor = 41;
        patch = 0;
    }

    MaximaVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    MaximaVersion(int[] vers) {
        this.major = vers[0];
        this.minor = vers[1];
        this.patch = vers[2];
    }

    public void loadVersFromSharedPrefs(Context context) {
        SharedPreferences pref = context.getSharedPreferences("maxima",
                Context.MODE_PRIVATE);
        major = pref.getInt("major", 5);
        minor = pref.getInt("minor", 41);
        patch = pref.getInt("patch", 0);
    }

    public void saveVersToSharedPrefs(Context context) {
        Editor ed = context
                .getSharedPreferences("maxima", Context.MODE_PRIVATE).edit();
        ed.putInt("major", major);
        ed.putInt("minor", minor);
        ed.putInt("patch", patch);
        ed.apply();
    }

    public long versionInteger() {
        long res = ((long) major) * (1000 * 1000) + ((long) minor) * 1000
                + ((long) patch);
        return res;
    }

    public String versionString() {
        return major + "." + minor + "." + patch;
    }

}
