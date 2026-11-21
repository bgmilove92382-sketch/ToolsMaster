package com.ninjamaxyt.toolsmaster;

import java.io.*;

public class FileService extends IFileService.Stub {
    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public String copyConfigs(String srcDir, String dstDir) {
        try {
            copyRecursive(new File(srcDir), new File(dstDir));
            return "OK";
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }

    private void copyRecursive(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) {
                throw new Exception("Failed mkdirs: " + dst.getAbsolutePath());
            }
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    copyRecursive(new File(src, child), new File(dst, child));
                }
            }
        } else {
            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
