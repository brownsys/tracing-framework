package edu.brown.cs.systems.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TempFileExtractor {

    /**
     * Searches the classpath for any JAR that contains the specified source
     * directory, then creates a folder in the system temp directory and copies
     * the folder contents to the temp director
     */
    public static String extractFolderToTemp(String srcDir, String destName) throws IOException {
        // Create the destination directory in temp storage
        File dest = File.createTempFile(destName, "");
        dest.deleteOnExit();
        dest.delete();
        dest.mkdirs();

        // Find the JAR that contains the src directory
        URL dirURL = TempFileExtractor.class.getClassLoader().getResource(srcDir);
        String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
        JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

        // Enumerate all of the files in the JAR
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(srcDir)) {
                    File copied = new File(dest + File.separator + name);
                    if (entry.isDirectory()) {
                        copied.mkdir();
                    } else {
                        InputStream is = jar.getInputStream(entry);
                        OutputStream os = new FileOutputStream(copied);
                        try {
                            copy(is, os);
                        } finally {
                            is.close();
                            os.close();
                        }
                    }
                    copied.deleteOnExit();
                }
            }
        } finally {
            jar.close();
        }

        return dest + File.separator + destName;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] tmp = new byte[8192];
        int len = 0;
        while (true) {
            len = in.read(tmp);
            if (len <= 0) {
                break;
            }
            out.write(tmp, 0, len);
        }
    }

}
