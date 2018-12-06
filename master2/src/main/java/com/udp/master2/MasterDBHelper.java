package com.udp.master2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MasterDBHelper extends SQLiteOpenHelper
{
    private static final String DB_NAME = "master.db";
    private static final int DB_VERSION = 1;

    MasterDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(MasterDBDao.SQL_CREATE_LINE_TABLE);
        db.execSQL(MasterDBDao.SQL_CREATE_FAILED_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
