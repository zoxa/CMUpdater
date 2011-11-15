package ca.zoxa.cyanogen.updater;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class CMUpdaterActivity extends Activity
{
	private String				device;

	private static final int	PREFRENCES_ACTIVITY	= 1;

	// Tag for the logs
	private static final String	TAG					= "CMU";

	//
	private CMListAdapter		adapter;

	// Build.DEVICE: crespo
	// Build.MODEL: Nexus S

	/** Called when the activity is first created. */
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		// Request progress bar
		requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

		setContentView( R.layout.main );

		getPrefs();
		if ( device.isEmpty() )
		{
			// force device selection
			startActivityForResult( new Intent( this, CMUpdaterManager.class ), PREFRENCES_ACTIVITY );
		}

		// Retrieve and bind list view
		ExpandableListView listView = (ExpandableListView) findViewById( R.id.listView );

		// context menu
		registerForContextMenu( listView );

		// set adapter
		this.adapter = new CMListAdapter( this, device );
		listView.setAdapter( adapter );
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.main, menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		// Handle item selection
		switch ( item.getItemId() )
		{
			case R.id.menu_pref:
				startActivityForResult( new Intent( this, CMUpdaterManager.class ),
						PREFRENCES_ACTIVITY );
				return true;
			case R.id.menu_refresh:
				menuCallRefresh();
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	private void menuCallRefresh()
	{
		Log.d( TAG, "Start refresh" );
		Toast.makeText( getApplicationContext(), getString( R.string.msg_refreshing ),
				Toast.LENGTH_SHORT ).show();

		setProgressBarIndeterminateVisibility( true );
		CMChangelog clog = new CMChangelog( getApplicationContext(), device );
		clog.refreshChangelog( new MenuRefreshHandler() );
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if ( requestCode == PREFRENCES_ACTIVITY )
		{
			String oldDevice = device;
			this.getPrefs();
			// device was changed, call refresh logic
			if ( !device.equals( oldDevice ) )
			{
				adapter.setDevice( device );
				menuCallRefresh();
			}
		}
	}

	/**
	 * Load Default Shared preferences
	 */
	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getBaseContext() );
		device = prefs.getString( "device", "" );
		Log.d( TAG, "Device: " + device );
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
				// refresh list
				Toast.makeText(
						getApplicationContext(),
						getString( R.string.msg_refres_done, data.getInt(
								CMChangelog.MSG_DOWNLOADS_COUNT, 0 ), data.getInt(
								CMChangelog.MSG_JSON_COUNT, 0 ) ), Toast.LENGTH_LONG ).show();
				adapter.notifyDataSetChanged();
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
			setProgressBarIndeterminateVisibility( false );
		}
	}

	public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
	{
		Log.d( TAG, "onCreateContextMenu" );
		super.onCreateContextMenu( menu, v, menuInfo );

		// XXX: Stupid casting
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

		MenuInflater inflater = getMenuInflater();
		int type = ExpandableListView.getPackedPositionType( info.packedPosition );
		switch ( type )
		{
			case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
				inflater.inflate( R.menu.downloads, menu );
				break;
			case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
				inflater.inflate( R.menu.changelog, menu );
				break;
		}
	}

	@Override
	public boolean onContextItemSelected( MenuItem item )
	{
		Log.d( TAG, "onContextItemSelected" );

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

		int type = ExpandableListView.getPackedPositionType( info.packedPosition );
		if ( type == ExpandableListView.PACKED_POSITION_TYPE_CHILD )
		{
			int downloadPosition = ExpandableListView.getPackedPositionGroup( info.packedPosition );
			int changelogPostion = ExpandableListView.getPackedPositionChild( info.packedPosition );

			CMListAdapter.ChangeLogRecord cl = this.adapter.getChild( downloadPosition,
					changelogPostion );

			// Handle item selection
			switch ( item.getItemId() )
			{
				case R.id.cl_openurl:

					startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( String.format(
							CMListAdapter.CHANGELOG_URL, cl.id ) ) ) );
					return true;
			}

		}
		else if ( type == ExpandableListView.PACKED_POSITION_TYPE_GROUP )
		{
			int downloadPosition = ExpandableListView.getPackedPositionGroup( info.packedPosition );
			CMListAdapter.DownloadsRecord dl = this.adapter.getGroup( downloadPosition );

			// Handle item selection
			switch ( item.getItemId() )
			{
				case R.id.dl_openurl:
					startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( String.format(
							CMListAdapter.DOWNLOAD_LIST, this.device, dl.type.toLowerCase() ) ) ) );
					return true;

				case R.id.dl_download_browser:
					startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( String.format(
							CMListAdapter.DOWNLOAD_URL, dl.filename ) ) ) );
					return true;

				case R.id.dl_download:
					// TODO: implement ZIP download via application use
					Uri uri1 = Uri.parse( String.format( CMListAdapter.DOWNLOAD_URL, dl.filename ) );
					return true;
			}
		}

		return super.onContextItemSelected( item );
	}
}
