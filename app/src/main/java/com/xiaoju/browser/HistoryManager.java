package com.xiaoju.browser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {

    private static final String DB_NAME = "xiaoju_history.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_HISTORY = "history";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_URL = "url";
    private static final String COL_VISIT_TIME = "visit_time";

    private static final int MAX_HISTORY = 500;

    private DBHelper dbHelper;

    public HistoryManager(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void addHistory(String title, String url) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 更新已有记录
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_URL, url);
        cv.put(COL_VISIT_TIME, System.currentTimeMillis());
        int updated = db.update(TABLE_HISTORY, cv, COL_URL + "=?", new String[]{url});
        if (updated == 0) {
            db.insert(TABLE_HISTORY, null, cv);
        }
        // 超过最大数量时删除最旧的
        trimHistory(db);
    }

    private void trimHistory(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY, null);
        if (c.moveToFirst() && c.getInt(0) > MAX_HISTORY) {
            db.execSQL("DELETE FROM " + TABLE_HISTORY +
                " WHERE " + COL_ID + " IN (SELECT " + COL_ID +
                " FROM " + TABLE_HISTORY +
                " ORDER BY " + COL_VISIT_TIME + " ASC LIMIT " + (c.getInt(0) - MAX_HISTORY) + ")");
        }
        c.close();
    }

    public List<HistoryItem> getAll() {
        List<HistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY,
            new String[]{COL_ID, COL_TITLE, COL_URL, COL_VISIT_TIME},
            null, null, null, null, COL_VISIT_TIME + " DESC");
        while (c.moveToNext()) {
            HistoryItem item = new HistoryItem();
            item.id = c.getLong(0);
            item.title = c.getString(1);
            item.url = c.getString(2);
            item.visitTime = c.getLong(3);
            list.add(item);
        }
        c.close();
        return list;
    }

    public void delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_HISTORY, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
    }

    public static class HistoryItem {
        public long id;
        public String title;
        public String url;
        public long visitTime;
    }

    private static class DBHelper extends SQLiteOpenHelper {
        DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_URL + " TEXT UNIQUE, " +
                COL_VISIT_TIME + " INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
            onCreate(db);
        }
    }
}
