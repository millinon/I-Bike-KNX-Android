package edu.utk.cycleushare.cycleknoxville;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.utk.cycleushare.cycleknoxville.R;

public class NoteMapActivity extends Activity implements OnMapReadyCallback {
	GoogleMap map;

	private MenuItem saveMenuItem;

	ImageView imageView;

	Bitmap photo;

	private Menu menu;
	private NoteData note;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_map);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		imageView = (ImageView) findViewById(R.id.imageView);

		imageView.setVisibility(View.INVISIBLE);

		try {
			// Set zoom controls
			/*map = */((MapFragment) getFragmentManager().findFragmentById(
					R.id.noteMap)).getMapAsync(this);

			Bundle cmds = getIntent().getExtras();
			long noteid = cmds.getLong("shownote");

			note = edu.utk.cycleushare.cycleknoxville.NoteData.fetchNote(this, noteid);

			// Show note details
			TextView t1 = (TextView) findViewById(R.id.TextViewMapNoteType);
			TextView t2 = (TextView) findViewById(R.id.TextViewMapNoteDetails);
			TextView t3 = (TextView) findViewById(R.id.TextViewMapNoteFancyStart);

			//String[] noteTypeText = new String[]{"Pavement issue", "Traffic signal",
			//		"Enforcement", "Bike lane issue", "Note this issue", "Secret passage",
			//		"Crash / Near Miss"};

					/*new String[] { "Pavement issue",
					"Traffic signal", "Enforcement", "Bike parking",
					"Bike lane issue", "Note this issue", "Bike parking",
					"Bike shops", "Public restrooms", "Secret passage",
					"Water fountains", "Note this asset" };
					*/
			t1.setText(NoteData.NoteType.get(note.notetype).label);//noteTypeText[note.notetype]);
			t2.setText(note.notedetails);
			t3.setText(note.notefancystart);

			// Center & zoom the map
		} catch (Exception e) {
			Log.e("GOT!", e.toString());
		}
	}

	// Make sure overlays get zapped when we go BACK
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.note_map, menu);
		saveMenuItem = menu.getItem(0);
		Bundle cmds = getIntent().getExtras();
		long noteid = cmds.getLong("shownote");
		edu.utk.cycleushare.cycleknoxville.NoteData note = edu.utk.cycleushare.cycleknoxville.NoteData.fetchNote(this, noteid);
		if (note.noteimageurl.equals("")) {
			saveMenuItem.setVisible(false);
		} else {
			saveMenuItem.setVisible(true);
		}
		return true;
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_close_note_map:
			// close -> go back to FragmentMainInput
			onBackPressed();
		case R.id.action_switch_note_view:
			// animation for map and image..
			if (saveMenuItem.getTitle().equals("image")) {
				saveMenuItem.setTitle("map");
				Animation animFadeIn = AnimationUtils.loadAnimation(
						getApplicationContext(), android.R.anim.fade_in);
				imageView.setAnimation(animFadeIn);
				imageView.setVisibility(View.VISIBLE);
			} else if (saveMenuItem.getTitle().equals("map")) {
				saveMenuItem.setTitle("image");
				Animation animFadeOut = AnimationUtils.loadAnimation(
						getApplicationContext(), android.R.anim.fade_out);
				imageView.setAnimation(animFadeOut);
				imageView.setVisibility(View.INVISIBLE);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;

		LatLng center = new LatLng(note.latitude * 1E-6,
				note.longitude * 1E-6);

		map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 16));

		if (note != null) {

			int id = 0;

			if(NoteData.NoteType.get(note.notetype).isIssue){
				id = R.drawable.noteissuemapglyph_high;
			} else {
				id = R.drawable.noteassetmapglyph_high;
			}

			    map.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(id))
                    .anchor(0.0f, 1.0f)
                    .position(new LatLng(note.latitude * 1E-6, note.longitude * 1E-6)));

/*			if (note.notetype >= 0 && note.notetype <= 5) {
				map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.noteissuemapglyph_high))
						.anchor(0.0f, 1.0f) // Anchors the marker on the
						// bottom left
						.position(
								new LatLng(note.latitude * 1E-6,
										note.longitude * 1E-6)));
			} else if (note.notetype >= 6 && note.notetype <= 11) {
				map.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.noteassetmapglyph_high))
						.anchor(0.0f, 1.0f) // Anchors the marker on the
						// bottom left
						.position(
								new LatLng(note.latitude * 1E-6,
										note.longitude * 1E-6)));
			}
*/		}

		Log.v("Jason", "Image Photo: " + note.noteimagedata);
		Log.v("Jason", "Image Photo: " + note.noteimageurl);

		if (note.noteimageurl.equals("")) {
		} else {
			// Store photo error, retrieve error
			photo = BitmapFactory.decodeFile(NoteDetailActivity.imgDir() + "/" + note.noteimageurl + ".jpg");

			if (photo.getHeight() > photo.getWidth()) {
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			} else {
				imageView.setScaleType(ImageView.ScaleType.FIT_START);
			}
			imageView.setImageBitmap(photo);
			Log.v("Jason", "Image Photo: " + photo);
		}
	}
}
