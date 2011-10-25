package ca.zoxa.cyanogen.updater;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

public class CMupdaterActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Build.DEVICE: crespo
		// Build.MODEL: Nexus S
		
		TextView txt = (TextView) findViewById( R.id.txtInfo );

		txt.setText(  
			"DEVICE: " + Build.DEVICE + "\n" +
			"MODEL: " + Build.MODEL + "\n"
		);	

	}
}