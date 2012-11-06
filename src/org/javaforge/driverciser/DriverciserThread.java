package org.javaforge.driverciser;

import org.apache.log4j.Logger;
import org.javaforge.messaging.Sendmail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;

/**
 * Does the work of writing random bytes to a file on a device.
 *
 * @author Jared Klett
 */

public class DriverciserThread extends Thread {

    private static final String HASH_ALGORITHM = "SHA-1";

// Class variables ////////////////////////////////////////////////////////////

    private static Logger log = Logger.getLogger(DriverciserThread.class);

// Instance variables /////////////////////////////////////////////////////////

    private int bufferSize;
    private int fileSizeLimit;
    private int lowerFileSizeLimit;
    private int runSleepMin;
    private int runSleepMax;
    private int writeSleepMax;
    private boolean running;
    private File devicePath;
    private Random random = new Random();

// Constructor ////////////////////////////////////////////////////////////////

    public DriverciserThread(File devicePath, int sleepMin, int sleepMax, int fileSizeLimit, int bufferSize) {
        super("DriverciserThread-" + devicePath.toString().substring(devicePath.toString().lastIndexOf("/") + 1, devicePath.toString().length()));
        this.devicePath = devicePath;
        this.fileSizeLimit = fileSizeLimit;
        this.bufferSize = bufferSize;
        this.runSleepMin = sleepMin;
        this.runSleepMax = sleepMax;
        this.writeSleepMax = 50;
        this.lowerFileSizeLimit = bufferSize * 4; // * 1024 == 1 MB assuming 256 byte buffer size
    }

// Thread implementation //////////////////////////////////////////////////////

    public void run() {
        log.info("Thread writing to " + devicePath + " starting");
        int runSleepInterval;
        int writeSleepInterval;
        while (running) {
            // Decide on a file size
            int fileSize = random.nextInt(fileSizeLimit);
            if (fileSize < lowerFileSizeLimit)
                fileSize = lowerFileSizeLimit;
            log.info("Next file size: " + fileSize + "KB");
            // Decide on a sleep intervals
            int randomSleepInterval = random.nextInt(runSleepMax);
            if (randomSleepInterval < runSleepMin)
                randomSleepInterval = runSleepMin;
            runSleepInterval = randomSleepInterval;
            log.info("Next run sleep interval: " + runSleepInterval + " ms");
            randomSleepInterval = random.nextInt(writeSleepMax);
            if (randomSleepInterval == 0)
                randomSleepInterval = writeSleepMax;
            writeSleepInterval = randomSleepInterval;
            log.info("Write sleep interval: " + writeSleepInterval + " ms");
            // Create a GUID
            String uuidString = UUID.randomUUID().toString();
            // Create a subdirectory if necessary
            String directory = uuidString.substring(0, 2);
            File dirHandle = new File(devicePath, directory);
            boolean created = dirHandle.mkdir();
            // Check available space, remember that file size is in KB
            long freeSpace = dirHandle.getFreeSpace() / 1024;
            if (fileSize > freeSpace) {
                long totalSpace = dirHandle.getTotalSpace();
                long usedSpace = totalSpace - freeSpace;
                log.warn("Chosen file size exceeds available space: " + usedSpace + " / " + totalSpace);
                // We're done, exit out
                setRunning(false);
                continue;
            }
            // Create a filename
            String filename = "Driverciser-" + uuidString;
            File file;
            if (dirHandle.exists())
                file = new File(dirHandle, filename); // write to the subdir
            else {
                file = new File(devicePath, filename); // write to the root dir
                log.warn("Directory: " + dirHandle + " does not exist, create " + (created ? "succeeded" : "failed"));
            }
            // Write the file and return a hash
            long start = System.currentTimeMillis();
            BigInteger writtenHash = writeHash(file, fileSize, writeSleepInterval);
            long delta = System.currentTimeMillis() - start;
            log.info(getName() + " wrote " + fileSize + "K in " + delta + " ms");
            // Done writing the file, now read it back and hash it
            start = System.currentTimeMillis();
            BigInteger readHash = readHash(file);
            delta = System.currentTimeMillis() - start;
            log.info(getName() + " read file and hashed in " + delta + " ms");
            if (writtenHash == null || readHash == null) {
                log.warn("----> Got a null hash! Written hash: " + writtenHash + ", read hash: " + readHash);
            } else if (!readHash.equals(writtenHash)) {
                log.warn("----> Hashes were not equal! Written hash: " + writtenHash.toString(36) + ", read hash: " + readHash.toString(36));
            }
            try {
                sleep(runSleepInterval);
            } catch (InterruptedException e) {
                log.warn("Interrupted during sleep!");
            }
        }
        log.info("Thread writing to " + devicePath + " exiting");
        Sendmail.send("jared@blip.tv", "Driverciser thread " + this.getName() + " finished", devicePath.getAbsolutePath());
    }

    private BigInteger writeHash(File file, int fileSize, int sleepInterval) {
        int count = 0;
        MessageDigest md = null;
        FileOutputStream out = null;
        byte[] buffer = new byte[bufferSize * 1024];
        // Open a stream to the file
        try {
            out = new FileOutputStream(file);
            md = MessageDigest.getInstance(HASH_ALGORITHM);
            // Loop until we hit the limit
            while (count < (fileSize * 1024)) {
                random.nextBytes(buffer);
                out.write(buffer);
                out.flush();
                count += buffer.length;
                md.update(buffer, 0, buffer.length);
                try {
                    sleep(sleepInterval);
                } catch (InterruptedException e) {
                    log.warn("Interrupted during sleep!");
                }
            }
        } catch (Exception e) {
            log.error("Caught exception during run!", e);
            return null;
        } finally {
            if (out != null) try { out.close(); } catch (Exception e) { /* ignore */ }
        }
        return new BigInteger(1, md.digest());
    }

    private BigInteger readHash(File file) {
        FileInputStream in = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(HASH_ALGORITHM);
            in = new FileInputStream(file);
            byte[] buffer = new byte[64 * 1024];
            int c;
            while (( c = in.read(buffer)) != -1) {
                md.update(buffer, 0, c);
            }
        } catch (Exception e) {
            log.error("Caught exception while reading the file back in!", e);
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception e) { /* ignore */ }
        }
        return new BigInteger(1, md.digest());
    }

// Mutators ///////////////////////////////////////////////////////////////////

    public void setRunning(boolean running) {
        this.running = running;
    }

} // class DriverciserThread
