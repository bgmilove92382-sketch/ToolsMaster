package com.ninjamaxyt.toolsmaster;

import android.content.Context;
import android.os.Build;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import rikka.shizuku.Shizuku;

public class ConfigEngine {

    public static void patchAndApply(Context ctx, String pkgName, int resScale, int gfxVal, int fpsVal, int styleVal, boolean shadow, boolean zeroLag) throws Exception {
        File cacheOut = new File(ctx.getExternalCacheDir(), "patched_files");
        if (cacheOut.exists()) deleteRecursive(cacheOut);
        cacheOut.mkdirs();

        String targetBase = "/storage/emulated/0/Android/data/" + pkgName + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved";
        String targetCfg = targetBase + "/Config/Android";
        String targetSav = targetBase + "/SaveGames";

        boolean isSuperSmooth = (gfxVal == -1);
        int iniQuality = isSuperSmooth ? 0 : gfxVal;

        File localRealSav = new File(cacheOut, "RealActive.sav");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Shizuku.pingBinder()) {
                throw new Exception("Shizuku is not running! Start Shizuku service first.");
            }
            runShizukuShell("am force-stop " + pkgName + " || true");
            runShizukuShell("cp -f '" + targetSav + "/Active.sav' '" + localRealSav.getAbsolutePath() + "' 2>/dev/null || true");
        } else {
            File f = new File(targetSav + "/Active.sav");
            if (f.exists()) copyFile(f, localRealSav);
        }

        // GameUserSettings.ini
        String gus = readAssetText(ctx, "GameUserSettings.ini");
        gus = replaceOrAppendIni(gus, "sg.ResolutionQuality", String.valueOf(resScale));
        gus = replaceOrAppendIni(gus, "sg.ShadowQuality", shadow ? "1" : "0");
        gus = replaceOrAppendIni(gus, "sg.AntiAliasingQuality", zeroLag ? "0" : "1");
        gus = replaceOrAppendIni(gus, "sg.TextureQuality", String.valueOf(iniQuality));
        gus = replaceOrAppendIni(gus, "sg.EffectsQuality", "0");
        gus = replaceOrAppendIni(gus, "sg.FoliageQuality", "0");
        gus = replaceOrAppendIni(gus, "sg.ViewDistanceQuality", isSuperSmooth ? "0" : "1");
        File localGus = new File(cacheOut, "GameUserSettings.ini");
        writeBytes(localGus, gus.getBytes(StandardCharsets.UTF_8));

        // UserCustom.ini with Super Smooth + Extreme/90/120 FPS CVars
        int maxFpsNum = (fpsVal == 8) ? 120 : (fpsVal == 7) ? 90 : (fpsVal == 6) ? 60 : 40;
        String uCustom = "[UserCustom DeviceProfile]\n" +
                "+CVars=r.PUBGQualityLevel=" + (isSuperSmooth ? "-1" : String.valueOf(iniQuality)) + "\n" +
                "+CVars=r.PUBGMaxSupportQualityLevel=3\n" +
                "+CVars=r.UserQualitySetting=" + (isSuperSmooth ? "-1" : String.valueOf(iniQuality)) + "\n" +
                "+CVars=r.PUBGSuperSmooth=" + (isSuperSmooth ? "1" : "0") + "\n" +
                "+CVars=r.PUBGLowEndMode=" + (isSuperSmooth ? "1" : "0") + "\n" +
                "+CVars=r.PUBGDeviceFPSLow=" + maxFpsNum + "\n" +
                "+CVars=r.PUBGDeviceFPSMid=" + maxFpsNum + "\n" +
                "+CVars=r.PUBGDeviceFPSHigh=" + maxFpsNum + "\n" +
                "+CVars=r.PUBGDeviceFPSHDR=" + maxFpsNum + "\n" +
                "+CVars=r.PUBGDeviceFPSUltralHigh=" + maxFpsNum + "\n" +
                "+CVars=r.MobileContentScaleFactor=" + (resScale / 100.0f) + "\n" +
                "+CVars=r.ShadowQuality=" + (shadow ? "1" : "0") + "\n" +
                "+CVars=r.DefaultFeature.AntiAliasing=" + (zeroLag ? "0" : "1") + "\n\n" +
                "[BackUp DeviceProfile]\n" +
                "+CVars=r.PUBGMaxSupportQualityLevel=3\n" +
                "+CVars=r.PUBGSuperSmooth=" + (isSuperSmooth ? "1" : "0") + "\n" +
                "+CVars=r.PUBGDeviceFPSHigh=" + maxFpsNum + "\n";
        File localUCustom = new File(cacheOut, "UserCustom.ini");
        writeBytes(localUCustom, uCustom.getBytes(StandardCharsets.UTF_8));

        // Binary patch Active.sav
        byte[] savData = (localRealSav.exists() && localRealSav.length() > 100)
                ? readFileBytes(localRealSav)
                : readAssetBytes(ctx, "Active.sav");

        patchIntProp(savData, "BattleFPS", fpsVal);
        patchIntProp(savData, "LobbyFPS", fpsVal);
        patchIntProp(savData, "MainCityFPS", fpsVal);
        patchIntProp(savData, "FPSLevel", fpsVal);
        patchIntProp(savData, "BattleRenderQuality", gfxVal);
        patchIntProp(savData, "LobbyRenderQuality", gfxVal);
        patchIntProp(savData, "ArtQuality", styleVal);
        patchBoolProp(savData, "bSuperSmooth", isSuperSmooth);
        patchBoolProp(savData, "SuperSmooth", isSuperSmooth);

        File localPatchedSav = new File(cacheOut, "Active.sav");
        writeBytes(localPatchedSav, savData);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String cmd = "mkdir -p '" + targetCfg + "' '" + targetSav + "' && " +
                    "cp -f '" + localGus.getAbsolutePath() + "' '" + targetCfg + "/GameUserSettings.ini' && " +
                    "cp -f '" + localUCustom.getAbsolutePath() + "' '" + targetCfg + "/UserCustom.ini' && " +
                    "cp -f '" + localPatchedSav.getAbsolutePath() + "' '" + targetSav + "/Active.sav' && " +
                    "chmod 660 '" + targetCfg + "/UserCustom.ini' '" + targetSav + "/Active.sav'";
            runShizukuShell(cmd);
        } else {
            new File(targetCfg).mkdirs();
            new File(targetSav).mkdirs();
            copyFile(localGus, new File(targetCfg, "GameUserSettings.ini"));
            copyFile(localUCustom, new File(targetCfg, "UserCustom.ini"));
            copyFile(localPatchedSav, new File(targetSav, "Active.sav"));
        }
    }

    private static void runShizukuShell(String command) throws Exception {
        Method newProc = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        newProc.setAccessible(true);
        Process p = (Process) newProc.invoke(null, new String[]{"sh", "-c", command}, null, null);
        int exit = p.waitFor();
        if (exit != 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String err = br.readLine();
            throw new Exception("Shizuku shell failed (" + exit + "): " + (err != null ? err : "Permission denied"));
        }
    }

    private static void patchIntProp(byte[] data, String propName, int newVal) {
        byte[] nameBytes = (propName + "\0").getBytes(StandardCharsets.US_ASCII);
        byte[] typeBytes = "IntProperty\0".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < data.length - nameBytes.length - 30; i++) {
            boolean match = true;
            for (int j = 0; j < nameBytes.length; j++) {
                if (data[i + j] != nameBytes[j]) { match = false; break; }
            }
            if (match) {
                for (int k = i + nameBytes.length; k < i + nameBytes.length + 20; k++) {
                    boolean tMatch = true;
                    for (int t = 0; t < typeBytes.length; t++) {
                        if (data[k + t] != typeBytes[t]) { tMatch = false; break; }
                    }
                    if (tMatch) {
                        int valOffset = k + typeBytes.length + 9;
                        if (valOffset + 4 <= data.length) {
                            byte[] le = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(newVal).array();
                            System.arraycopy(le, 0, data, valOffset, 4);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void patchBoolProp(byte[] data, String propName, boolean newVal) {
        byte[] nameBytes = (propName + "\0").getBytes(StandardCharsets.US_ASCII);
        byte[] typeBytes = "BoolProperty\0".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < data.length - nameBytes.length - 30; i++) {
            boolean match = true;
            for (int j = 0; j < nameBytes.length; j++) {
                if (data[i + j] != nameBytes[j]) { match = false; break; }
            }
            if (match) {
                for (int k = i + nameBytes.length; k < i + nameBytes.length + 20; k++) {
                    boolean tMatch = true;
                    for (int t = 0; t < typeBytes.length; t++) {
                        if (data[k + t] != typeBytes[t]) { tMatch = false; break; }
                    }
                    if (tMatch) {
                        int valOffset = k + typeBytes.length + 8;
                        if (valOffset < data.length) {
                            data[valOffset] = (byte) (newVal ? 1 : 0);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static String replaceOrAppendIni(String text, String key, String val) {
        String regex = "(?m)^" + key.replace(".", "\\.") + "=.*$";
        if (text.matches("(?s).*" + regex + ".*")) {
            return text.replaceAll(regex, key + "=" + val);
        }
        return text + "\n" + key + "=" + val + "\n";
    }

    private static String readAssetText(Context ctx, String name) throws Exception {
        return new String(readAssetBytes(ctx, name), StandardCharsets.UTF_8);
    }

    private static byte[] readAssetBytes(Context ctx, String name) throws Exception {
        try (InputStream is = ctx.getAssets().open(name);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }

    private static byte[] readFileBytes(File f) throws Exception {
        try (FileInputStream is = new FileInputStream(f);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }

    private static void writeBytes(File f, byte[] data) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        writeBytes(dst, readFileBytes(src));
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }
}
