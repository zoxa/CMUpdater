package ca.zoxa.cyanogen.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private final String device;
    private final Context context;

    // JSON CM change log
    private final String URL_CHANGELOG_JSON = "http://cm-nightlies.appspot.com/changelog/?device=%s";

    // List of nightlies
    private final String URL_NIGHTLIES = "http://download.cyanogenmod.com/?device=%s";

    // Handler
    private Handler handler;

    // Tag for the logs
    private static final String TAG = "CMCLog";

    // Error codes
    public static int ERROR_CODE_HTTP_FAIL = 1;
    public static int ERROR_CODE_NO_HOST = ERROR_CODE_HTTP_FAIL + 1;
    public static int ERROR_CODE_JSON_PARSE = ERROR_CODE_NO_HOST + 1;
    public static int ERROR_CODE_DB = ERROR_CODE_JSON_PARSE + 1;

    // Messages keys
    public static String MSG_DATA_KEY_ERROR = "ERROR";
    public static String MSG_DATA_KEY_ERROR_MSG = "ERROR_MSG";
    public static String MSG_JSON_COUNT = "RESULT_JSON_COUNT";
    public static String MSG_DOWNLOADS_COUNT = "RESULT_DOWNLOADS_COUNT";

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

    @Override
    public void run()
    {
        Bundle data = new Bundle();

        // update changelog information
        // data = updateChangeLog( data );

        // pull latest available zip to download
        // previous update didn't cause any error
        if ( data.getInt( MSG_DATA_KEY_ERROR, 0 ) == 0 )
        {
            data = updateZipList( data );
        }

        // return request to handler
        Message msg = new Message();
        msg.setData( data );
        handler.sendMessage( msg );
    }

    /**
     * Update Change log records
     * 
     * @access private
     * @param data
     *            Bundle
     * @return Bundle
     */
    private Bundle updateChangeLog( Bundle data )
    {
        JSONArray json = null;

        try
        {
            // get JSON data from cm-nightlies.appspot.com
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
            // Specified un supported encoding, should not happen
            Log.e( TAG, "UnsupportedEncodingException", e );
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
                // write to DB
                int result = saveChangeLog( json );
                data.putInt( MSG_JSON_COUNT, result );
                Log.i( TAG, "Records Updated: " + result );
            }
            catch ( SQLException e )
            {
                // SQLException Problem with connecting to db
                Log.e( TAG, "SQLException", e );
                data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_DB );
                data.putString( MSG_DATA_KEY_ERROR_MSG, e.getMessage() );
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

    private JSONArray getChangelogJSON() throws ClientProtocolException, IOException,
            UnsupportedEncodingException, JSONException
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

    private int saveChangeLog( JSONArray json ) throws JSONException
    {
        int res = 0;
        NightliesAdapter na = new NightliesAdapter( this.context );
        na.open();
        Log.i( TAG, "Create DB and get connection" );
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
            if ( na.addChangeLog( rec.getInt( "id" ), rec.getString( "project" ), rec
                    .getString( "subject" ), last_update ) )
            {
                res++;
                Log.i( TAG, "Record Saved to DB" );
            }
        }
        na.close();
        return res;
    }

    /* END OF CHANGELOG */

    private static String convertStreamToString( InputStream is )
            throws UnsupportedEncodingException
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
        StringBuilder sb = new StringBuilder();
        String line = null;
        try
        {
            while ( (line = reader.readLine()) != null )
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

    /**
     * Connect to download zip list and get the latest ones (with posted date)
     * 
     * @param data
     * @return
     */
    private Bundle updateZipList( Bundle data )
    {
        List<CMDownloadableRecord> downloads = null;
        try
        {
            downloads = getListofDownloads();

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
            // Specified un supported encoding, should not happen
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

        if ( downloads != null && downloads.size() > 0 )
        {
            // writ to DB
            int result = saveDownloads( downloads );
            data.putInt( MSG_DOWNLOADS_COUNT, result );
            Log.i( TAG, "Records Updated: " + result );
        }

        return data;
    }

    private List<CMDownloadableRecord> getListofDownloads() throws ClientProtocolException,
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

            // return String, parse on db save. no need on extra java object
          
            return downloads;
        }

        return null;
    }

    private int saveDownloads( List<CMDownloadableRecord> downloads )
    {
        int res = 0;
        NightliesAdapter na = new NightliesAdapter( this.context );
        na.open();
        Log.i( TAG, "Create DB and get connection" );
        for ( int i = 0; i < json.length(); i++ )
        {
            /**
             * Parse this HTML into CMDownloadableRecord in loop
             */
            List<CMDownloadableRecord> downloads = new ArrayList<CMDownloadableRecord>();
            String td;
            int pos = 0;
            while ( -1 != (pos = result.indexOf( "<tr>", pos )) )
            {
                try
                {
                    CMDownloadableRecord download = new CMDownloadableRecord();
                    Log.d( TAG, "Found <tr> at " + pos );
                    for ( int i = 0; i < 5; i++ )
                    {
                        pos = result.indexOf( "<td>", pos ) + 4;
                        Log.d( TAG, "Found <td> at " + pos );
                        td = result.substring( pos, (pos = result.indexOf( "</td>", pos )) );
                        Log.d( TAG, "Got td " + td );

                        switch ( i )
                        {
                            case 1:
                                // type: nightly
                                download.setType( td.trim() );
                                Log.d( TAG, "Type: " + td );
                                break;
                            case 2:
                                // filename format: cm_***.zip
                                // md5sum: ****
                                int _pos = td.indexOf( ">cm_" ) + 1;
                                download.setFileName( td.substring( _pos,
                                        td.indexOf( ".zip", _pos ) + 4 ) );
                                _pos = td.indexOf( "md5sum:" ) + 8;
                                download.setMd5Sum( td.substring( _pos, td.indexOf( "<", _pos ) ) );
                                break;
                            case 3:
                                // size remove small tag
                                download.setSize( td.replaceAll( "(</?small>)", "" ) );
                                break;
                            case 4:
                                // added date: remove small tag
                                download.setDate( td.replaceAll( "(</?small>)", "" ) );
                                break;
                            case 0:
                                // device skip
                            default:
                                // skip
                                break;
                        }
                    }
                    Log.d( TAG, "Converted: " + download );
                    downloads.add( download );
                }
                catch ( ParseException e )
                {
                    Log.e( TAG, "ParseException", e );
                }
            }
        }
        na.close();
        return res;
    }
}
