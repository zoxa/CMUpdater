/**
 * 
 */
package ca.zoxa.cyanogen.updater;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * @author zoxa
 */
public class CMDBHandler extends SQLiteOpenHelper
{
    /**
     * DB Params
     */
    private static String DB_NAME = "CMNightlies.db";
    private static int DB_VERSION = 1;

    /**
     * Simplest constructor
     * 
     * @param context
     */

    public CMDBHandler( Context context )
    {
        super( context, DB_NAME, null, DB_VERSION );
    }

    /**
     * Default constructor
     * 
     * @param context
     * @param name
     * @ignored
     * @param factory
     * @param version
     * @ignored
     */
    public CMDBHandler( Context context, String name, CursorFactory factory, int version )
    {
        super( context, DB_NAME, factory, DB_VERSION );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
     * .SQLiteDatabase)
     */
    @Override
    public void onCreate( SQLiteDatabase sqLiteDatabase )
    {
        String sql = "CREATE TABLE " + NightliesAdapter.CL_TABLE + " (" + NightliesAdapter.CL_ID
                + " INTEGER primary key, " + NightliesAdapter.CL_PROJECT + " TEXT, "
                + NightliesAdapter.CL_SUBJECT + " TEXT, " + NightliesAdapter.CL_LAST_UPDATED
                + " INTEGER)";

        sqLiteDatabase.execSQL( sql );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite
     * .SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade( SQLiteDatabase sqLiteDatabase, int oldV, int newV )
    {
        sqLiteDatabase.execSQL( "DROP TABLE " + NightliesAdapter.CL_TABLE );
        onCreate( sqLiteDatabase );
    }
}
