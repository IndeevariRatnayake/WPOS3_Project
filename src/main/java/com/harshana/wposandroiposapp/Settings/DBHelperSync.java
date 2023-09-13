package com.harshana.wposandroiposapp.Settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.harshana.wposandroiposapp.Utilities.Utility;

public class DBHelperSync extends SQLiteOpenHelper {
    private static final String DBName = "DBConfig_Sync.db";

    SQLiteDatabase db;
    Context appContext;
    String dbPath = "";
    private static DBHelperSync instace = null;

    public String getDbPath()
    {
        return dbPath;
    }

    public String getDBName()
    {
        return DBName;
    }

    public DBHelperSync(Context context) {
        super(context, DBName, null, 2);
        dbPath = context.getDatabasePath(DBName).getPath();
        appContext = context;
        db = this.getWritableDatabase();
    }

    public static DBHelperSync getInstance(Context context) {
        if (instace == null)
            instace = new DBHelperSync(context);
        return instace;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    private static final String DB_ZIPPED_FILE = "DB.zip";

    public void prepareInitialDatabase() throws Exception {
        Preferences pref = Preferences.getInstance(appContext);

        String path = "";
        if (pref.isInitialLoading()) {//yes this is an initial loading
           //get the terminal database path
           path =  appContext.getDatabasePath(DBName).getParent();

           if (Utility.unzipFromAsset(appContext,DB_ZIPPED_FILE,path))
               Log.e("TTTTTTTTTTTTT","TTTTTTTTTTTTTTTTTT");
           else
               throw new  Exception("Transferring databases failed");
        }
    }

    public boolean insertRecords(String tableName,ContentValues values) {
        long result = 0;
        try {
            result = db.insert(tableName,null,values);
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public Cursor readWithCustomQuary(String quary) {
        try {
            Cursor result = db.rawQuery(quary,null);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean executeCustomQuary(String quary) {
        try {
            db.execSQL(quary);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}