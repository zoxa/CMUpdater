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
	private static String	DB_NAME		= "CMNightlies.db";
	private static int		DB_VERSION	= 1;

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
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
	 * .SQLiteDatabase)
	 */
	@Override
	public void onCreate( SQLiteDatabase sqLiteDatabase )
	{
		// create clog table
		StringBuilder sql = new StringBuilder( "CREATE TABLE " );
		sql.append( NightliesAdapter.CL_TABLE ).append( " (" );
		sql.append( NightliesAdapter.CL_ID ).append( " INTEGER PRIMARY KEY, " );
		sql.append( NightliesAdapter.CL_PROJECT ).append( " TEXT, " );
		sql.append( NightliesAdapter.CL_SUBJECT ).append( " TEXT, " );
		sql.append( NightliesAdapter.CL_LAST_UPDATED ).append( " INTEGER)" );

		sqLiteDatabase.execSQL( sql.toString() );

		// create downloads table
		sql.setLength( 0 );
		sql.append( "CREATE TABLE " ).append( NightliesAdapter.CM_TABLE ).append( " ( " );
		sql.append( NightliesAdapter.CM_FILENAME ).append( " TEXT PRIMARY KEY, " );
		sql.append( NightliesAdapter.CM_TYPE ).append( " TEXT, " );
		sql.append( NightliesAdapter.CM_MD5SUM ).append( " TEXT, " );
		sql.append( NightliesAdapter.CM_SIZE ).append( " TEXT, " );
		sql.append( NightliesAdapter.CM_DATE ).append( " INTEGER) " );

		sqLiteDatabase.execSQL( sql.toString() );
	}

	/*
	 * (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite
	 * .SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade( SQLiteDatabase sqLiteDatabase, int oldV, int newV )
	{
		sqLiteDatabase.execSQL( "DROP TABLE " + NightliesAdapter.CL_TABLE );
		sqLiteDatabase.execSQL( "DROP TABLE " + NightliesAdapter.CM_TABLE );
		onCreate( sqLiteDatabase );
	}
}
