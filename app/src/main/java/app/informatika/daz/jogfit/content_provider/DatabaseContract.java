package app.informatika.daz.jogfit.content_provider;

import android.net.Uri;

public class DatabaseContract {
	public static final String AUTHORITY = "app.informatika.daz.jogfit.content_provider.LocationProvider";
	
	public static final String CONTENT_TYPE_SINGLE = "vnd.android.cursor.item/LocationProvider.data.text";
	public static final String CONTENT_TYPE_MULTIPLE = "vnd.android.cursor.dir/LocationProvider.data.text";
	
	public static final String DATABASE_NAME = "g53mdp_running-tracker";
	
	public static class RunDataTable {
		public static final String TABLE_NAME = "run_data";
		
		public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String ID = "_id";
		public static final String START_TIME = "start_time";
		public static final String DISTANCE = "distance";
		public static final String DURATION = "duration";
	}
	
	public static class LocationDataTable {
		public static final String TABLE_NAME = "location_data";
		
		public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
		
		public static final String ID = "_id";
		public static final String RUN_ID = "run_id";
		public static final String LAT = "lat";
		public static final String LNG = "lng";
		public static final String ALTITUDE = "altitude";
		public static final String TIME = "time";
	}
}
