package org.maximandroid.cas;

import android.os.Build;

public final class CpuArchitecture {
    static final String X86 = "x86";
    static final String ARM = "arm";
    static final String NOT_SUPPORTED = "not supported";
    static final String NOT_INITIALIZED = "not initialized";

    private static String cpu_arch = NOT_INITIALIZED;

    private CpuArchitecture() {}

    public static String getCpuArchitecture() {
        return cpu_arch;
    }

    public static void initCpuArchitecture() {
        String res = Build.CPU_ABI.toLowerCase();
        if (res.contains(X86)) {
            cpu_arch = X86;
        } else if (res.contains(ARM)) {
            cpu_arch = ARM;
        } else if (res.equals(NOT_SUPPORTED)) {
            cpu_arch = NOT_SUPPORTED;
        }
    }

    public static String getMaximaFile() {
        if (cpu_arch.startsWith("not")) {
            return cpu_arch;
        }
        if (cpu_arch.equals(X86)) {
            return ("maxima.x86.pie");
        } else if (cpu_arch.equals(ARM)) {
            return ("maxima.pie");
        }
        return NOT_SUPPORTED;
    }
}
