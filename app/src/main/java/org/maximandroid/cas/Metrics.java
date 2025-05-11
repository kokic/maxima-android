package org.maximandroid.cas;

import android.content.Context;

public class Metrics {
    public static int padding;

    public static void init(Context context) {
        padding = (int) (16 * context.getResources().getDisplayMetrics().density);
    }
}
