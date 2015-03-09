package com.yili.wormhole.common.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class DateHelper
{
  private static final Log s_logger = LogFactory.getLog(DateHelper.class);
  public static final String DATE_FORMAT_PATTERN_YEAR_MONTH = "yyyy-MM";
  public static final String DATE_FORMAT_PATTERN_YEAR_MONTH_DAY = "yyyy-MM-dd";
  public static final String DATE_FORMAT_PATTERN_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";
  
  public static Date rollToZeroAM(Date time)
  {
    if (time == null) {
      return null;
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(time);
    calendar.set(14, 0);
    calendar.set(13, 0);
    calendar.set(12, 0);
    calendar.set(11, 0);
    return calendar.getTime();
  }

  
  public static Date change(Date time, int field, int amount)
  {
    if (time == null) {
      return null;
    }
    try
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(time);
      calendar.add(field, amount);
      return calendar.getTime();
    }
    catch (IllegalArgumentException e)
    {
      s_logger.error("input field " + field + " is illegal!", e);
    }
    return null;
  }
  
  public static Date changeDays(Date time, int numOfDays)
  {
    return change(time, 5, numOfDays);
  }
  
  public static String format(Date time, String pattern)
  {
    if ((time == null) || (pattern == null)) {
      return null;
    }
    try
    {
      DateFormat dateFormat = new SimpleDateFormat(pattern);
      return dateFormat.format(time);
    }
    catch (IllegalArgumentException e)
    {
      s_logger.error("input pattern " + pattern + " is illegal!", e);
    }
    return null;
  }
  
  public static Date parse(String string, String pattern, Date defaultValue)
  {
    try
    {
      DateFormat dateFormat = new SimpleDateFormat(pattern);
      return dateFormat.parse(string);
    }
    catch (ParseException e)
    {
      s_logger.warn("Can't convert " + string + " to Date, use default value " + defaultValue, e);
    }
    catch (IllegalArgumentException e)
    {
      s_logger.error("input pattern " + pattern + " is illegal!", e);
    }
    return defaultValue;
  }
}
