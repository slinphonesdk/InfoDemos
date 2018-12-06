package com.udp.master;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bean.common.DeviceType;
import com.udp.master.model.ExPhoneModel;
import com.udp.master.model.FailedRequestJsonModel;

import java.util.ArrayList;
import java.util.List;

public class MasterDBDao
{
    private static final String TABLE_NAME_LINE = "exphone";//表名
    private static final String TABLE_NAME_FAILED = "failed";//表名

    private static final String ID = "id";
    private static final String SIP = "sip";
    private static final String DeviceTypeInt = "deviceTypeInt";
    private static final String IPAddress = "IPAddress";
    private static final String exPhoneModelStateInt = "exphoneModelStateInt";
    private static final String theLastHeartTime = "theLastHeartTime";
    private static final String offLineTimes = "offLineTimes";
    private static final String jsonStr = "jsonStr";

    private MasterDBHelper dbHelper;

    //创建表结构
    public static final String SQL_CREATE_LINE_TABLE = "create table " + TABLE_NAME_LINE + "(" +
            ID + " integer primary key autoincrement," +
            SIP + " text," +
            DeviceTypeInt + " integer," +
            IPAddress + " text," +
            exPhoneModelStateInt + " integer," +
            theLastHeartTime + " long," +
            offLineTimes + " integer" +
            ")";

    //创建表结构
    public static final String SQL_CREATE_FAILED_TABLE = "create table " + TABLE_NAME_FAILED + "(" +
            ID + " integer primary key autoincrement," +
            jsonStr + " text)";

    private MasterDBDao() {
        dbHelper = new MasterDBHelper(MasterManager.getContext());
    }

    public static MasterDBDao getInstance() {
        return InnerDB.instance;
    }

    private static class InnerDB {
        private static MasterDBDao instance = new MasterDBDao();
    }

    public synchronized<T> void removeExPhoneModel(T bean) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ExPhoneModel exphoneModel = (ExPhoneModel) bean;
        db.delete(TABLE_NAME_LINE, "sip=?", new String[]{exphoneModel.getSip()});
        Log.e("DB", "remove: "+"sip="+exphoneModel.getSip());
    }

    public synchronized<T> void removeFailed(T bean) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        FailedRequestJsonModel failedRequestJsonModel = (FailedRequestJsonModel) bean;
        db.delete(TABLE_NAME_FAILED, "jsonStr=?", new String[]{failedRequestJsonModel.getJsonStr()});
        Log.e("DB", "remove: "+"jsonStr="+failedRequestJsonModel.getJsonStr());
    }

    /**
     * 数据库插入数据
     *
     * @param bean 实体类
     * @param <T>  T
     */
    public synchronized <T> void insertExModel(T bean) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            if (bean != null && bean instanceof ExPhoneModel) {
                ExPhoneModel exphoneModel = (ExPhoneModel) bean;
                ContentValues cv = new ContentValues();
                cv.put(SIP, exphoneModel.getSip());
                cv.put(DeviceTypeInt, exphoneModel.getDeviceType().value());
                cv.put(IPAddress, exphoneModel.getIPAddress());
                cv.put(exPhoneModelStateInt, exphoneModel.getState().value());
                cv.put(theLastHeartTime, exphoneModel.getTheLastHeartTime());
                cv.put(offLineTimes, exphoneModel.getOffLineTimes());
                db.insert(TABLE_NAME_LINE, null, cv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public synchronized <T> void insertFailedModel(T bean) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            if (bean != null && bean instanceof FailedRequestJsonModel) {
                FailedRequestJsonModel failedRequestJsonModel = (FailedRequestJsonModel) bean;
                ContentValues cv = new ContentValues();
                cv.put(jsonStr, failedRequestJsonModel.getJsonStr());
                db.insert(TABLE_NAME_FAILED, null, cv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    /**
     * 删除表中所有的数据
     */
    public synchronized void clearAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "delete from " + TABLE_NAME_LINE;
        String sql2 = "delete from " + TABLE_NAME_FAILED;

        try {
            db.execSQL(sql);
            db.execSQL(sql2);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public synchronized void clearExModels() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "delete from " + TABLE_NAME_LINE;

        try {
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    /**
     * 查询数据
     *
     * @return List
     */
    public synchronized <T> List<T> queryExModels() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<T> list = new ArrayList<>();
        String querySql = "select * from " + TABLE_NAME_LINE;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(querySql, null);
            while (cursor.moveToNext()) {

                String sip = cursor.getString(cursor.getColumnIndex(SIP));
                int typeInt = cursor.getInt(cursor.getColumnIndex(DeviceTypeInt));
                int stateInt = cursor.getInt(cursor.getColumnIndex(exPhoneModelStateInt));
                String ipAddress = cursor.getString(cursor.getColumnIndex(IPAddress));
                ExPhoneModel exphoneModel = new ExPhoneModel(sip, DeviceType.fromInt(typeInt), ipAddress, ExPhoneModel.State.fromInt(stateInt));
                exphoneModel.setOffLineTimes(cursor.getInt(cursor.getColumnIndex(offLineTimes)));
                exphoneModel.setTheLastHeartTime(cursor.getLong(cursor.getColumnIndex(theLastHeartTime)));
                list.add((T) exphoneModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return list;
    }

    public synchronized <T> List<T> queryFailedModels() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<T> list = new ArrayList<>();
        String querySql = "select * from " + TABLE_NAME_FAILED;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(querySql, null);
            while (cursor.moveToNext()) {

                FailedRequestJsonModel failedRequestJsonModel = new FailedRequestJsonModel();
                failedRequestJsonModel.setJsonStr(cursor.getString(cursor.getColumnIndex(jsonStr)));
                list.add((T) failedRequestJsonModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return list;
    }
}
