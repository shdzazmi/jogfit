package app.informatika.daz.jogfit.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat.Builder;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;

import app.informatika.daz.jogfit.R;
import app.informatika.daz.jogfit.activities.MapsActivity;
import app.informatika.daz.jogfit.content_provider.DatabaseContract.LocationDataTable;
import app.informatika.daz.jogfit.content_provider.DatabaseContract.RunDataTable;
import app.informatika.daz.jogfit.content_provider.DatabaseHelper;

public class LocationService extends Service {
	
	private static final int NOTIFICATION_ID = 812;
	
	private final LocationBinder binder = new LocationBinder();

	private LocationManager locationManager;
	
	/**
	 * The listener which is called when there is a change in position
	 * from the location manager
	 */
	private LocationListener listener;
	
	private boolean recordLocations = false;
	
	/** List of all locations while bound */
	private ArrayList<Location> locations = new ArrayList<>(0);
	
	/** Last recorded position - null if no positions recorded yet */
	private Location currentPosition;
	
	private Date startTime;
	
	private DatabaseHelper dbHelper;
	
	// Already checked permissions so just suppressing the error in the editor
	@SuppressLint("MissingPermission")
	@Override
	public void onCreate() {
		// Create a location listener which sends the new latitude/longitude information
		// to the broadcast receiver
		listener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				// Create intent for broadcast
				Intent intent = new Intent(
					getApplicationContext().getString(R.string.location_updates)
				);
				
				// Update current position
				currentPosition = location;
				
				// If someone is bound
				if (recordLocations) {
					locations.add(location);
				}
				
				// Make LatLng from given location
				LatLng latLng = new LatLng(
					location.getLatitude(),
					location.getLongitude()
				);
				
				// Add position to intent and send
				intent.putExtra(
					getApplicationContext().getString(R.string.position_latlng),
					latLng
				);
				sendBroadcast(intent);
			}
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// Not used
			}
			
			@Override
			public void onProviderEnabled(String provider) {
				// Not used
			}
			
			@Override
			public void onProviderDisabled(String provider) {
				// Open location settings menu if location is disabled
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		};
		
		// Get reference to system location service
		locationManager = (LocationManager) getApplicationContext()
			.getSystemService(Context.LOCATION_SERVICE);
		
		if (locationManager != null) {
			// Request user location every 3 seconds
			locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				3000,
				0,
				listener
			);
		}
		
		
		/* Database */
		
		dbHelper = new DatabaseHelper(this);
		dbHelper.onCreate(dbHelper.getWritableDatabase());
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	private void setupNotification() {
		// ID of the notification
		String id = "g53mdp_runningTracker";
		// Creates notification information
		Builder builder = new Builder(this, id)
			.setSmallIcon(R.drawable.ic_directions_run_black_32dp)
			.setContentTitle("Running Tracker")
			.setContentText("Tracking your run");
		Intent result = new Intent(this, MapsActivity.class);
		
		// Create notification
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MapsActivity.class);
		stackBuilder.addNextIntent(result);
		PendingIntent resultPendingIntent = stackBuilder
			.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		
		// Start service as a foreground service
		startForeground(NOTIFICATION_ID, builder.build());
	}
	
	private void saveToDatabase(Date stopTime) {
		if (locations == null || locations.size() == 0) {
			return;
		}
		
		
		/* Run data */
		
		ContentValues runValues = new ContentValues();
		runValues.put(RunDataTable.START_TIME, startTime.getTime());
		
		// Distance and duration
		float dist = 0;
		for (int i = 0; i < locations.size() - 1; i++) {
			dist += locations.get(i).distanceTo(locations.get(i + 1));
		}
		
		// Time in ms
		long time = stopTime.getTime() - startTime.getTime();
		
		runValues.put(RunDataTable.DISTANCE, dist);
		runValues.put(RunDataTable.DURATION, time);
		
		ContentResolver resolver = getContentResolver();
		resolver.insert(RunDataTable.URI, runValues);
		
		Cursor cursor = resolver.query(
			RunDataTable.URI,
			new String[]{RunDataTable.ID},
			null, null,
			""
		);
		
		// Get id of last entry (ie. the one just added above)
		cursor.moveToLast();
		int run_id = cursor.getInt(cursor.getColumnIndex(RunDataTable.ID));
		
		
		/* Location data */
		
		ContentValues locValues = new ContentValues();
		locValues.put(LocationDataTable.RUN_ID, run_id);
		for (Location l : locations) {
			locValues.put(LocationDataTable.LAT, l.getLatitude());
			locValues.put(LocationDataTable.LNG, l.getLongitude());
			locValues.put(LocationDataTable.ALTITUDE, l.getAltitude());
			locValues.put(LocationDataTable.TIME, l.getTime());
			
			resolver.insert(LocationDataTable.URI, locValues);
		}
		
		cursor.close();
	}
	
	@Override
	public void onDestroy() {
		if (locationManager != null) {
			// Detach listener
			locationManager.removeUpdates(listener);
		}
		super.onDestroy();
	}
	
	public class LocationBinder extends Binder {
		
		public boolean isRecording() {
			return recordLocations;
		}
		
		public void startRecording() {
			// Want to get time first so it's more accurate
			startTime = new Date();
			
			recordLocations = true;
			setupNotification();
			
			locations = new ArrayList<>();
			if (currentPosition != null) {
				locations.add(currentPosition);
			}
		}
		
		public void stopRecording() {
			// Want to get time first so it's more accurate
			Date stopTime = new Date();
			
			recordLocations = false;
			if (locations.size() > 0) {
				saveToDatabase(stopTime);
			}
			stopForeground(true);
		}
		
		public ArrayList<LatLng> getPositions() {
			ArrayList<LatLng> positions = new ArrayList<>();
			
			for (Location l : locations) {
				positions.add(new LatLng(l.getLatitude(), l.getLongitude()));
			}
			
			return positions;
		}
		
		public LatLng getCurrentPosition() {
			return new LatLng(currentPosition.getLatitude(), currentPosition.getLongitude());
		}
	}
}
