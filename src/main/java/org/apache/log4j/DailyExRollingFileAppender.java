package org.apache.log4j;

import org.apache.log4j.helpers.GZipUtils;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
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
 * @see FileAppender
 * @since 2017 /8/31
 */
public class DailyExRollingFileAppender extends FileAppender {


    // The code assumes that the following constants are in a increasing
    // sequence.
    static final int TOP_OF_TROUBLE = -1;
    static final int TOP_OF_MINUTE = 0;
    static final int TOP_OF_HOUR = 1;
    static final int HALF_DAY = 2;
    static final int TOP_OF_DAY = 3;
    static final int TOP_OF_WEEK = 4;
    static final int TOP_OF_MONTH = 5;


    /**
     * The date pattern. By default, the pattern is set to
     * "'.'yyyy-MM-dd" meaning daily rollover.
     */
    private String datePattern = "'.'yyyy-MM-dd";

    /**
     * The log file will be renamed to the value of the
     * scheduledFilename variable when the next interval is entered. For
     * example, if the rollover period is one hour, the log file will be
     * renamed to the value of "scheduledFilename" at the beginning of
     * the next hour.
     * <p>
     * The precise time when a rollover occurs depends on logging
     * activity.
     */
    private String scheduledFilename;

    /**
     * The next time we estimate a rollover should occur.
     */
    private long nextCheck = System.currentTimeMillis() - 1;

    Date now = new Date();

    SimpleDateFormat sdf;

    RollingCalendar rc = new RollingCalendar();

    int checkPeriod = TOP_OF_TROUBLE;

    // The gmtTimeZone is used only in computeCheckPeriod() method.
    static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");


    /**
     * The default constructor does nothing.
     */
    public DailyExRollingFileAppender() {
    }

    /**
     * Instantiate a <code>DailyRollingFileAppender</code> and open the
     * file designated by <code>filename</code>. The opened filename will
     * become the ouput destination for this appender.
     */
    public DailyExRollingFileAppender(Layout layout, String filename,
                                      String datePattern) throws IOException {
        super(layout, filename, true);
        this.datePattern = datePattern;
        activateOptions();
    }

    /**
     * The <b>DatePattern</b> takes a string in the same format as
     * expected by {@link SimpleDateFormat}. This options determines the
     * rollover schedule.
     */
    public void setDatePattern(String pattern) {
        datePattern = pattern;
    }

    /**
     * Returns the value of the <b>DatePattern</b> option.
     */
    public String getDatePattern() {
        return datePattern;
    }

    public void activateOptions() {
        super.activateOptions();
        if (datePattern != null && fileName != null) {
            now.setTime(System.currentTimeMillis());
            sdf = new SimpleDateFormat(datePattern);
            int type = computeCheckPeriod();
            printPeriodicity(type);
            rc.setType(type);
            File file = new File(fileName);
            scheduledFilename = fileName + sdf.format(new Date(file.lastModified()));

        } else {
            LogLog.error("Either File or DatePattern options are not set for appender ["
                    + name + "].");
        }
    }

    void printPeriodicity(int type) {
        switch (type) {
            case TOP_OF_MINUTE:
                LogLog.debug("Appender [" + name + "] to be rolled every minute.");
                break;
            case TOP_OF_HOUR:
                LogLog.debug("Appender [" + name
                        + "] to be rolled on top of every hour.");
                break;
            case HALF_DAY:
                LogLog.debug("Appender [" + name
                        + "] to be rolled at midday and midnight.");
                break;
            case TOP_OF_DAY:
                LogLog.debug("Appender [" + name
                        + "] to be rolled at midnight.");
                break;
            case TOP_OF_WEEK:
                LogLog.debug("Appender [" + name
                        + "] to be rolled at start of week.");
                break;
            case TOP_OF_MONTH:
                LogLog.debug("Appender [" + name
                        + "] to be rolled at start of every month.");
                break;
            default:
                LogLog.warn("Unknown periodicity for appender [" + name + "].");
        }
    }


    // This method computes the roll over period by looping over the
    // periods, starting with the shortest, and stopping when the r0 is
    // different from from r1, where r0 is the epoch formatted according
    // the datePattern (supplied by the user) and r1 is the
    // epoch+nextMillis(i) formatted according to datePattern. All date
    // formatting is done in GMT and not local format because the test
    // logic is based on comparisons relative to 1970-01-01 00:00:00
    // GMT (the epoch).

    int computeCheckPeriod() {
        RollingCalendar rollingCalendar = new RollingCalendar(gmtTimeZone, Locale.getDefault());
        // set sate to 1970-01-01 00:00:00 GMT
        Date epoch = new Date(0);
        if (datePattern != null) {
            for (int i = TOP_OF_MINUTE; i <= TOP_OF_MONTH; i++) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
                simpleDateFormat.setTimeZone(gmtTimeZone); // do all date formatting in GMT
                String r0 = simpleDateFormat.format(epoch);
                rollingCalendar.setType(i);
                Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
                String r1 = simpleDateFormat.format(next);
                //System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
                if (r0 != null && r1 != null && !r0.equals(r1)) {
                    return i;
                }
            }
        }
        return TOP_OF_TROUBLE; // Deliberately head for trouble...
    }

    /**
     * Rollover the current file to a new file.
     */
    void rollOver() throws IOException {

    /* Compute filename, but only if datePattern is specified */
        if (datePattern == null) {
            errorHandler.error("Missing DatePattern option in rollOver().");
            return;
        }

        String datedFilename = fileName + sdf.format(now);
        // It is too early to roll over because we are still within the
        // bounds of the current interval. Rollover will occur once the
        // next interval is reached.
        if (scheduledFilename.equals(datedFilename)) {
            return;
        }

        // close current file, and rename it to datedFilename
        this.closeFile();

        File target = new File(scheduledFilename);
        if (target.exists()) {
            target.delete();
        }

        File file = new File(fileName);
        boolean result = file.renameTo(target);
        if (result) {
            LogLog.debug(fileName + " -> " + scheduledFilename);
        } else {
            LogLog.error("Failed to rename [" + fileName + "] to [" + scheduledFilename + "].");
        }

        try {
            // This will also close the file. This is OK since multiple
            // close operations are safe.
            this.setFile(fileName, true, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            errorHandler.error("setFile(" + fileName + ", true) call failed.");
        }
        scheduledFilename = datedFilename;

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

    /**
     * This method differentiates DailyRollingFileAppender from its
     * super class.
     * <p>
     * <p>Before actually logging, this method will check whether it is
     * time to do a rollover. If it is, it will schedule the next
     * rollover time and then rollover.
     */
    protected void subAppend(LoggingEvent event) {
        long n = System.currentTimeMillis();
        if (n >= nextCheck) {
            now.setTime(n);
            nextCheck = rc.getNextCheckMillis(now);
            try {
                rollOver();
            } catch (IOException ioe) {
                if (ioe instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
                LogLog.error("rollOver() failed.", ioe);
            }
        }
        super.subAppend(event);
    }

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