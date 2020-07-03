package com.example.contact_tracing;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    final static String TB1="history";
    final static String TB3="online";
    private final static int VS=1;
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, VS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //接觸史
        String SQL1 = "CREATE TABLE IF NOT EXISTS "+ TB1 +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT , " +
                "user_id VARCHAR(15) NOT NULL , " +
                "time_first DATETIME NOT NULL , "+
                "time_last DATETIME NOT NULL , "+
                "rssi_level_1 INTEGER NOT NULL , "+
                "rssi_level_2 INTEGER NOT NULL , "+
                "rssi_level_3 INTEGER NOT NULL ," +
                "is_contact INTEGER )";  //is_contact 0:沒接觸 ； 1:有接觸 ； 2:已回傳




        //上線紀錄
        String SQL3 = "CREATE TABLE IF NOT EXISTS "+ TB3 +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT , " +
                "time_first DATETIME NOT NULL ," +
                "time_last DATETIME )";

        db.execSQL(SQL1);
        db.execSQL(SQL3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL = "DROP TABLE " + TB1;
        db.execSQL(SQL);
    }
}
