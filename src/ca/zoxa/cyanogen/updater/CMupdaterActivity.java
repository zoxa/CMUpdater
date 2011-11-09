package ca.zoxa.cyanogen.updater;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class CMupdaterActivity extends Activity
{
	private String				device;

	// Tag for the logs
	private static final String	TAG	= "CMU";

	/** Called when the activity is first created. */
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );
		getPrefs();

		// Build.DEVICE: crespo
		// Build.MODEL: Nexus S

		TextView txt = (TextView) findViewById( R.id.txtInfo );
		txt.setText( "DEVICE: " + Build.DEVICE + "\n" + "MODEL: " + Build.MODEL );
		txt.setText( txt.getText() + "\n\nStored Device:" + device );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.menu_main, menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		// Handle item selection
		switch ( item.getItemId() )
		{
			case R.id.menu_pref:
				startActivity( new Intent( this, CMUpdaterManager.class ) );
				return true;
			case R.id.menu_refresh:
				Log.d( TAG, "Start refresh" );
				CMChangelog clog = new CMChangelog( getApplicationContext(), device );
				clog.refreshChangelog( new MenuRefreshHandler() );

				Toast.makeText( getApplicationContext(), getString( R.string.msg_refreshing ),
						Toast.LENGTH_SHORT ).show();
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	/**
	 * Load Default Shared preferences
	 */
	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getBaseContext() );
		device = prefs.getString( "device", "xz" );
		Log.d( TAG, device );
	}

	private class MenuRefreshHandler extends Handler
	{
		@Override
		public void handleMessage( Message msg )
		{
			Bundle data = msg.getData();
			int error = data.getInt( CMChangelog.MSG_DATA_KEY_ERROR, 0 );
			if ( error == 0 )
			{
				Toast.makeText( getApplicationContext(), getString( R.string.msg_refres_done ),
						Toast.LENGTH_LONG ).show();
			}
			else if ( CMChangelog.ERROR_CODE_HTTP_FAIL == error )
			{
				Toast.makeText( getApplicationContext(), getString( R.string.err_msg_http_fail ),
						Toast.LENGTH_LONG ).show();
			}
			else if ( CMChangelog.ERROR_CODE_NO_HOST == error )
			{
				Toast.makeText( getApplicationContext(), getString( R.string.err_msg_no_host ),
						Toast.LENGTH_LONG ).show();
			}
			else if ( CMChangelog.ERROR_CODE_JSON_PARSE == error )
			{
				Toast.makeText( getApplicationContext(), getString( R.string.err_msg_json_parse ),
						Toast.LENGTH_LONG ).show();
			}
			else if ( CMChangelog.ERROR_CODE_DB_OPEN == error )
			{
				Toast.makeText( getApplicationContext(), getString( R.string.err_msg_db_open ),
						Toast.LENGTH_LONG ).show();
			}
		}
	}
}
