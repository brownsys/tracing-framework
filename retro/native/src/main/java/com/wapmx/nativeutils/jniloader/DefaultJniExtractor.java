// $Id: DefaultJniExtractor.java 135270 2008-01-16 16:00:27Z richardv $
package com.wapmx.nativeutils.jniloader;

/*
 * Copyright 2006 MX Telecom Ltd.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public class DefaultJniExtractor implements JniExtractor {
    private static boolean debug = false;

    /**
     * this is where JNI libraries are extracted to.
     */
    private File jniDir = null;

    static {
        // initialise the debug switch
        String s = System.getProperty("java.library.debug");
        if (s != null && (s.toLowerCase().startsWith("y") || s.startsWith("1")))
            debug = true;
    }

    /**
     * Gets the working directory to use for jni extraction.
     * <p>
     * Attempts to create it if it doesn't exist.
     *
     * @return jni working dir
     * @throws IOException
     *             if there's a problem creating the dir
     */
    public File getJniDir() throws IOException {
        if (jniDir == null) {
            jniDir = new File(System.getProperty("java.library.tmpdir", "tmplib"));
            if (debug)
                System.err.println("Initialised JNI library working directory to '" + jniDir + "'");
        }

        if (!jniDir.exists()) {
            if (!jniDir.mkdirs())
                throw new IOException("Unable to create JNI library working directory " + jniDir);
        }
        return jniDir;
    }

    /**
     * extract a JNI library from the classpath
     *
     * @param libname
     *            - System.loadLibrary() - compatible library name
     * @return the extracted file
     * @throws IOException
     */
    public File extractJni(String libname) throws IOException {
        String mappedlib = System.mapLibraryName(libname);

        /*
         * on darwin, the default mapping is to .jnilib; but we use .dylibs so
         * that library interdependencies are handled correctly. if we don't
         * find a .jnilib, try .dylib instead.
         */
        if (mappedlib.endsWith(".jnilib")) {
            if (this.getClass().getClassLoader().getResource("META_INF/lib/" + mappedlib) == null)
                mappedlib = mappedlib.substring(0, mappedlib.length() - 7) + ".dylib";
        }

        return extractResource("/META-INF/lib/" + mappedlib, mappedlib);
    }

    /**
     * extract a resource to the tmp dir (this entry point is used for unit
     * testing)
     * 
     * @param resourcename
     *            the name of the resource on the classpath
     * @param outputname
     *            the filename to copy to (within the tmp dir)
     * @return the extracted file
     * @throws IOException
     */
    File extractResource(String resourcename, String outputname) throws IOException {
        File outfile = File.createTempFile("jni_file_", ".dll");
        outfile.deleteOnExit();
        if (outfile.exists()) {
            // multiple processes might be using the same dll at the same time.
            // try to delete it, but if it can't be deleted, then just use it
            try {
                outfile.delete();
            } catch (Exception e) {
                // ignore for now
            }
        }

        // only try to copy if the file doesn't now exist
        if (!outfile.exists()) {
            if (debug)
                System.err.println("Extracting '" + resourcename + "' to '" + outfile.getAbsolutePath() + "'");

            OutputStream out = null;
            InputStream in = null;
            try {
                out = new FileOutputStream(outfile);
                in = DefaultJniExtractor.class.getResourceAsStream(resourcename);
                if (in == null)
                    throw new IOException("Unable to find library " + resourcename + " on classpath");
                copy(in, out);
            } catch (Exception e) {
                // do nothing - even if the copy was bogus, we still try to load
                // it, and let it fail through there
            } finally {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            }
        }
        System.out.println(resourcename + " extracted to temporary file " + outfile);
        return outfile;
    }

    /**
     * copy an InputStream to an OutputStream.
     *
     * @param in
     *            InputStream to copy from
     * @param out
     *            OutputStream to copy to
     * @throws IOException
     *             if there's an error
     */
    static void copy(InputStream in, OutputStream out) throws IOException {
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
