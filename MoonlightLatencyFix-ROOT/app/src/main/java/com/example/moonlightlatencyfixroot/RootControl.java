package com.example.moonlightlatencyfixroot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/** Only the CPU scaling_min_freq and governor are changed; thermal protections remain untouched. */
final class RootControl {
    static final String LITTLE = "/sys/devices/system/cpu/cpufreq/policy0/";
    static final String BIG = "/sys/devices/system/cpu/cpufreq/policy6/";
    private static final String PREFS = "root_originals";
    private static final String[] PATHS = {LITTLE, BIG};
    private final SharedPreferences prefs;
    RootControl(Context ctx) { prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static String quote(String s) { return "'" + s.replace("'", "'\\''") + "'"; }
    static String run(String command) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) { if (out.length() < 4096) out.append(line).append('\n'); }
        }
        if (!p.waitFor(8, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new Exception("Root command timed out"); }
        if (p.exitValue() != 0) throw new Exception("Root command failed: " + out.toString().trim());
        return out.toString().trim();
    }
    static String read(String path) throws Exception { return run("cat " + quote(path)); }
    static void write(String path, String value) throws Exception {
        // Never interpolate untrusted strings into shell commands.
        run("printf %s " + quote(value) + " > " + quote(path));
        // Some cpufreq drivers apply policy changes asynchronously. Read back with a short retry.
        String actual = "";
        for (int attempt=0; attempt<5; attempt++) {
            actual = read(path);
            if (value.equals(actual)) return;
            if (attempt<4) Thread.sleep(100);
        }
        throw new Exception("Verification failed: " + path + " (expected " + value + ", got " + actual + ")");
    }
    boolean hasSnapshot() { return prefs.getBoolean("snapshot", false); }
    private boolean isTabA11() { return "SM-X130".equalsIgnoreCase(Build.MODEL); }
    private boolean suspiciousSnapshot(String l, String b) throws Exception {
        // The initial version could capture maxed-out minimums as "defaults".
        return (l.equals(read(LITTLE+"scaling_max_freq")) &&
                b.equals(read(BIG+"scaling_max_freq")));
    }
    private void validateSnapshot() throws Exception {
        if (!hasSnapshot()) return;
        String l=prefs.getString("lmin", ""), b=prefs.getString("bmin", "");
        if (!l.matches("[0-9]+") || !b.matches("[0-9]+"))
            throw new Exception("Invalid saved CPU defaults. Restore stock settings before continuing.");
        if (isTabA11()) {
            // This model's stock minimums were measured independently. A previous
            // version could persist boosted or maxed-out values as defaults.
            if (!l.equals("500000") || !b.equals("725000")) {
                if (!prefs.edit().putString("lmin","500000")
                        .putString("bmin","725000").commit())
                    throw new Exception("Could not repair saved CPU defaults");
            }
        } else if (suspiciousSnapshot(l,b)) {
            throw new Exception("Saved CPU defaults equal maximum frequencies. Cannot safely restore on this device.");
        }
    }
    // 0 = OFF, 1 = BOOST, 2 = custom/unknown. Read the kernel, not the last button.
    int cpuMode() throws Exception {
        validateSnapshot();
        String l=read(LITTLE+"scaling_min_freq"), b=read(BIG+"scaling_min_freq");
        if (l.equals("1800000") && b.equals("1100000")) return 1;
        // Recognize the known stock policy even if preferences have not yet been
        // initialized or were created by an older buggy release.
        if (isTabA11() && l.equals("500000") && b.equals("725000")) return 0;
        if (hasSnapshot() && l.equals(prefs.getString("lmin", "")) &&
                b.equals(prefs.getString("bmin", ""))) return 0;
        return 2;
    }
    String[] cpuMinimums() throws Exception {
        return new String[]{read(LITTLE+"scaling_min_freq"), read(BIG+"scaling_min_freq")};
    }
    // 0 = original governors, 1 = performance, 2 = custom/unavailable.
    int governorMode() throws Exception {
        String l=read(LITTLE+"scaling_governor"), b=read(BIG+"scaling_governor");
        if (l.equals("performance") && b.equals("performance")) return 1;
        if (hasSnapshot() && l.equals(prefs.getString("lgov", "")) &&
                b.equals(prefs.getString("bgov", ""))) return 0;
        return 2;
    }
    // 0 = Auto (both overrides absent), 1 = 60 Hz, 2 = 90 Hz, 3 = custom.
    int refreshMode() throws Exception {
        String peak=run("settings get system peak_refresh_rate");
        String min=run("settings get system min_refresh_rate");
        if (peak.equals("null") && min.equals("null")) return 0;
        if (peak.equals("60.0") && min.equals("60.0")) return 1;
        if (peak.equals("90.0") && min.equals("90.0")) return 2;
        return 3;
    }
    void snapshot() throws Exception {
        if (hasSnapshot()) { validateSnapshot(); return; }
        String[] values = new String[4];
        for (int i=0;i<2;i++) {
            values[i*2] = read(PATHS[i]+"scaling_min_freq");
            values[i*2+1] = read(PATHS[i]+"scaling_governor");
        }
        if (isTabA11()) {
            // Never capture a currently modified CPU policy as stock on SM-X130.
            values[0]="500000"; values[2]="725000";
        } else if (suspiciousSnapshot(values[0],values[2])) {
            throw new Exception("CPU minimums are maxed; refusing unsafe default snapshot");
        }
        prefs.edit().putString("lmin",values[0]).putString("lgov",values[1])
            .putString("bmin",values[2]).putString("bgov",values[3])
            .putBoolean("snapshot",true).commit();
    }
    void setBoost(boolean on) throws Exception {
        snapshot();
        try {
            write(LITTLE+"scaling_min_freq", on ? "1800000" : prefs.getString("lmin","500000"));
            write(BIG+"scaling_min_freq", on ? "1100000" : prefs.getString("bmin","725000"));
        } catch(Exception e) {
            // A failed BOOST must not leave a partially applied profile.
            if (on) { try { restoreCpu(); } catch(Exception ignored) {} }
            throw e;
        }
    }
    void setGovernor(boolean performance) throws Exception {
        snapshot();
        String l = performance ? "performance" : prefs.getString("lgov", "schedutil");
        String b = performance ? "performance" : prefs.getString("bgov", "schedutil");
        if (performance) {
            for (String path : PATHS) {
                String available=read(path+"scaling_available_governors");
                if (!java.util.Arrays.asList(available.split("\\s+")).contains("performance"))
                    throw new Exception("Performance governor unavailable on " + path);
            }
        }
        try {
            write(LITTLE+"scaling_governor",l);
            write(BIG+"scaling_governor",b);
        } catch(Exception e) { try { restoreGovernors(); } catch(Exception ignored) {} throw e; }
    }
    void setRefresh(String mode) throws Exception {
        if (!mode.equals("auto") && !mode.equals("60") && !mode.equals("90")) throw new Exception("Invalid refresh mode");
        if (mode.equals("auto")) {
            run("settings delete system peak_refresh_rate; settings delete system min_refresh_rate");
            return;
        }
        String v=mode+".0";
        run("settings put system peak_refresh_rate " + v + " && settings put system min_refresh_rate " + v);
        String a=run("settings get system peak_refresh_rate");
        String b=run("settings get system min_refresh_rate");
        if (!v.equals(a) || !v.equals(b)) throw new Exception("Refresh setting verification failed");
    }
    void restoreGovernors() throws Exception {
        if (!hasSnapshot()) return;
        // Governor changes can reset minimum frequencies, so restore governors first.
        write(LITTLE+"scaling_governor",prefs.getString("lgov","schedutil"));
        write(BIG+"scaling_governor",prefs.getString("bgov","schedutil"));
    }
    void restoreCpu() throws Exception {
        if (!hasSnapshot()) return;
        write(LITTLE+"scaling_min_freq",prefs.getString("lmin","500000"));
        write(BIG+"scaling_min_freq",prefs.getString("bmin","725000"));
    }
    void restoreAll() throws Exception {
        Exception error=null;
        try { restoreGovernors(); } catch(Exception e) { error=e; }
        try { restoreCpu(); } catch(Exception e) { if(error==null) error=e; }
        try { setRefresh("auto"); } catch(Exception e) { if(error==null) error=e; }
        if (error!=null) throw error;
        prefs.edit().clear().commit();
    }
    String getDefaultGovernor() { return prefs.getString("lgov", "original"); }
}
