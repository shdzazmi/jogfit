package app.informatika.daz.jogfit.activities;

import android.Manifest.permission;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import app.informatika.daz.jogfit.R;
import app.informatika.daz.jogfit.content_provider.DatabaseContract.LocationDataTable;
import app.informatika.daz.jogfit.services.LocationService;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
	
	private final static int PERMISSION_REQUEST_CODE = 434;
	private final static int DATA_ACTIVITY_CODE = 478;
	
	boolean isRunning = false;
	boolean hasRunOnce = false;
	
	private FloatingActionButton fab;
	private GoogleMap map;
	private ArrayList<LatLng> positions = new ArrayList<>();
	
	private BroadcastReceiver broadcastReceiver;
	
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	
	private SharedPreferences pref;
	
	private LocationService.LocationBinder service = null;
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// Assign binder
			LocationService.LocationBinder s = MapsActivity.this.service = (LocationService.LocationBinder) service;
			
			// If service is already running
			// Default state is not recording run so no else needed
			if (s.isRecording()) {
				startRun(true);
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Remove reference to binder
			service = null;
		}
	};
	
	/**
	 * Gets references to the widgets from {@link R.layout}.activity_maps
	 * <p>
	 * Initialises map fragment (code from Google's Android Documentation)
	 * <p>
	 * Sets onClickListener for floating action button which starts the app
	 * recording position data and storing it
	 * <p>
	 * Sets-up navigation drawer and actionbar which listeners to switch activities
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		setTitle(R.string.navigation_menu_map);
		
		/* Facebook API to see database information in chrome */
		
		Stetho.initialize(
			Stetho.newInitializerBuilder(this)
				.enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
				.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
				.build()
		);
		
		
		/* Shared Preferences */
		
		pref = getSharedPreferences(getString(R.string.preferences_path), MODE_PRIVATE);
		
		
		/* Map */
		
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
			.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		
		
		/* Floating action button */
		
		fab = findViewById(R.id.floating_action_button);
		// Check if user has accepted the required permissions
		if (checkPermissions()) {
			// Start
			Intent intent = new Intent(this, LocationService.class);
			// Create and bind to service
			startService(intent);
			bindService(intent, serviceConnection, BIND_AUTO_CREATE);
			
			fab.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Flip service from on/off
					if (!isRunning) {
						startRun(false);
					} else {
						stopRun();
					}
				}
			});
		}
		
		
		/* Navigation panel */
		
		drawerLayout = findViewById(R.id.drawer_layout_maps);
		drawerToggle = new ActionBarDrawerToggle(
			this,
			drawerLayout,
			R.string.navigation_menu_open,
			R.string.navigation_menu_close
		);
		drawerLayout.addDrawerListener(drawerToggle);
		drawerToggle.syncState();
		// Shows menu button in action bar
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Do stuff when items are pressed
		NavigationView navigationView = findViewById(R.id.navigation_maps);
		navigationView.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				switch (item.getItemId()) {
					case R.id.nav_map:
						break;
					case R.id.nav_past_data:
						Intent data_intent = new Intent(MapsActivity.this, DataActivity.class);
						startActivityForResult(data_intent, DATA_ACTIVITY_CODE);
						break;
					//case R.id.nav_settings:
					//	Intent sett_intent = new Intent(MapsActivity.this, SettingsActivity.class);
					//	startActivity(sett_intent);
					//	break;
				}
				
				drawerLayout.closeDrawers();
				return true;
			}
		});
		// Set current page to be highlighted
		navigationView.getMenu().getItem(0).setChecked(true);
	}
	
	/**
	 * Check that the application has the correct permissions
	 * Requests permission dialogue opens if permissions are not granted
	 *
	 * @return True if permissions are granted
	 */
	private boolean checkPermissions() {
		// Check if correct version of android
		// and app has location permissions
		if (
			VERSION.SDK_INT >= VERSION_CODES.M
				&& ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED
			) {
			// If not got permissions, request permission dialogue box with
			// personal request code
			requestPermissions(new String[]{
				permission.ACCESS_FINE_LOCATION
			}, PERMISSION_REQUEST_CODE);
			
			return false;
		}
		return true;
	}
	
	@Override
	public void onRequestPermissionsResult(
		int requestCode,
		@NonNull String[] permissions,
		@NonNull int[] grantResults
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		// If the permission request code matches our code
		if (requestCode == PERMISSION_REQUEST_CODE) {
			// Double check that the permission was granted
			if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				// else run the check permission method again
				checkPermissions();
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			// Intercept the onClick and return true if it belongs to
			// the navigation drawer
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DATA_ACTIVITY_CODE && resultCode == RESULT_OK) {
			// Only show route if not running
			if (isRunning) {
				Toast.makeText(this, R.string.already_running, Toast.LENGTH_LONG)
					.show();
				return;
			}
			
			// Get id of run from intent
			int runId = data.getExtras().getInt(getString(R.string.run_id));
			
			// Query database
			String[] projection = new String[]{
				LocationDataTable.ID,
				LocationDataTable.LAT,
				LocationDataTable.LNG,
				LocationDataTable.TIME
			};
			
			Cursor cursor = getContentResolver().query(
				LocationDataTable.URI,
				projection,
				LocationDataTable.RUN_ID + " = " + runId,
				null,
				LocationDataTable.TIME + " ASC"
			);
			
			// Go through cursor and store locations
			positions = new ArrayList<>();
			cursor.moveToFirst();
			int latIndex = cursor.getColumnIndex(LocationDataTable.LAT);
			int lngIndex = cursor.getColumnIndex(LocationDataTable.LNG);
			while (cursor.moveToNext()) {
				LatLng latLng = new LatLng(
					cursor.getDouble(latIndex),
					cursor.getDouble(lngIndex)
				);
				positions.add(latLng);
			}
			cursor.close();
			
			// Draw on map and update camera
			drawRouteOnMap();
			updateCameraPosition(true, null);
		}
	}
	
	private void startRun(boolean alreadyRunning) {
		isRunning = true;
		hasRunOnce = true;
		positions = null;
		
		// Only show toast if not running
		// eg. if app is closed and reopened don't show
		if (!alreadyRunning) {
			service.startRecording();
			
			Toast.makeText(
				MapsActivity.this,
				R.string.start_run,
				Toast.LENGTH_SHORT
			).show();
		}
		
		// Change image to cross
		fab.setImageResource(R.drawable.ic_clear_white_32dp);
	}
	
	private void stopRun() {
		isRunning = false;
		service.stopRecording();
		
		Toast.makeText(
			MapsActivity.this,
			R.string.stop_run,
			Toast.LENGTH_SHORT
		).show();
		
		// Change image to plus
		fab.setImageResource(R.drawable.ic_add_white_32dp);
		
		// Reset min/max zoom
		map.resetMinMaxZoomPreference();
		// Update camera to show entire route
		updateCameraPosition(true, null);
	}
	
	/**
	 * Create and register the broadcast receiver which gets location information
	 * from the {@link LocationService}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		// If the broadcast receiver doesn't exist, create it
		if (broadcastReceiver == null) {
			broadcastReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					// Get location information from service
					LatLng currentPos = (LatLng) intent.getExtras()
						.get(getApplicationContext().getString(R.string.position_latlng));
					
					if (currentPos == null) {
						// Since from intent, can produce null
						return;
					}
					
					// If bound to service and running, get stored positions
					if (service != null && isRunning) {
						if (positions == null) {
							// If positions doesn't exist get the whole array list
							positions = service.getPositions();
						} else {
							// Else just add the current position to out array list
							positions.add(currentPos);
						}
					}
					
					updateMap(currentPos);
				}
			};
		}
		
		// Register the broadcast receiver with the location_update filter
		registerReceiver(broadcastReceiver, new IntentFilter(
			getApplicationContext().getString(R.string.location_updates)
		));
	}
	
	/**
	 * Updates the map by drawing the marker and current route
	 *
	 * @param currentPos The user's current position
	 */
	private void updateMap(LatLng currentPos) {
		// Create marker at new location
		MarkerOptions marker = new MarkerOptions()
			.position(currentPos)
			.icon(
				vectorToBitmap(
					R.drawable.ic_directions_run_black_32dp,
					getApplicationContext().getColor(R.color.colorPrimary)
				)
			);
		
		// Clear the map of old markers and add new one
		// This is to prevent loads of markers on the screen at each data-point
		map.clear();
		map.addMarker(marker);
		
		if (positions == null) {
			return;
		}
		
		drawRouteOnMap();
		
		updateCameraPosition(false, currentPos);
	}
	
	/**
	 * Demonstrates converting a {@link Drawable} to a {@link BitmapDescriptor},
	 * for use as a marker icon.
	 * <p>
	 * Code from Google's Android documentation website
	 */
	private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color) {
		Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
		
		Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
			vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		
		Canvas canvas = new Canvas(bitmap);
		vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		
		DrawableCompat.setTint(vectorDrawable, color);
		vectorDrawable.draw(canvas);
		
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	
	/**
	 * Update the map camera's position and zoom, and animate to it
	 *
	 * @param allPoints Show all points or just previous x (Default: 10)
	 */
	private void updateCameraPosition(boolean allPoints, @Nullable LatLng currentPos) {
		CameraUpdate cameraUpdate;
		int zoom = pref.getInt(
			getString(R.string.preferences_max_zoom),
			getResources().getInteger(R.integer.default_max_zoom)
		);
		
		if (isRunning) {
			// Set max zoom to prevent the camera zooming in too far
			// when only a few points have been recorded
			int maxZoomRun = pref.getInt(
				getString(R.string.preferences_max_zoom_run),
				getResources().getInteger(R.integer.default_max_zoom_run)
			);
			map.setMaxZoomPreference(maxZoomRun);
		}
		
		if (positions == null) {
			return;
		} else if (positions.size() == 0) {
			// While positions is empty move the camera
			// This is for when a run has been completed, it won't
			// move the camera to the user and stay on the final route
			// or where the user manually moves it
			cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentPos, zoom);
		} else if (positions.size() == 1) {
			cameraUpdate = CameraUpdateFactory.newLatLngZoom(positions.get(0), zoom);
		} else { // if (positions.size() > 1) {
			LatLngBounds.Builder builder = new LatLngBounds.Builder();
			int maxNoSteps = pref.getInt(
				getString(R.string.preferences_no_points),
				getResources().getInteger(R.integer.default_no_points)
			);
			
			if (allPoints || maxNoSteps < 0) {
				// If '-1' use all positions
				for (LatLng pos : positions) {
					builder.include(pos);
				}
			} else if (!isRunning && hasRunOnce) {
				return;
			} else {
				int length = positions.size();
				
				for (int i = (length > maxNoSteps) ? length - maxNoSteps : 0; i < length; i++) {
					
					builder.include(positions.get(i));
				}
			}
			
			LatLngBounds bounds = builder.build();
			cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100);
		}
		
		map.animateCamera(cameraUpdate);
	}
	
	/**
	 * Draw route on map using {@link #positions}
	 */
	private void drawRouteOnMap() {
		// Draw line between all points to show route
		// Uses app accent colour
		PolylineOptions line = new PolylineOptions()
			.addAll(positions)
			.width(10)
			.color(getApplicationContext().getColor(R.color.colorAccent)
			);
		map.addPolyline(line);
	}
	
	/**
	 * When the map API has been instantiated, store the reference to the
	 * map object
	 *
	 * @param googleMap The map reference
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
	}
	
	/**
	 * Unregister the broadcast receiver to prevent memory leaks
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (broadcastReceiver != null) {
			unregisterReceiver(broadcastReceiver);
		}
		
		boolean running = service.isRecording();
		unbindService(serviceConnection);
		if (!running) {
			// If not running then stop the location service
			Intent intent = new Intent(this, LocationService.class);
			stopService(intent);
		}
	}
}
