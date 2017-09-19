package org.apache.log4j;

import org.apache.log4j.helpers.GZipUtils;
import org.apache.log4j.helpers.LogLog;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: DailyExRollingFileAppender</p>
 * <p>Description: DailyRollingFileAppender with maxBackupIndex</p>
 *
 * @author Victor
 * @version 1.3.1
 * @see DailyRollingFileAppender
 * @since 2017 /8/31
 */
public class DailyExRollingFileAppender extends DailyRollingFileAppender {

    /**
     * @since 1.3.1
     */
    private int maxBackupIndex;

    /**
     * @since 1.3.2
     */
    private boolean fileCompress;

    /**
     * The constant threadPool.
     *
     * @since 1.3.3
     */
    protected final static Executor threadPool = Executors.newFixedThreadPool(4, new CompressThreadFactory());

    /**
     * Gets max backup index.
     *
     * @return the max backup index
     */
    public int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    /**
     * Sets max backup index.
     *
     * @param maxBackupIndex the max backup index
     */
    public void setMaxBackupIndex(int maxBackupIndex) {
        this.maxBackupIndex = maxBackupIndex;
    }

    /**
     * Gets file compress.
     *
     * @return the boolean
     */
    public boolean getFileCompress() {
        return fileCompress;
    }

    /**
     * Sets file compress.
     *
     * @param fileCompress the file compress
     */
    public void setFileCompress(boolean fileCompress) {
        this.fileCompress = fileCompress;
    }

    @Override
    void rollOver() throws IOException {

        /* Compute filename, but only if datePattern is specified */
        if (getDatePattern() == null) {
            errorHandler.error("Missing DatePattern option in rollOver().");
            return;
        }

        String datedFilename = fileName + sdf.format(now);
        // It is too early to roll over because we are still within the
        // bounds of the current interval. Rollover will occur once the
        // next interval is reached.
        if (getScheduledFilename().equals(datedFilename)) {
            return;
        }

        // close current file, and rename it to datedFilename
        this.closeFile();

        // delete file if the file exists.
        File target = new File(getScheduledFilename());
        if (target.exists()) {
            target.delete();
        }

        // rename file to previous name.
        File file = new File(fileName);
        boolean result = file.renameTo(target);
        if (result) {
            LogLog.debug(fileName + " -> " + getScheduledFilename());
        } else {
            LogLog.error("Failed to rename [" + fileName + "] to [" + getScheduledFilename() + "].");
        }

        try {
            // This will also close the file. This is OK since multiple
            // close operations are safe.
            this.setFile(fileName, true, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            errorHandler.error("setFile(" + fileName + ", true) call failed.");
        }

        setScheduledFilename(datedFilename);

        /* Delete history files if more than maxBackupIndex */
        if (file.getParentFile().exists() && this.getMaxBackupIndex() > 0) {

            // get all history files.
            File[] files = file.getParentFile().listFiles(new LogFileFilter(file.getName()));

            // soft by date asc.
            Arrays.sort(files);

            // delete files if more than maxBackupIndex.
            if (files.length > maxBackupIndex) {
                for (int i = 0; i < files.length - maxBackupIndex; i++) {
                    File dateFile = files[i];
                    if (dateFile.exists()) {
                        dateFile.delete();
                    }
                }
            }
        }

        /* Compress log file and delete source */
        if (this.getFileCompress() && result) {

            threadPool.execute(new CompressThread(target));
        }
    }
}

/**
 * The type Log file filter.
 */
class LogFileFilter implements FileFilter {

    private String logName;

    /**
     * Instantiates a new Log file filter.
     *
     * @param logName the log name
     */
    public LogFileFilter(String logName) {
        this.logName = logName;
    }

    @Override
    public boolean accept(File file) {
        if (logName == null || file.isDirectory()) {
            return false;
        } else {
            LogLog.debug(file.getName());
            return file.getName().startsWith(logName) &&
                    !file.getName().equals(logName);
        }
    }
}

/**
 * The type Compress thread.
 */
class CompressThread implements Runnable {

    private final File targetFile;

    /**
     * Instantiates a new Compress and clean thread.
     *
     * @param targetFile the target file
     */
    public CompressThread(File targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    public void run() {

        if (targetFile != null && targetFile.exists())
            try {
                GZipUtils.compress(targetFile, true);
            } catch (IOException e) {
                LogLog.error("Failed to compress [" + targetFile.getName() + "].", e);
            }
    }
}

/**
 * The type Compress thread factory.
 */
class CompressThreadFactory implements ThreadFactory {

    private AtomicInteger i = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {

        Thread thread = new Thread(r);

        thread.setDaemon(true);

        thread.setName("CompressThread" + "-" + i.getAndIncrement());

        return thread;
    }
}