package com.bean.common;

import android.app.AlarmManager;
import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SimpleDate
{

    public static class Sys {

        public static void setDateTime(Context mContext, long temp) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
            long lt = temp;
            Date date = new Date(lt);
            int year = Integer.parseInt(simpleDateFormat.format(date));

            simpleDateFormat = new SimpleDateFormat("MM");
            int month = Integer.parseInt(simpleDateFormat.format(date));

            simpleDateFormat = new SimpleDateFormat("dd");
            int day = Integer.parseInt(simpleDateFormat.format(date));

            simpleDateFormat = new SimpleDateFormat("HH");
            int hour = Integer.parseInt(simpleDateFormat.format(date));

            simpleDateFormat = new SimpleDateFormat("mm");
            int minute = Integer.parseInt(simpleDateFormat.format(date));

            setDate(mContext, year, month, day);
            setTime(mContext, hour, minute);
        }


        /*
        * 设置系统日期
        * */
        public static void setDate(Context mContext, int year, int month, int day) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month-- == -1 ? 12 : month);
            c.set(Calendar.DAY_OF_MONTH, day);
            long when = c.getTimeInMillis();
            if (when / 1000 < Integer.MAX_VALUE) {
                ((AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE)).setTime(when);
            }
        }

        /*
        * 设置系统时间
        * */
        public static void setTime(Context mContext, int hour, int minute) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long when = c.getTimeInMillis();
            if (when / 1000 < Integer.MAX_VALUE) {
                ((AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE)).setTime(when);
            }
        }
    }

    public static String getCurrentDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }
    public static String getCurrentDateNu() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    /*
     * 将时间转换为时间戳
     */
    public static String dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }


    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(String s){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }
}
