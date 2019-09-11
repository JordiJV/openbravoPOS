package com.openbravo.pos.util;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

//jjuanmarti
public class CustomFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        //StringBuffer buffer = new StringBuffer();
        //buffer.append(record.getMessage());
        //return record.toString();
        return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %5$s%6$s%n", new Date(), record.getSourceClassName(), record.getLoggerName(), record.getLevel(), record.getMessage(), record.getThrown()==null?"":record.getThrown());
    }

}