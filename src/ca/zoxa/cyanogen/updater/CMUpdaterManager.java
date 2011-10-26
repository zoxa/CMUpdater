/**
 * 
 */
package ca.zoxa.cyanogen.updater;

import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

/**
 * @author azim
 * 
 */
public class CMUpdaterManager extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_main);

		Preference cur_device = (Preference) findPreference("cur_device");
		cur_device.setSummary(Build.MODEL + " (" + Build.DEVICE + ")");

		Preference ref_dev_list = (Preference) findPreference("ref_dev_list");
		ref_dev_list.setOnPreferenceClickListener(new PrefRefreshDevList());

		ListPreference device = (ListPreference) findPreference("device");
		device.setSummary(device.getValue());
		this.fillDeviceList(device);

	}

	private void fillDeviceList(ListPreference device) {
		// get list from db

		// if failed set "loading message"
		CharSequence[] entries = { getString(R.string.pref_device_loading) };
		CharSequence[] entryValues = { "" };
		device.setEntries(entries);
		device.setEntryValues(entryValues);
		device.setSummary(R.string.pref_device_loading);

	}

	/**
	 * On Preference Click, run Device Refresh
	 * 
	 * @author azim
	 */
	private class PrefRefreshDevList implements OnPreferenceClickListener {

		public boolean onPreferenceClick(Preference pref) {
			pref.setSummary(getString(R.string.ref_dev_list_running));

			return true;
		}
	}
}
