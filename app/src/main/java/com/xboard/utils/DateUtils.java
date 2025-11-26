package com.xboard.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static final String ALL_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SIMPLE_FORMAT = "yyyy/MM/dd";
    public static final long ONE_SEC = 1000L;
    public static final long ONE_MIN = 60000L;
    public static final long ONE_HOUR = 3600000L;
    public static final long ONE_DAY = 86400000L;

    public DateUtils() {
    }

    public static String getStringTime(String format) {
        String time = "";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.CHINA);
            time = sdf.format(new Date());
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        return time;
    }

    public static String getStringTime() {
        return getStringTime("yyyy-MM-dd HH:mm:ss");
    }

    public static String getStringTime(long time, String format) {
        return (new SimpleDateFormat(format, Locale.CHINA)).format(new Date(time));
    }

    public static String getStringTime(long time) {
        return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)).format(new Date(time));
    }

    public static long getLongTime(String format, String time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            Date date = dateFormat.parse(time);
            return date.getTime();
        } catch (Exception var4) {
            return 0L;
        }
    }

    public static String getSimpleTime(String time) {
        long timeLong = getLongTime("yyyy-MM-dd HH:mm:ss", time);
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        return format.format(new Date(timeLong));
    }
    public static String getDateTime(String time) {
        long timeLong = getLongTime("yyyy-MM-dd", time);
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        return format.format(new Date(timeLong));
    }

    public static String getTime(String time) {
        long timeLong = getLongTime("HH:mm:ss", time);
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        return format.format(new Date(timeLong));
    }
    public static long getLongTimeDefault(String time) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = dateFormat.parse(time);
            return date.getTime();
        } catch (Exception var3) {
            return 0L;
        }
    }

    public static int getMinByMs(long interval) {
        if (interval <= 60000L) {
            return 1;
        } else {
            return interval % 60000L == 0L ? (int)(interval / 60000L) : (int)(interval / 60000L + 1L);
        }
    }
    public static String getStringTimeInCustomFormat(long time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a 'on' MMMM d, yyyy", Locale.ENGLISH);
            return sdf.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public static String getStringTimeInCustomFormatOn(long time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("'On' MMMM d, yyyy 'at' hh:mm a", Locale.ENGLISH);
            return sdf.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public static String getStringTimeInCustomFormatAt(long time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("'at' hh:mm a 'on' MMMM d, yyyy", Locale.ENGLISH);
            return sdf.format(new Date(time));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private static String formatNumber(int number) {
        return number < 10 ? "0" + number : String.valueOf(number);
    }

    public static String formatCouponRemainTime(long interval) {
        String sec = "00";
        if (interval < 0L) {
            interval = 0L;
        }

        String timeStr;
        String min;
        if (interval > 3600000L) {
            String hour = formatNumber((int)(interval / 3600000L));
            min = formatNumber((int)(interval % 3600000L / 60000L));
            timeStr = hour + "时" + min + "分";
        } else {
            min = formatNumber((int)(interval / 60000L));
            sec = formatNumber((int)(interval % 60000L / 1000L));
            timeStr = min + "分" + sec + "秒";
        }

        return timeStr;
    }

}
