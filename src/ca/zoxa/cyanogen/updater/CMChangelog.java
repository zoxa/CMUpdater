package ca.zoxa.cyanogen.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CMChangelog implements Runnable
{
    // Device code name
    private final String device;

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

    // Messages keys
    public static String MSG_DATA_KEY_ERROR = "ERROR";
    public static String MSG_DATA_KEY_RESPONSE = "RESPONSE";

    public CMChangelog( final String device )
    {
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

        try
        {
            // get JSON data from cm-nightlies.appspot.com
            JSONObject log = getChangelogJSON();
        }
        catch ( ClientProtocolException e )
        {
            // ClientProtocolException in case of an http protocol error
            data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
        }
        catch ( UnsupportedEncodingException e )
        {
            // Specified un supported encoding, should not happen
            data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_HTTP_FAIL );
        }
        catch ( JSONException e )
        {
            // Cannot parse response
            data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_JSON_PARSE );
        }
        catch ( IOException e )
        {
            // IOException in case of a problem or the connection was aborted
            data.putInt( MSG_DATA_KEY_ERROR, ERROR_CODE_NO_HOST );
        }

        // write to DB

        // return request to handler
        Message msg = new Message();
        msg.setData( data );
        handler.sendMessage( msg );

    }

    private JSONObject getChangelogJSON() throws ClientProtocolException, IOException,
            UnsupportedEncodingException, JSONException
    {
        // build request
        HttpGet httpget = new HttpGet( String.format( URL_CHANGELOG_JSON, Uri.encode( device ) ) );
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute( httpget );

        // FIXME: I think we need to make sure that response code is around >= 200 && < 400
        Log.i( TAG, "Status:[" + response.getStatusLine().getStatusCode() + "]" );
        Log.i( TAG, "Status:[" + response.getStatusLine().toString() + "]" );

        HttpEntity entity = response.getEntity();
        if ( entity != null )
        {
            InputStream instream = entity.getContent();
            String result = CMChangelog.convertStreamToString( instream );
            instream.close();
            Log.i( TAG, "Result of converstion: [" + result + "]" );
            Log.i( TAG, "--- Result.length: [" + result.length() + "]" );

            if ( result.length() > 0 )
            {
                Log.i( TAG, "Start JSON Parsing..." );
                JSONTokener jsonToken = new JSONTokener( result );
                Log.i( TAG, "Parsed, show first object" );
                JSONObject json = (JSONObject) jsonToken.nextValue();
                //Log.i( TAG, "JSON: [" + json.toString() + "]" );
                return json;
            }
        }

        return null;
    }

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

}
