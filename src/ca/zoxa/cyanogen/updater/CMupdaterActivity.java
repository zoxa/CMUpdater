package ca.zoxa.cyanogen.updater;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class CMupdaterActivity extends Activity
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
		// click events
		listView.setOnGroupClickListener( new GroupClickListener() );
		listView.setOnChildClickListener( new ChildClickListener() );

		// context menu
		registerForContextMenu( listView );

		this.adapter = new CMListAdapter( this );
		listView.setAdapter( adapter );
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
		setProgressBarIndeterminateVisibility( true );
		CMChangelog clog = new CMChangelog( getApplicationContext(), device );
		clog.refreshChangelog( new MenuRefreshHandler() );

		Toast.makeText( getApplicationContext(), getString( R.string.msg_refreshing ),
				Toast.LENGTH_SHORT ).show();
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
			setProgressBarIndeterminateVisibility( false );
		}
	}

	/* List View functions */
	private class GroupClickListener implements OnGroupClickListener
	{
		public boolean onGroupClick( ExpandableListView parent, View v, int groupPosition, long id )
		{
			// TODO Auto-generated method stub
			return false;
		}
	}

	private class ChildClickListener implements OnChildClickListener
	{
		public boolean onChildClick( ExpandableListView parent, View v, int groupPosition,
				int childPosition, long id )
		{
			// TODO Auto-generated method stub
			return false;
		}
	}

	@Override
	public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
	{
		Log.d( TAG, "onCreateContextMenu" );
		menu.setHeaderTitle( "Sample menu" );
		menu.add( 0, 0, 0, "Test" );
	}

	@Override
	public boolean onContextItemSelected( MenuItem item )
	{
		Log.d( TAG, "onContextItemSelected" );
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();

		String title = ( (TextView) info.targetView ).getText().toString();

		int type = ExpandableListView.getPackedPositionType( info.packedPosition );
		if ( type == ExpandableListView.PACKED_POSITION_TYPE_CHILD )
		{
			int groupPos = ExpandableListView.getPackedPositionGroup( info.packedPosition );
			int childPos = ExpandableListView.getPackedPositionChild( info.packedPosition );
			Toast.makeText( this, title + ": Child " + childPos + " clicked in group " + groupPos,
					Toast.LENGTH_SHORT ).show();
			return true;
		}
		else if ( type == ExpandableListView.PACKED_POSITION_TYPE_GROUP )
		{
			int groupPos = ExpandableListView.getPackedPositionGroup( info.packedPosition );
			Toast.makeText( this, title + ": Group " + groupPos + " clicked", Toast.LENGTH_SHORT )
					.show();
			return true;
		}

		return false;
	}

}
