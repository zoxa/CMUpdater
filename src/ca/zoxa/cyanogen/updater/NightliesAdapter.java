package ca.zoxa.cyanogen.updater;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class NightliesAdapter
{
	/**
	 * Change log table
	 */
	public static String	CL_TABLE		= "clog";
	public static String	CL_ID			= "id";
	public static String	CL_PROJECT		= "project";
	public static String	CL_SUBJECT		= "subject";
	public static String	CL_LAST_UPDATED	= "last_updated";

	public static String[]	CL_FIELDS		= { CL_ID, CL_PROJECT, CL_SUBJECT, CL_LAST_UPDATED };

	/**
	 * Downloads table
	 */
	public static String	CM_TABLE		= "downloads";
	public static String	CM_TYPE			= "type";
	public static String	CM_FILENAME		= "filename";
	public static String	CM_MD5SUM		= "md5sum";
	public static String	CM_SIZE			= "size";
	public static String	CM_DATE			= "date_added";

	public static String[]	CM_FIELDS		= { CM_TYPE, CM_FILENAME, CM_MD5SUM, CM_SIZE, CM_DATE };

	/**
	 * Limits
	 */
	private final int		CM_LIMITS		= 10;

	/**
	 * Private properties
	 */
	private final Context	context;
	private SQLiteDatabase	database;
	private CMDBHandler		dbHelper;

	public NightliesAdapter( Context context )
	{
		this.context = context;
	}

	@Override
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

	public NightliesAdapter read() throws SQLException
	{
		dbHelper = new CMDBHandler( context );
		database = dbHelper.getReadableDatabase();
		return this;
	}

	public void close()
	{
		dbHelper.close();
	}

	public void cleanup()
	{
		// TODO: Add cleanup logic here
		// Each refresh will fill last 20 nightlies for downloads.
		// Cleanup logic will be the following, delete any downloads more than 20 and all related
		// changes
	}

	/* FUNCTIONS FOR CHANGE LOG TABLE */
	/**
	 * Generate Change log record for db
	 * 
	 * @param id
	 * @param project
	 * @param subject
	 * @param last_updated
	 * @return ContentValues
	 */
	private ContentValues createCLContent( final int id, final String project,
			final String subject, final long last_updated )
	{
		ContentValues values = new ContentValues();
		values.put( CL_ID, id );
		values.put( CL_PROJECT, project );
		values.put( CL_SUBJECT, subject );
		values.put( CL_LAST_UPDATED, last_updated );
		return values;
	}

	/**
	 * Add Change log record into data base, overwrite on conflict
	 * 
	 * @param id
	 * @param project
	 * @param subject
	 * @param last_updated
	 * @return boolean
	 */
	public boolean addChangeLog( final int id, final String project, final String subject,
			final long last_updated )
	{
		ContentValues initialValues = createCLContent( id, project, subject, last_updated );

		// FIXME: change to return insert or update
		long res = database.insertWithOnConflict( CL_TABLE, null, initialValues,
				SQLiteDatabase.CONFLICT_REPLACE );
		return ( res != -1 );
	}

	/* END: FUNCTIONS FOR CHANGE LOG TABLE */

	/* FUNCTIONS FOR DOWNLOADS TABLE */
	/**
	 * Generate Downloads record for db
	 * 
	 * @param filename
	 * @param type
	 * @param md5sum
	 * @param size
	 * @param date_added
	 * @return ContentValues
	 */
	private ContentValues createCMContent( final String filename, final String type,
			final String md5sum, final String size, final long date_added )
	{
		ContentValues values = new ContentValues();
		values.put( CM_FILENAME, filename );
		values.put( CM_TYPE, type );
		values.put( CM_MD5SUM, md5sum );
		values.put( CM_SIZE, size );
		values.put( CM_DATE, date_added );
		return values;
	}

	/**
	 * Add / replace downloads record into db
	 * 
	 * @param filename
	 * @param type
	 * @param md5sum
	 * @param size
	 * @param date_added
	 * @return boolean
	 */
	public boolean addDownlods( final String filename, final String type, final String md5sum,
			final String size, final long date_added )
	{
		ContentValues initialValues = createCMContent( filename, type, md5sum, size, date_added );

		// FIXME: change to return insert or update
		long res = database.insertWithOnConflict( CM_TABLE, null, initialValues,
				SQLiteDatabase.CONFLICT_REPLACE );
		return ( res != -1 );
	}

	/* END: FUNCTIONS FOR CHANGE LOG TABLE */

	/**
	 * Get Downloads Cursor
	 * 
	 * @return
	 */
	public Cursor getDownloadsCursor()
	{
		return database.query( CM_TABLE, CM_FIELDS, null, null, null, null, CM_FILENAME + " DESC",
				String.valueOf( CM_LIMITS ) );
	}

	/**
	 * Get Change Log Cursor
	 * 
	 * @return
	 */
	public Cursor getChangeLogCursor( long dateFrom, long dateTo )
	{
		return database.query( CL_TABLE, CL_FIELDS, CL_LAST_UPDATED + " BETWEEN ? AND ?",
				new String[] { String.valueOf( dateFrom ), String.valueOf( dateTo ) }, null, null,
				CL_LAST_UPDATED + " DESC" );
	}
}
