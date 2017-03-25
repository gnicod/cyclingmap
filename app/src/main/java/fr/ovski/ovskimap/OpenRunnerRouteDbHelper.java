package fr.ovski.ovskimap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class OpenRunnerRouteDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ovskimap.db";
    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + OpenRunnerEntry.TABLE_NAME + "(" +
            OpenRunnerEntry.TABLE_COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
            OpenRunnerEntry.TABLE_COLUMN_NAME+" TEXT ," +
            OpenRunnerEntry.TABLE_COLUMN_DOWNLOADED+" TEXT" +
            ")";

    public static class OpenRunnerEntry implements BaseColumns {
        public static final String TABLE_NAME = "openrunner_routes";
        public static final String TABLE_COLUMN_ID = "id";
        public static final String TABLE_COLUMN_NAME = "name";
        public static final String TABLE_COLUMN_DOWNLOADED = "downloaded";
    }

    public OpenRunnerRouteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
