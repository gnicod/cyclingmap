package fr.ovski.ovskimap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

import fr.ovski.ovskimap.models.ORPass;

public class OpenRunnerRouteDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ovskimap.db";


    public static final String SQL_CREATE_TABLE_PASSES = "CREATE TABLE " + ORPasses.TABLE_NAME + "(" +
            ORPasses.TABLE_COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
            ORPasses.TABLE_COLUMN_NAME+" TEXT ," +
            ORPasses.TABLE_COLUMN_LAT+" DOUBLE," +
            ORPasses.TABLE_COLUMN_LNG+" DOUBLE," +
            ORPasses.TABLE_COLUMN_ALT+" INT," +
            ORPasses.TABLE_COLUMN_DESC+" TEXT" +
            ")";
    public static final String SQL_CREATE_TABLE_ROUTES = "CREATE TABLE " + ORRoutes.TABLE_NAME + "(" +
            ORRoutes.TABLE_COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
            ORRoutes.TABLE_COLUMN_NAME+" TEXT ," +
            ORRoutes.TABLE_COLUMN_DOWNLOADED+" TEXT" +
            ")";

    public static class ORRoutes implements BaseColumns {
        //tables names
        public static final String TABLE_NAME = "openrunner_routes";
        public static final String TABLE_COLUMN_ID = "id";
        public static final String TABLE_COLUMN_NAME = "name";
        public static final String TABLE_COLUMN_DOWNLOADED = "downloaded";
    }

    public static class ORPasses implements BaseColumns {
        //tables names
        public static final String TABLE_NAME = "openrunner_passes";
        public static final String TABLE_COLUMN_ID = "id";
        public static final String TABLE_COLUMN_NAME = "name";
        public static final String TABLE_COLUMN_LAT = "lat";
        public static final String TABLE_COLUMN_LNG = "lng";
        public static final String TABLE_COLUMN_ALT = "alt";
        public static final String TABLE_COLUMN_DESC = "desc";
    }

    public OpenRunnerRouteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_ROUTES);
        db.execSQL(SQL_CREATE_TABLE_PASSES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long insertPass(ORPass pass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ORPasses.TABLE_COLUMN_ID, pass.getId());
        values.put(ORPasses.TABLE_COLUMN_NAME, pass.getName());
        values.put(ORPasses.TABLE_COLUMN_LAT, pass.getLat());
        values.put(ORPasses.TABLE_COLUMN_LNG, pass.getLng());
        values.put(ORPasses.TABLE_COLUMN_ALT, pass.getAlt());
        values.put(ORPasses.TABLE_COLUMN_DESC, pass.getDesc());
        // insert row
        long pass_id = db.insert(ORPasses.TABLE_NAME, null, values);
        return pass_id;
    }

    public ORPass getPass(int id ) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + ORPasses.TABLE_NAME + "WHERE ID = " + id;
        Cursor c = db.rawQuery(selectQuery, null);
        if (c != null)
            c.moveToFirst();

        ORPass pass = new ORPass(
                c.getInt(c.getColumnIndex(ORPasses.TABLE_COLUMN_ID)),
                c.getString(c.getColumnIndex(ORPasses.TABLE_COLUMN_NAME)),
                c.getDouble(c.getColumnIndex(ORPasses.TABLE_COLUMN_LAT)),
                c.getDouble(c.getColumnIndex(ORPasses.TABLE_COLUMN_LNG)),
                c.getInt(c.getColumnIndex(ORPasses.TABLE_COLUMN_ALT)),
                c.getString(c.getColumnIndex(ORPasses.TABLE_COLUMN_DESC))
                );
        return pass;
    }

    /*
 * getting all todos under single tag
 * */
    public List<ORPass> getAllPasses() {
        List<ORPass> passes = new ArrayList<ORPass>();
        String selectQuery = "SELECT  * FROM " + ORPasses.TABLE_NAME ;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                ORPass pass = new ORPass(
                        c.getInt(c.getColumnIndex(ORPasses.TABLE_COLUMN_ID)),
                        c.getString(c.getColumnIndex(ORPasses.TABLE_COLUMN_NAME)),
                        c.getDouble(c.getColumnIndex(ORPasses.TABLE_COLUMN_LAT)),
                        c.getDouble(c.getColumnIndex(ORPasses.TABLE_COLUMN_LNG)),
                        c.getInt(c.getColumnIndex(ORPasses.TABLE_COLUMN_ALT)),
                        c.getString(c.getColumnIndex(ORPasses.TABLE_COLUMN_DESC))
                );
                passes.add(pass);
            } while (c.moveToNext());
        }

        return passes;
    }
}
