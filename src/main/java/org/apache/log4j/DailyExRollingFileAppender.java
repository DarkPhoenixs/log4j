package org.apache.log4j;

import org.apache.log4j.helpers.LogLog;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

/**
 * <p>Title: DailyExRollingFileAppender</p>
 * <p>Description: DailyRollingFileAppender with maxBackupIndex</p>
 *
 * @author Victor
 * @version 1.0
 * @see DailyRollingFileAppender
 * @since 2017/8/31
 */
public class DailyExRollingFileAppender extends DailyRollingFileAppender {

    private int maxBackupIndex = 1;

    public int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    public void setMaxBackupIndex(int maxBackupIndex) {
        this.maxBackupIndex = maxBackupIndex;
    }

    @Override
    void rollOver() throws IOException {

        super.rollOver();

        File file = new File(fileName);

        //获取日志文件列表，控制数量，实现清理策略
        if (file.getParentFile().exists()) {
            File[] files = file.getParentFile().listFiles(new LogFileFilter(file.getName()));
            Long[] dateArray = new Long[files.length];
            for (int i = 0; i < files.length; i++) {
                File fileItem = files[i];
                String fileDateStr = fileItem.getName().replace(file.getName(), "");
                try {
                    Date filedate = sdf.parse(fileDateStr);
                    long fileDateLong = filedate.getTime();
                    dateArray[i] = fileDateLong;
                } catch (ParseException e) {
                    LogLog.error("Parse File Date Throw Exception : " + e.getMessage());
                }
            }

            Arrays.sort(dateArray);

            if (dateArray.length > maxBackupIndex) {
                for (int i = 0; i < dateArray.length - maxBackupIndex; i++) {
                    String dateFileName = file.getPath() + sdf.format(dateArray[i]);
                    File dateFile = new File(dateFileName);
                    if (dateFile.exists()) {
                        dateFile.delete();
                    }
                }
            }
        }

    }
}

class LogFileFilter implements FileFilter {

    private String logName;

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