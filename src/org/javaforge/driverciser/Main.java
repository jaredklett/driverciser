package org.javaforge.driverciser;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Starts and stops the Driverciser object.
 *
 * @author Jared Klett
 */

public class Main {

    private static Logger log = Logger.getLogger(Main.class);
    private static String lockFile = "/tmp/driverciser.lock";
    private static Driverciser ciser;
    private static Thread shutdownHook = new Thread("Main.shutdownHook") {
        public void run() {
            if (ciser != null)
                ciser.halt();
            File lockFilehandle = new File(lockFile);
            if (!lockFilehandle.delete())
                log.warn("Could not delete lock file!");
        }
    };

    public static void main(String[] args) {
        // Register our shutdown hook
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        // Create lock file
        File lockFilehandle = new File(lockFile);
        try {
            boolean gotLock = lockFilehandle.createNewFile();
            if (!gotLock) {
                log.warn("Could not create lock file, exiting...");
                return;
            }
        } catch (IOException e) {
            log.error("Caught exception while attempting to create lock file!", e);
            return;
        }
        // Launch
        ciser = new Driverciser();
        ciser.exercise();
    }

} // class Main
