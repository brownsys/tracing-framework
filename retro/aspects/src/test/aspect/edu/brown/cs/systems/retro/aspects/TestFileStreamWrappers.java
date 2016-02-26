package edu.brown.cs.systems.retro.aspects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import edu.brown.cs.systems.retro.wrappers.FileInputStreamWrapper;
import edu.brown.cs.systems.retro.wrappers.FileOutputStreamWrapper;
import junit.framework.TestCase;

public class TestFileStreamWrappers extends TestCase {

    @Test
    public void testFileStreamWrappers() throws IOException {
        File f = File.createTempFile("aspectjinput", "txt");
        f.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(f);
        assertTrue(fos instanceof FileOutputStreamWrapper);

        FileOutputStream fos2 = new FileOutputStream(f.getAbsolutePath());
        assertTrue(fos2 instanceof FileOutputStreamWrapper);

        FileOutputStream fos3 = new FileOutputStream(fos.getFD());
        assertTrue(fos3 instanceof FileOutputStreamWrapper);

        fos.write(5);
        fos.close();

        FileInputStream fis = new FileInputStream(f);
        assertTrue(fis instanceof FileInputStreamWrapper);

        FileInputStream fis2 = new FileInputStream(f.getAbsolutePath());
        assertTrue(fis2 instanceof FileInputStreamWrapper);

        FileInputStream fis3 = new FileInputStream(fis.getFD());
        assertTrue(fis3 instanceof FileInputStreamWrapper);
    }

}
