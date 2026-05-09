package com.xiaoju.browser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {

    private static final String DB_NAME = "xiaoju.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_URL = "url";
    private static final String COL_SORT_ORDER = "sort_order";
    private static final String COL_CREATE_TIME = "create_time";

    private DBHelper dbHelper;

    public BookmarkManager(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void addBookmark(String title, String url) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 检查是否已存在
        Cursor c = db.query(TABLE_BOOKMARKS, new String[]{COL_ID},
            COL_URL + "=?", new String[]{url}, null, null, null);
        if (c.getCount() > 0) {
            c.close();
            return; // 已存在，不重复添加
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_URL, url);
        cv.put(COL_SORT_ORDER, System.currentTimeMillis());
        cv.put(COL_CREATE_TIME, System.currentTimeMillis());
        db.insert(TABLE_BOOKMARKS, null, cv);
    }

    public List<BookmarkItem> getAll() {
        List<BookmarkItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_BOOKMARKS,
            new String[]{COL_ID, COL_TITLE, COL_URL, COL_SORT_ORDER},
            null, null, null, null, COL_SORT_ORDER + " ASC");
        while (c.moveToNext()) {
            BookmarkItem item = new BookmarkItem();
            item.id = c.getLong(0);
            item.title = c.getString(1);
            item.url = c.getString(2);
            item.sortOrder = c.getLong(3);
            list.add(item);
        }
        c.close();
        return list;
    }

    public void delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_BOOKMARKS, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void updateSortOrder(long id, long sortOrder) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SORT_ORDER, sortOrder);
        db.update(TABLE_BOOKMARKS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public static class BookmarkItem {
        public long id;
        public String title;
        public String url;
        public long sortOrder;
    }

    private static class DBHelper extends SQLiteOpenHelper {
        DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_BOOKMARKS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_URL + " TEXT UNIQUE, " +
                COL_SORT_ORDER + " INTEGER, " +
                COL_CREATE_TIME + " INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
            onCreate(db);
        }
    }
}
