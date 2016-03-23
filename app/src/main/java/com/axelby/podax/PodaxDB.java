package com.axelby.podax;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PodaxDB {
	private DBAdapter _dbAdapter;

	public PodaxDB(Context context) {
		_dbAdapter = new DBAdapter(context);
	}

	public GPodderDB gPodder() { return new GPodderDB(); }

	public class GPodderDB {

		private String[] _columns = new String[]{"url"};

		public GPodderDB() { }

		private List<String> getUrls(String selection) {
			SQLiteDatabase db = _dbAdapter.getReadableDatabase();
			Cursor c = db.query("gpodder_sync", _columns, selection, null, null, null, "url");
			if (c == null)
				return new ArrayList<>(0);
			ArrayList<String> urls = new ArrayList<>(c.getCount());
			while (c.moveToNext())
				urls.add(c.getString(0));
			c.close();
			return urls;
		}

		public List<String> getToAdd() {
			return getUrls("to_add = 1");
		}

		public List<String> getToRemove() {
			return getUrls("to_remove = 1");
		}

		public void add(@NonNull String url) {
			ContentValues values = new ContentValues(2);
			values.put("url", url);
			values.put("to_add", 1);

			SQLiteDatabase db = _dbAdapter.getWritableDatabase();
			db.insert("gpodder_sync", null, values);
			db.close();
		}

		public void remove(@NonNull String url) {
			ContentValues values = new ContentValues(2);
			values.put("url", url);
			values.put("to_remove", 1);

			SQLiteDatabase db = _dbAdapter.getWritableDatabase();
			db.insert("gpodder_sync", null, values);
			db.close();
		}

		public void clear() {
			SQLiteDatabase db = _dbAdapter.getWritableDatabase();
			db.delete("gpodder_sync", null, null);
			db.close();
		}
	}
}
