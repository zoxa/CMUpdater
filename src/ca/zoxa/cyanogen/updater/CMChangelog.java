package ca.zoxa.cyanogen.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CMChangelog implements Runnable
{
	// Device code name
	private final String		device;
	private final Context		context;

	// JSON CM change log
	private final String		URL_CHANGELOG_JSON		= "http://cm-nightlies.appspot.com/changelog/?device=%s";

	// List of nightlies
	private final String		URL_NIGHTLIES			= "http://download.cyanogenmod.com/?device=%s";

	// Handler
	private Handler				handler;

	// Tag for the logs
	private static final String	TAG						= "CMCLog";

	// Error codes
	public static int			ERROR_CODE_DB_OPEN		= 1;
	public static int			ERROR_CODE_HTTP_FAIL	= ERROR_CODE_DB_OPEN + 1;
	public static int			ERROR_CODE_NO_HOST		= ERROR_CODE_HTTP_FAIL + 1;
	public static int			ERROR_CODE_JSON_PARSE	= ERROR_CODE_NO_HOST + 1;

	// Error Messages keys
	public static String		MSG_DATA_KEY_ERROR		= "ERROR";
	public static String		MSG_DATA_KEY_ERROR_MSG	= "ERROR_MSG";

	// Positive Message keys
	public static String		MSG_JSON_COUNT			= "RESULT_JSON_COUNT";
	public static String		MSG_DOWNLOADS_COUNT		= "RESULT_DOWNLOADS_COUNT";

	public CMChangelog( final Context context, final String device )
	{
		this.context = context;
		this.device = device;
	}

	/**
	 * Refresh and write changes to db
	 */
	public void refreshChangelog( final Handler handler )
	{
		this.handler = handler;

		Thread thread = new Thread( this );
		thread.start();
	}

	public void run()
	{
		Bundle data = new Bundle();
		NightliesAdapter na = null;

		try
		{
			// open db connection
			na = new NightliesAdapter( this.context );
			na.open();
		}
		catch ( SQLException e )
		{
			// Failed creating db connection
			Log.e( TAG, "SQLException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_DB_OPEN );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}

		if ( na != null )
		{
			// update changelog information
			data = updateChangeLog( data, na );

			// pull latest available zip to download
			data = updateZipList( data, na );

			// remove old records
			na.cleanup();
			na.close();
		}

		// return request to handler
		Message msg = new Message();
		msg.setData( data );
		handler.sendMessage( msg );
	}

	/* UPDATE JSON change log */
	/**
	 * Run change log updater: Call json and parse it and Save changes into db
	 * 
	 * @param data
	 * @param na
	 * @return
	 */
	private Bundle updateChangeLog( Bundle data, NightliesAdapter na )
	{
		JSONArray json = null;

		// get JSON data from cm-nightlies.appspot.com
		try
		{
			json = getChangelogJSON();
		}
		catch ( ClientProtocolException e )
		{
			// ClientProtocolException in case of an http protocol error
			Log.e( TAG, "ClientProtocolException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( UnsupportedEncodingException e )
		{
			// Specified unsupported encoding, should not happen
			Log.e( TAG, "UnsupportedEncodingException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( IllegalStateException e )
		{
			// Entity is empty or was previously pulled oO
			Log.e( TAG, "IllegalStateException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( JSONException e )
		{
			// Cannot parse response
			Log.e( TAG, "JSONException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_JSON_PARSE );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( IOException e )
		{
			// IOException in case of a problem or the connection was aborted
			Log.e( TAG, "IOException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_NO_HOST );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}

		if ( json != null )
		{
			try
			{
				int result = saveChangeLog( na, json );
				data.putInt( MSG_JSON_COUNT, result );
				Log.i( TAG, "Records Updated: " + result );
			}
			catch ( JSONException e )
			{
				// JSONException problem parsing
				Log.e( TAG, "SQLException", e );
				data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_JSON_PARSE );
				data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
			}
		}

		return data;
	}

	/**
	 * Do HTTP call for JSON and get JSONArray
	 * 
	 * @return JSONArray parsed change log
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws JSONException
	 */
	private JSONArray getChangelogJSON() throws ClientProtocolException, IllegalStateException,
			UnsupportedEncodingException, JSONException, IOException
	{
		// build request
		HttpGet httpget = new HttpGet( String.format( URL_CHANGELOG_JSON, Uri.encode( device ) ) );
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute( httpget );

		// FIXME: I think we need to make sure that response code
		// is around >= 200 && < 400
		Log.i( TAG, "Status:[" + response.getStatusLine().getStatusCode() + "]" );
		Log.i( TAG, "Status:[" + response.getStatusLine().toString() + "]" );

		HttpEntity entity = response.getEntity();
		if ( entity != null )
		{
			InputStream instream = entity.getContent();
			String result = CMChangelog.convertStreamToString( instream );
			instream.close();
			Log.i( TAG, "--- Result.length: [" + result.length() + "]" );

			if ( result.length() > 0 )
			{
				Log.i( TAG, "Start JSON Parsing..." );
				JSONArray json = new JSONArray( result );
				Log.i( TAG, "JSON Parsed..." );
				return json;
			}
		}

		return null;
	}

	/**
	 * Save pulled JSON array into db
	 * 
	 * @param na
	 *            NightliesAdapter opened connection
	 * @param json
	 *            JSONArray pulled and parse change log record
	 * @return number of changes
	 * @throws JSONException
	 */
	private int saveChangeLog( NightliesAdapter na, JSONArray json ) throws JSONException
	{
		int res = 0;
		// need to parse "2011-10-29 06:41:01.000000000"
		SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSSSSSSSS" );
		for ( int i = 0; i < json.length(); i++ )
		{
			JSONObject rec = json.getJSONObject( i );
			Log.i( TAG, "Processing JSON Object: " + rec.toString( 2 ) );
			long last_update;
			try
			{
				Date d = format.parse( rec.getString( "last_updated" ) );
				last_update = d.getTime();
			}
			catch ( ParseException e )
			{
				Log.d( TAG, "Exception: " + e.getMessage() );
				last_update = 0;
			}
			if ( na.addChangeLog( rec.getInt( "id" ), rec.getString( "project" ),
					rec.getString( "subject" ), last_update ) )
			{
				res++;
				Log.i( TAG, "Record Saved to DB" );
			}
		}
		return res;
	}

	/* END: UPDATE JSON change log */

	/* UPDATE Downloads list */
	/**
	 * Do HTTP call to pull html.table dom, and save this infomration into db
	 * 
	 * @param data
	 * @param na
	 * @return
	 */
	private Bundle updateZipList( Bundle data, NightliesAdapter na )
	{
		String table = null;

		try
		{
			table = getListofDownloads();
		}
		catch ( ClientProtocolException e )
		{
			// ClientProtocolException in case of an http protocol error
			Log.e( TAG, "ClientProtocolException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( IllegalStateException e )
		{
			// Entity is empty or was previously pulled oO
			Log.e( TAG, "IllegalStateException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( UnsupportedEncodingException e )
		{
			// Specified unsupported encoding, should not happen
			Log.e( TAG, "UnsupportedEncodingException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}
		catch ( IOException e )
		{
			// IOException in case of a problem or the connection was aborted
			Log.e( TAG, "IOException", e );
			data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_NO_HOST );
			data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
		}

		if ( table != null && table.length() > 0 )
		{
			// writ to DB
			int result = saveDownloads( na, table );
			data.putInt( MSG_DOWNLOADS_COUNT, result );
			Log.i( TAG, "Records Updated: " + result );
		}

		return data;
	}

	/**
	 * Do HTML call and return table portion of it
	 * 
	 * @return
	 * @throws ClientProtocolException
	 * @throws IllegalStateException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private String getListofDownloads() throws ClientProtocolException, IllegalStateException,
			UnsupportedEncodingException, IOException
	{
		// build request
		HttpGet httpget = new HttpGet( String.format( URL_NIGHTLIES, Uri.encode( device ) ) );
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute( httpget );

		// FIXME: I think we need to make sure that response code
		// is around >= 200 && < 400
		Log.i( TAG, "Status:[" + response.getStatusLine().getStatusCode() + "]" );
		Log.i( TAG, "Status:[" + response.getStatusLine().toString() + "]" );

		HttpEntity entity = response.getEntity();
		if ( entity != null )
		{
			// convert InputStream into String
			InputStream instream = entity.getContent();
			String result = CMChangelog.convertStreamToString( instream );
			instream.close();
			Log.d( TAG, "--- Result.length: [" + result.length() + "]" );

			// get table from result
			int table_start = result.indexOf( "<table" );
			int table_end = result.indexOf( ">", result.indexOf( "</table>", table_start ) );
			result = result.substring( table_start, table_end );
			Log.d( TAG, "--- Table.length: [" + result.length() + "]" );
			return result;
		}

		return null;
	}

	/**
	 * Parse HTML.Table from string and save it into data base
	 * 
	 * @param na
	 * @param table
	 * @return
	 */
	private int saveDownloads( NightliesAdapter na, String table )
	{
		int res = 0, pos = 0, _pos = 0;
		String td;
		SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

		// find beginning of the table
		while ( -1 != ( pos = table.indexOf( "<tr>", pos ) ) )
		{
			Log.d( TAG, "Found <tr> at " + pos );
			try
			{
				String type = null, filename = null, md5sum = null, size = null;
				Date date_added = null;

				// try to parse td
				// we know that we have 5 columns. lets loop via tds
				for ( int i = 0; i < 5; i++ )
				{
					pos = table.indexOf( "<td>", pos ) + 4;
					Log.d( TAG, "Found <td> at " + pos );
					td = table.substring( pos, ( pos = table.indexOf( "</td>", pos ) ) );
					Log.d( TAG, "Got td " + td );

					// each td has its own parse rulez
					switch ( i )
					{
						case 1:
							// type: nightly
							type = td.trim();
							break;
						case 2:
							// filename format: cm_***.zip
							_pos = td.indexOf( ">cm_" ) + 1;
							filename = td.substring( _pos, td.indexOf( ".zip", _pos ) + 4 ).trim();

							// md5sum: ****
							_pos = td.indexOf( "md5sum:" ) + 8;
							md5sum = td.substring( _pos, td.indexOf( "<", _pos ) ).trim();
							break;
						case 3:
							// size remove small tag
							size = td.replaceAll( "(</?small>)", "" ).trim();
							break;
						case 4:
							// added date: remove small tag
							date_added = format.parse( td.replaceAll( "(</?small>)", "" ).trim() );
							break;

						case 0:
							// device: we already know skip it
						default:
							// unknown td, skip it
							break;
					}
				}

				Log.i( TAG, "Record: type " + type + " filename " + filename );
				Log.i( TAG, " md5sum " + md5sum + " size " + size );
				Log.i( TAG, " Date " + date_added.toString() );
				// we have all the data now, lets try to save it
				if ( na.addDownlods( filename, type, md5sum, size, date_added.getTime() ) )
				{
					res++;
					Log.i( TAG, "Record Saved to DB" );
				}
			}
			catch ( ParseException e )
			{
				Log.e( TAG, "ParseException", e );
			}
		}
		return res;
	}

	/* END: UPDATE Downloads list */

	/**
	 * Function to convert InputStream into String
	 * 
	 * @param is
	 *            InputStream
	 * @return String
	 * @throws UnsupportedEncodingException
	 */
	private static String convertStreamToString( InputStream is )
			throws UnsupportedEncodingException
	{
		BufferedReader reader = new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
		StringBuilder sb = new StringBuilder();
		String line = null;
		try
		{
			while ( ( line = reader.readLine() ) != null )
			{
				sb.append( line + "\n" );
			}
		}
		catch ( IOException e )
		{
			Log.d( TAG, "Exception: " + e.getMessage() );
		}
		finally
		{
			try
			{
				is.close();
			}
			catch ( IOException e )
			{
				Log.d( TAG, "Exception: " + e.getMessage() );
			}
		}
		return sb.toString();
	}
}
