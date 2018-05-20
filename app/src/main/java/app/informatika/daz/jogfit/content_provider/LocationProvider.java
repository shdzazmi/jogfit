package app.informatika.daz.jogfit.content_provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import app.informatika.daz.jogfit.content_provider.DatabaseContract.LocationDataTable;
import app.informatika.daz.jogfit.content_provider.DatabaseContract.RunDataTable;

public class LocationProvider extends ContentProvider {
	
	/**
	 * Matches URIs and returns codes set below
	 */
	private static final UriMatcher uriMatcher;
	
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		// Add runs table
		uriMatcher.addURI(DatabaseContract.AUTHORITY, RunDataTable.TABLE_NAME, 1);
		uriMatcher.addURI(DatabaseContract.AUTHORITY, RunDataTable.TABLE_NAME + "/#", 2);
		// Add locations table
		uriMatcher.addURI(DatabaseContract.AUTHORITY, LocationDataTable.TABLE_NAME, 3);
		uriMatcher.addURI(DatabaseContract.AUTHORITY, LocationDataTable.TABLE_NAME + "/#", 4);
		// If not found return code '9'
		uriMatcher.addURI(DatabaseContract.AUTHORITY, "*", 9);
	}
	
	/**
	 * Helper class which deals with interacting with the database
	 */
	private DatabaseHelper dbHelper;
	
	@Override
	public boolean onCreate() {
		// Create helper
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		if (uri.getLastPathSegment() == null) {
			return DatabaseContract.CONTENT_TYPE_MULTIPLE;
		} else {
			return DatabaseContract.CONTENT_TYPE_SINGLE;
		}
	}
	
	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		String tableName;
		
		switch (uriMatcher.match(uri)) {
			case 1:
			case 2:
				tableName = RunDataTable.TABLE_NAME;
				break;
			case 3:
			case 4:
				tableName = LocationDataTable.TABLE_NAME;
				break;
			default:
				throw new UnsupportedOperationException("Unknown Table");
		}
		
		long id = db.insert(tableName, null, values);
		Uri nu = ContentUris.withAppendedId(uri, id);
		
		getContext().getContentResolver().notifyChange(nu, null);
		
		return nu;
	}
	
	@Nullable
	@Override
	public Cursor query(
		@NonNull Uri uri,
		@Nullable String[] projection,
		@Nullable String selection,
		@Nullable String[] selectionArgs,
		@Nullable String sortOrder
	) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		switch (uriMatcher.match(uri)) {
			case 2:
				selection = RunDataTable.ID + " = " + uri.getLastPathSegment();
			case 1:
				return db.query(
					RunDataTable.TABLE_NAME,
					projection,
					selection,
					selectionArgs,
					null,
					null,
					sortOrder
				);
			case 4:
				selection = LocationDataTable.ID + " = " + uri.getLastPathSegment();
			case 3:
				return db.query(
					LocationDataTable.TABLE_NAME,
					projection,
					selection,
					selectionArgs,
					null,
					null,
					sortOrder
				);
			default:
				return null;
		}
	}
	
	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
		@Nullable String[] selectionArgs) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection,
		@Nullable String[] selectionArgs) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
