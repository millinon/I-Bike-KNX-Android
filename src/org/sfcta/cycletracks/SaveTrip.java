package org.sfcta.cycletracks;

import java.text.DateFormat;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SaveTrip extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save);

		// Remove the notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		
		// Turn of GPS updates
		CycleTrackData ctd = CycleTrackData.get();
		ctd.activity = this;
		ctd.killListener();		

		// Discard btn
		final Button btnDiscard = (Button) findViewById(R.id.ButtonDiscard);
		final Intent i = new Intent(this, MainInput.class);
		btnDiscard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(getBaseContext(), "Trip discarded.",Toast.LENGTH_SHORT).show();
				
				CycleTrackData.get().dropTrip();
				startActivity(i);
				SaveTrip.this.finish();
			}
		});

		// Submit btn
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		final Intent xi = new Intent(this, ShowMap.class);
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				CycleTrackData ctd = CycleTrackData.get();

				Toast.makeText(
						getBaseContext(),
						"Submitting trip with " + ctd.coords.size()
								+ " points. Thank you!", Toast.LENGTH_SHORT)
						.show();

				// Find user-entered info
				Spinner purpose = (Spinner) findViewById(R.id.SpinnerPurp);
				EditText notes = (EditText) findViewById(R.id.NotesField);

				// Save the trip coords to the database. W00t!
				DbAdapter mDbHelper = new DbAdapter(SaveTrip.this);
				mDbHelper.open();
				String fancystarttime = DateFormat.getInstance().format(ctd.startTime);
				
				mDbHelper.updateTrip(ctd.tripid,
						purpose.getSelectedItem().toString(), 
						ctd.startTime, 
						fancystarttime, notes.getEditableText().toString(),
						ctd.lathigh,ctd.latlow,ctd.lgthigh,ctd.lgtlow
				);
				
				// mDbHelper.createCoordsForTrip(tripid, ctd.coords);
				mDbHelper.close();

				// Show the map!
				startActivity(xi);
				SaveTrip.this.finish();
			}
		});
	}
}