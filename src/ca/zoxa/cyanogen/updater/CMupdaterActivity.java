package ca.zoxa.cyanogen.updater;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class CMupdaterActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Build.DEVICE: crespo
		// Build.MODEL: Nexus S

		TextView txt = (TextView) findViewById(R.id.txtInfo);

		txt.setText("DEVICE: " + Build.DEVICE + "\n" + "MODEL: " + Build.MODEL);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_pref:
			startActivity(new Intent(this, CMUpdaterManager.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}