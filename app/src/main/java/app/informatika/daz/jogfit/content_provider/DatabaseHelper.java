package app.informatika.daz.jogfit.content_provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import app.informatika.daz.jogfit.content_provider.DatabaseContract.LocationDataTable;
import app.informatika.daz.jogfit.content_provider.DatabaseContract.RunDataTable;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	public DatabaseHelper(Context context) {
		super(context, DatabaseContract.DATABASE_NAME, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// Allow SQLite to use foreign keys
		db.execSQL("PRAGMA foreign_keys = 1;");
		
		// Create run data table which is used for each run
		db.execSQL(
			"CREATE TABLE IF NOT EXISTS " + RunDataTable.TABLE_NAME + "("
				
				// Keys
				+ RunDataTable.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ RunDataTable.START_TIME + " INTEGER NOT NULL, "
				+ RunDataTable.DISTANCE + " REAL, "
				+ RunDataTable.DURATION + " INTEGER "
				
				+ ");"
		);
		
		// Create the location data table which is used to store all the run data
		db.execSQL(
			"CREATE TABLE IF NOT EXISTS " + LocationDataTable.TABLE_NAME + "("
				
				// Keys
				+ LocationDataTable.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ LocationDataTable.RUN_ID + " INTEGER NOT NULL, "
				+ LocationDataTable.LAT + " REAL NOT NULL, "
				+ LocationDataTable.LNG + " REAL NOT NULL, "
				+ LocationDataTable.ALTITUDE + " REAL, "
				+ LocationDataTable.TIME + " INTEGER, "
				
				// Foreign key linking run_id to run data table's primary key
				+ "FOREIGN KEY(" + LocationDataTable.RUN_ID + ") REFERENCES "
				+ RunDataTable.TABLE_NAME + "(" + RunDataTable.ID + ") "
				
				+ ");"
		);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Need to drop locations first due to foreign key constraints
		db.execSQL("DROP TABLE IF EXISTS " + LocationDataTable.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + RunDataTable.TABLE_NAME);
		onCreate(db);
	}
}
