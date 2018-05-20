package app.informatika.daz.jogfit.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import app.informatika.daz.jogfit.R;
import app.informatika.daz.jogfit.content_provider.DatabaseContract.RunDataTable;

public class DataActivity extends AppCompatActivity {
	
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	
	private int noEntries = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data);
		setTitle(R.string.navigation_menu_data);
		
		/* Navigation panel */
		
		drawerLayout = findViewById(R.id.drawer_layout_data);
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
		NavigationView navigationView = findViewById(R.id.navigation_data);
		navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				switch (item.getItemId()) {
					case R.id.nav_map:
						setResult(RESULT_CANCELED);
						finish();
						break;
					case R.id.nav_past_data:
						break;
					//case R.id.nav_settings:
					//	Intent sett_intent = new Intent(DataActivity.this, SettingsActivity.class);
					//	startActivity(sett_intent);
					//	break;
				}
				
				drawerLayout.closeDrawers();
				return true;
			}
		});
		// Set current page to be highlighted
		navigationView.getMenu().getItem(1).setChecked(true);
		
		
		/* List View */
		
		Cursor cursor = populateListView();
		
		if (noEntries > 0) {
			// Get longest time
			cursor = getRunColumn(RunDataTable.DURATION, false);
			
			cursor.moveToFirst();
			long millis = cursor.getInt(cursor.getColumnIndex(RunDataTable.DURATION));
			String maxTime = longMillisToTime(millis);
			TextView time = findViewById(R.id.data_max_time);
			time.setText(maxTime);
			
			// Get longest distance
			cursor = getRunColumn(RunDataTable.DISTANCE, false);
			
			cursor.moveToFirst();
			long maxDist = cursor.getInt(cursor.getColumnIndex(RunDataTable.DISTANCE));
			TextView dist = findViewById(R.id.data_max_dist);
			dist.setText(maxDist + "m");
		} else {
			TextView time = findViewById(R.id.data_max_time);
			TextView dist = findViewById(R.id.data_max_dist);
			time.setText("--:--:--");
			dist.setText("---");
		}
		
		cursor.close();
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
	
	/**
	 * Query the database for runs and populate the list view
	 *
	 * @return The cursor object used to query the database so it can be reused
	 */
	private Cursor populateListView() {
		String[] projection = new String[]{
			RunDataTable.ID,
			RunDataTable.START_TIME,
			RunDataTable.DURATION,
			RunDataTable.DISTANCE
		};
		
		Cursor cursor = getContentResolver().query(
			RunDataTable.URI,
			projection,
			null, null,
			RunDataTable.START_TIME + " DESC"
		);
		
		noEntries = cursor.getCount();
		
		String[] toDisplay = new String[]{
			RunDataTable.START_TIME,
			RunDataTable.DURATION,
			RunDataTable.DISTANCE
		};
		
		int[] columnResIds = new int[]{
			R.id.run_list_start,
			R.id.run_list_time,
			R.id.run_list_distance
		};
		
		SimpleCursorAdapter dataAdapter = new SimpleCursorAdapter(
			this,
			R.layout.run_listview_layout,
			cursor,
			toDisplay,
			columnResIds,
			0
		) {
			@Override
			public void setViewText(TextView v, String text) {
				super.setViewText(v, formatText(v, text));
			}
			
			private String formatText(TextView v, String text) {
				switch (v.getId()) {
					case R.id.run_list_start:
						long startTime = Long.parseLong(text);
						Date startDate = new Date(startTime);
						SimpleDateFormat startSdf = new SimpleDateFormat("EEE, dd MMM - h:mm a");
						return startSdf.format(startDate);
					case R.id.run_list_time:
						long millis = Long.parseLong(text);
						return longMillisToTime(millis);
					case R.id.run_list_distance:
						int dist = (int) Float.parseFloat(text);
						return String.valueOf(dist) + "m";
					default:
						return text;
				}
			}
		};
		
		ListView listView = findViewById(R.id.list_view);
		listView.setAdapter(dataAdapter);
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
				int id = noEntries - position;
				Intent returnIntent = new Intent();
				returnIntent.putExtra(getString(R.string.run_id), id);
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		});
		
		return cursor;
	}
	
	/**
	 * Query the run table to get a specific column
	 *
	 * @param column The column to get
	 * @param asc
	 * @return
	 */
	private Cursor getRunColumn(String column, boolean asc) {
		return getContentResolver().query(
			RunDataTable.URI,
			new String[]{
				RunDataTable.ID,
				column
			},
			null, null,
			column + (asc ? " ASC" : " DESC")
		);
	}
	
	private String longMillisToTime(long millis) {
		long second = (millis / 1000) % 60;
		long minute = (millis / (1000 * 60)) % 60;
		long hour = (millis / (1000 * 60 * 60)) % 24;
		return String.format(Locale.UK, "%02d:%02d:%02d", hour, minute, second);
	}
}
