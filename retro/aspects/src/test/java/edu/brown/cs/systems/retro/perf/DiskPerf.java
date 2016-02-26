package edu.brown.cs.systems.retro.perf;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import edu.brown.cs.systems.retro.perf.utils.AspectJPerfAPI;
import edu.brown.cs.systems.retro.perf.utils.AspectJPerfTest;

/**
 * Perf test of XTraceAPICalls
 * 
 * @author a-jomace
 */
public class DiskPerf extends AspectJPerfTest {

    private static final String name = "Disk Interception Tracking";
    private static final String description = "Tests cost of intercepting and logging Disk operations";

    public DiskPerf() {
        super(name, description, 4, 1000);
    }

    @Test
    public void testXTraceAPICalls() throws IOException {
        fileExists();
        printResults();
    }

    private void fileExists() throws IOException {
        final File f = File.createTempFile("fileExists", "txt");
        f.deleteOnExit();
        PerfRunnable r = new PerfRunnable() {
            public void run(AspectJPerfAPI api) {
                api.fileExists(f);
            }
        };
        doTest("File.exists()", "", r);
    }

}
