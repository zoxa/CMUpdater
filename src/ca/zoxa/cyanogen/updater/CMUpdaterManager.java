/**
 * 
 */
package ca.zoxa.cyanogen.updater;

import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * @author azim
 */
public class CMUpdaterManager extends PreferenceActivity
{

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		addPreferencesFromResource( R.xml.pref_main );

		Preference cur_device = (Preference) findPreference( "cur_device" );
		cur_device.setSummary( Build.MODEL + " (" + Build.DEVICE + ")" );

		ListPreference device = (ListPreference) findPreference( "device" );
		device.setDefaultValue( Build.DEVICE );
		device.setSummary( device.getEntry() );
		device.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener()
		{

			/**
			 * Trying to auto-update Summary field for the preference ;)
			 */
			public boolean onPreferenceChange( Preference arg0, Object newValue )
			{
				// dirty way to find correlated entry name for new value
				ListPreference device = (ListPreference) arg0;
				CharSequence vals[] = device.getEntryValues();
				for ( int c = 0; c < vals.length; c++ )
				{
					if ( vals[c] == newValue )
					{
						device.setSummary( device.getEntries()[c] );
						return true;
					}
				}
				return false;
			}

		} );
	}
}
