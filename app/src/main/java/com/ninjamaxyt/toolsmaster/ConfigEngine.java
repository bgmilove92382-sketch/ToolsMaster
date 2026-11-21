package com.ninjamaxyt.toolsmaster;

import android.content.Context;
import android.os.Build;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ConfigEngine {

    public static void patchAndApply(Context ctx, String pkgName, IFileService shizukuService, int resScale, int gfxVal, int fpsVal, int styleVal, boolean shadow, boolean zeroLag) throws Exception {
        File cacheOut = new File(ctx.getExternalCacheDir(), "patched_files");
        if (cacheOut.exists()) deleteRecursive(cacheOut);
        File cfgOut = new File(cacheOut, "UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android");
        File savOut = new File(cacheOut, "UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames");
        cfgOut.mkdirs();
        savOut.mkdirs();

        String gus = readAssetText(ctx, "GameUserSettings.ini");
        gus = replaceOrAppendIni(gus, "sg.ResolutionQuality", String.valueOf(resScale));
        gus = replaceOrAppendIni(gus, "sg.ShadowQuality", shadow ? "1" : "0");
        gus = replaceOrAppendIni(gus, "sg.AntiAliasingQuality", zeroLag ? "0" : "1");
        gus = replaceOrAppendIni(gus, "sg.TextureQuality", String.valueOf(gfxVal));
        writeBytes(new File(cfgOut, "GameUserSettings.ini"), gus.getBytes(StandardCharsets.UTF_8));

        byte[] uCustom = readAssetBytes(ctx, "UserCustom.ini");
        writeBytes(new File(cfgOut, "UserCustom.ini"), uCustom);

        byte[] savData = readAssetBytes(ctx, "Active.sav");
        patchIntProp(savData, "BattleFPS", fpsVal);
        patchIntProp(savData, "LobbyFPS", fpsVal);
        patchIntProp(savData, "MainCityFPS", fpsVal);
        patchIntProp(savData, "FPSLevel", fpsVal);
        patchIntProp(savData, "RenderQuality", gfxVal);
        patchIntProp(savData, "ArtQuality", styleVal);

        writeBytes(new File(savOut, "Active.sav"), savData);
        writeBytes(new File(savOut, "SettingConfig_Slot.sav"), savData);

        String targetBase = "/storage/emulated/0/Android/data/" + pkgName + "/files";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (shizukuService == null) {
                throw new Exception("Shizuku Service not bound! Tap 'Grant Shizuku Access' first.");
            }
            String res = shizukuService.copyConfigs(cacheOut.getAbsolutePath(), targetBase);
            if (!"OK".equals(res)) {
                throw new Exception(res);
            }
        } else {
            copyRecursive(cacheOut, new File(targetBase));
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
                for (int k = i + nameBytes.length; k < i + nameBytes.length + 16; k++) {
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

    private static void writeBytes(File f, byte[] data) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        }
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

    private static void copyRecursive(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            String[] children = src.list();
            if (children != null) {
                for (String child : children) copyRecursive(new File(src, child), new File(dst, child));
            }
        } else {
            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        }
    }
}
