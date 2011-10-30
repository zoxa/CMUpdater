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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CMupdaterActivity extends Activity
{
    private String device;

    // Tag for the logs
    private static final String TAG = "CMU";

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

        Button btn = (Button) findViewById( R.id.btnRefresh );
        btn.setOnClickListener( new RefreshList() );
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

    private class RefreshList implements OnClickListener
    {
        public void onClick( View v )
        {
            Log.d( TAG, "Start refresh" );
            CMChangelog clog = new CMChangelog( device );
            clog.refreshChangelog( new ListHandler() );

            Toast.makeText( CMupdaterActivity.this, "Fetching JSON", Toast.LENGTH_LONG ).show();
        }
    }

    private class ListHandler extends Handler
    {
        @Override
        public void handleMessage( Message msg )
        {}
    }
}