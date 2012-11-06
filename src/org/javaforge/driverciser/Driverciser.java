package org.javaforge.driverciser;

import org.apache.log4j.Logger;
import org.javaforge.util.Config;
import org.javaforge.util.Execute;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Manages a list of threads that will do the work of writing to the devices.
 *
 * @author Jared Klett
 */

public class Driverciser {

// Constants //////////////////////////////////////////////////////////////////

    private static final Object mutex = new Object();

// Class variables ////////////////////////////////////////////////////////////

    private static Logger log = Logger.getLogger(Driverciser.class);

// Configuration variables ////////////////////////////////////////////////////

    public static final String CONFIG_NAME = "driverciser";
    public static final String PROPERTY_MIN_SLEEP = "minimum.sleep";
    public static final String PROPERTY_MAX_SLEEP = "maximum.sleep";
    public static final String PROPERTY_FILE_SIZE_LIMIT = "file.size.limit";
    public static final String PROPERTY_BUFFER_SIZE = "buffer.size";
    public static final String PROPERTY_PATH = "path";

    private static int threadSleepMin;
    private static int threadSleepMax;
    private static int fileSizeLimit;
    private static int bufferSize;
    private static String path;

    public static final int DEFAULT_MIN_SLEEP = 10 * 1000; // 10 seconds
    public static final int DEFAULT_MAX_SLEEP = 45 * 1000; // 2 minutes
    public static final int DEFAULT_FILE_SIZE_LIMIT = 1024 * 1024; // * 1024 == 1 GB
    public static final int DEFAULT_BUFFER_SIZE = 256; // * 1024 == 256K
    public static final String DEFAULT_PATH = "/usr/driverciser";

// Instance variables /////////////////////////////////////////////////////////

    private DriverciserThread[] threads;

// Class initializer //////////////////////////////////////////////////////////

    static {
        loadConfiguration();
    }

// Constructor ////////////////////////////////////////////////////////////////

    public Driverciser() {
        threads = new DriverciserThread[0];
    }

// Class methods //////////////////////////////////////////////////////////////

    public static void loadConfiguration() {
        Config config  = new Config(CONFIG_NAME);
        synchronized (Driverciser.class) {
            threadSleepMin = config.intProperty(PROPERTY_MIN_SLEEP, DEFAULT_MIN_SLEEP);
            threadSleepMax = config.intProperty(PROPERTY_MAX_SLEEP, DEFAULT_MAX_SLEEP);
            fileSizeLimit = config.intProperty(PROPERTY_FILE_SIZE_LIMIT, DEFAULT_FILE_SIZE_LIMIT);
            bufferSize = config.intProperty(PROPERTY_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
            path = config.stringProperty(PROPERTY_PATH, DEFAULT_PATH);
        }

        log.info("Loading configuration from file: " + CONFIG_NAME);
        log.info(PROPERTY_MIN_SLEEP + " = " + threadSleepMin);
        log.info(PROPERTY_MAX_SLEEP + " = " + threadSleepMax);
        log.info(PROPERTY_FILE_SIZE_LIMIT + " = " + fileSizeLimit);
        log.info(PROPERTY_BUFFER_SIZE + " = " + bufferSize);
        log.info(DEFAULT_PATH + " = " + path);
    }

// Instance methods ///////////////////////////////////////////////////////////

    public void exercise() {
        List<File> list = getDevicePaths();
        threads = new DriverciserThread[list.size()];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new DriverciserThread(list.get(i), threadSleepMin, threadSleepMax, fileSizeLimit, bufferSize);
        }
        for (DriverciserThread thread : threads) {
            thread.setRunning(true);
            thread.start();
        }
        synchronized(mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting on monitor");
            }
        }
    }

    public void halt() {
        synchronized(mutex) {
            mutex.notify();
        }
        for (DriverciserThread thread : threads) {
            thread.setRunning(false);
            try {
                // TODO: make configurable
                thread.join(5000);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for thread to join!");
            }
        }
    }

    private List<File> getDevicePaths() {
        List<File> list = new ArrayList<File>();
        String[] args = {"mount"};
        long execTimeout = 30 * 1000L;
        long execInterval = 2 * 1000L;
        StringBuilder outbuffer = new StringBuilder();
        StringBuilder errbuffer = new StringBuilder();
        Execute.execute(args, execTimeout, execInterval, null, outbuffer, errbuffer);
        // Parse output to get a list of mounted devices
        StringTokenizer tokenizer = new StringTokenizer(outbuffer.toString());
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith(path)) {
                list.add(new File(token));
            }
        }
        return list;
    }

} // class Driverciser
