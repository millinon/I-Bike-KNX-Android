/**	 CycleTracks, (c) 2009 San Francisco County Transportation Authority
 * 					  San Francisco, CA, USA
 *
 *   Licensed under the GNU GPL version 3.0.
 *   See http://www.gnu.org/licenses/gpl-3.0.txt for a copy of GPL version 3.0.
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 */
package org.sfcta.cycletracks;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ShowMap extends MapActivity {
	private MapView mapView;
	List<Overlay> mapOverlays;
	Drawable drawable;
	ItemizedOverlayTrack gpspoints;
	float[] lineCoords;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		try {
			// Set zoom controls
			mapView = (MapView) findViewById(R.id.mapview);
			mapView.setBuiltInZoomControls(true);

			// Set up the point layer
			mapOverlays = mapView.getOverlays();
			if (mapOverlays != null) mapOverlays.clear();

	        Bundle cmds = getIntent().getExtras();
            long tripid = cmds.getLong("showtrip");

            TripData trip = TripData.fetchTrip(this, tripid);

            // Show trip details
            TextView t1 = (TextView) findViewById(R.id.TextViewT1);
            TextView t2 = (TextView) findViewById(R.id.TextViewT2);
            TextView t3 = (TextView) findViewById(R.id.TextViewT3);
            t1.setText(trip.purp);
            t2.setText(trip.info);
            t3.setText(trip.fancystart);

            // Center & zoom the map
			int latcenter = (trip.lathigh + trip.latlow) / 2;
			int lgtcenter = (trip.lgthigh + trip.lgtlow) / 2;
			GeoPoint center = new GeoPoint(latcenter, lgtcenter);
			MapController mc = mapView.getController();
			mc.animateTo(center);
			mc.zoomToSpan(10+trip.lathigh - trip.latlow, 10+trip.lgthigh - trip.lgtlow);

			if (gpspoints == null) {
				AddPointsToMapLayerTask maptask = new AddPointsToMapLayerTask();
				maptask.execute(trip);
			} else {
				mapOverlays.add(gpspoints);
			}

			if (trip.status < TripData.STATUS_SENT
					&& cmds != null
					&& cmds.getBoolean("uploadTrip", false)) {
			    // And upload to the cloud database, too!  W00t W00t!
                TripUploader uploader = new TripUploader(ShowMap.this);
                uploader.execute(trip.tripid);
			}

		} catch (Exception e) {
			Log.e("GOT!",e.toString());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// Auto-generated method stub
		return false;
	}

	private class AddPointsToMapLayerTask extends AsyncTask <TripData, Integer, ItemizedOverlayTrack> {
	    TripData trip;

		@Override
		protected ItemizedOverlayTrack doInBackground(TripData... trips) {
	        trip = trips[0]; // always get just the first trip

			drawable = getResources().getDrawable(R.drawable.point);
            ShowMap.this.gpspoints = trip.getPoints(drawable);

			return ShowMap.this.gpspoints;
		}

		@Override
		protected void onPostExecute(ItemizedOverlayTrack gpspoints) {
		    // Add the points
			mapOverlays.add(ShowMap.this.gpspoints);

			// Add the lines!  W00t!
            mapOverlays.add(new LineOverlay(ShowMap.this.gpspoints));

			// Add start & end pins
			if (trip.startpoint != null) {
			    mapOverlays.add(new PushPinOverlay(trip.startpoint, R.drawable.pingreen));
			}
            if (trip.endpoint != null) {
                mapOverlays.add(new PushPinOverlay(trip.endpoint, R.drawable.pinpurple));
            }

            // Redraw the map
			mapView.invalidate();
		}
	}

    class LineOverlay extends com.google.android.maps.Overlay
    {
        ItemizedOverlayTrack track;

        public LineOverlay(ItemizedOverlayTrack track) {
            super();
            this.track = track;
        }

        @Override
        public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
            super.draw(canvas, mapView, shadow);

            // Need at least two points to draw a line, duh
            if (track.size()<2) return true;

            // Build array of points
            float[] points = new float[4 * track.size()];
            int segments = 0;
            int startx = -1; int starty = -1;

            for (int i=0; i<track.size(); i++) {
                CyclePoint z = (CyclePoint) track.getItem(i).getPoint();

                // Skip lousy points
                if (z.accuracy > 8) {
                    startx = -1;
                    continue;
                }

                // If this is the beginning of a new segment, great
                Point screenPoint = new Point();
                mapView.getProjection().toPixels(z, screenPoint);

                if (startx == -1) {
                    startx = screenPoint.x;
                    starty = screenPoint.y;
                    continue;
                }
                int numpts = segments*4;
                points[numpts] = startx;
                points[numpts+1] = starty;
                points[numpts+2] = startx = screenPoint.x;
                points[numpts+3] = starty = screenPoint.y;
                segments++;
            }

            // Line style
            Paint paint = new Paint();
            paint.setARGB(255,0,0,255);
            paint.setStrokeWidth(5);
            paint.setStyle(Style.FILL_AND_STROKE);

            canvas.drawLines(points, 0, segments*4, paint);
            return false;
        }
    }

	class PushPinOverlay extends com.google.android.maps.Overlay
    {
        GeoPoint p;
        int d;

        public PushPinOverlay(GeoPoint p, int drawrsrc) {
            super();
            this.p=p;
            this.d=drawrsrc;
        }

        @Override
        public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
            super.draw(canvas, mapView, shadow);

            //---translate the GeoPoint to screen pixels---
            Point screenPoint = new Point();
            mapView.getProjection().toPixels(p, screenPoint);

            //---add the marker---
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), d);
            int height = bmp.getScaledHeight(canvas);
            int width = (int)(0.133333 * bmp.getScaledWidth(canvas));  // 4/30 pixels: how far right we want the pushpin

            canvas.drawBitmap(bmp, screenPoint.x-width, screenPoint.y-height, null);
            return true;
        }
    }
}
