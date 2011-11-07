package ca.zoxa.cyanogen.updater;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class NightliesAdapter
{
    /**
     * Change log table
     */
    public static String CL_TABLE = "clog";
    public static String CL_ID = "id";
    public static String CL_PROJECT = "project";
    public static String CL_LAST_UPDATED = "last_updated";
    public static String CL_SUBJECT = "subject";

    /**
     * Downloads table
     */
    public static String CM_TABLE = "downloads";
    public static String CM_ID = "_id";
    public static String CM_TYPE = "type";
    public static String CM_FILENAME = "filename";
    public static String CM_MD5SUM = "md5sum";
    public static String CM_SIZE = "size";
    public static String CM_DATE = "date_added";
    
    /**
     * Private properties
     */
    private Context context;
    private SQLiteDatabase database;
    private CMDBHandler dbHelper;

    public NightliesAdapter( Context context )
    {
        this.context = context;
    }

    protected void finalize()
    {
        close();
    }

    public NightliesAdapter open() throws SQLException
    {
        dbHelper = new CMDBHandler( context );
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close()
    {
        dbHelper.close();
    }

    /* FUNCTIONS FOR CHANGE LOG TABLE */
    /**
     * Generate Change log record for db
     * 
     * @param ID
     * @param PROJECT
     * @param SUBJECT
     * @param LAST_UPDATED
     * @return boolean
     */
    private ContentValues createCLContent( final int ID, final String PROJECT,
            final String SUBJECT, final long LAST_UPDATED )
    {
        ContentValues values = new ContentValues();
        values.put( CL_ID, ID );
        values.put( CL_PROJECT, PROJECT );
        values.put( CL_SUBJECT, SUBJECT );
        values.put( CL_LAST_UPDATED, LAST_UPDATED );
        return values;
    }

    /**
     * Add Change log record into data base, overwrite on conflict
     * 
     * @param ID
     * @param PROJECT
     * @param SUBJECT
     * @param LAST_UPDATED
     * @return boolean
     */
    public boolean addChangeLog( final int ID, final String PROJECT, final String SUBJECT,
            final long LAST_UPDATED )
    {
        ContentValues initialValues = createCLContent( ID, PROJECT, SUBJECT, LAST_UPDATED );

        long res = database.insertWithOnConflict( CL_TABLE, null, initialValues,
                SQLiteDatabase.CONFLICT_REPLACE );
        return (res != -1);
    }

    /* END: FUNCTIONS FOR CHANGE LOG TABLE */
}
