package com.axelby.podax;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "podax.db";
	private static final int DATABASE_VERSION = 5;

	public DBAdapter(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE subscriptions(" + 
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				"title VARCHAR, " + 
				"url VARCHAR NOT NULL, " + 
				"lastModified DATE, " + 
				"lastUpdate DATE," +
				"eTag VARCHAR," +
				"thumbnail VARCHAR," +
				"subscribed INTEGER NOT NULL DEFAULT 1);");
		db.execSQL("CREATE UNIQUE INDEX subscription_url ON subscriptions(url)");

		db.execSQL("CREATE TABLE podcasts(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"subscriptionId INTEGER, " +
				"title VARCHAR, " +
				"link VARCHAR, " +
				"pubDate DATE, " + 
				"description VARCHAR, " + 
				"mediaUrl VARCHAR," +
				"fileSize INTEGER," +
				"queuePosition INTEGER," +
				"lastPosition INTEGER NOT NULL DEFAULT 0," +
				"duration INTEGER DEFAULT 0," +
				"payment VARCHAR)"
				);
		db.execSQL("CREATE UNIQUE INDEX podcasts_mediaUrl ON podcasts(mediaUrl)");
		db.execSQL("CREATE INDEX podcasts_queuePosition ON podcasts(queuePosition)");

		db.execSQL("CREATE TABLE podax(lastPodcastId INTEGER, activeDownloadId INTEGER)");
		db.execSQL("INSERT INTO podax(lastPodcastId, activeDownloadId) VALUES(NULL, NULL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			// rename id in podcasts table
			db.execSQL("ALTER TABLE podcasts RENAME TO podcasts_old");
			db.execSQL("CREATE TABLE podcasts(" +
					"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"subscriptionId INTEGER, " +
					"title VARCHAR, " +
					"link VARCHAR, " +
					"pubDate DATE, " + 
					"description VARCHAR, " + 
					"mediaUrl VARCHAR," +
					"fileSize INTEGER," +
					"queuePosition INTEGER," +
					"lastPosition INTEGER NOT NULL DEFAULT 0," +
					"duration INTEGER DEFAULT 0)"
					);
			db.execSQL("INSERT INTO podcasts(_id, subscriptionId, title, link, " +
					"pubDate, description, mediaUrl, fileSize, " +
					"queuePosition, lastPosition, duration) " +
					"SELECT id, subscriptionId, title, link, " +
					"pubDate, description, mediaUrl, fileSize, " +
					"queuePosition, lastPosition, duration " +
					"FROM podcasts_old");
			db.execSQL("DROP TABLE podcasts_old");
			db.execSQL("CREATE UNIQUE INDEX podcasts_mediaUrl ON podcasts(mediaUrl)");
			db.execSQL("CREATE INDEX podcasts_queuePosition ON podcasts(queuePosition)");

			// rename id in subscriptions table
			db.execSQL("ALTER TABLE subscriptions RENAME TO subscriptions_old"); 
			db.execSQL("CREATE TABLE subscriptions(" + 
					"_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
					"title VARCHAR, " + 
					"url VARCHAR NOT NULL, " + 
					"lastModified DATE, " + 
					"lastUpdate DATE," +
					"eTag VARCHAR," +
					"thumbnail VARCHAR);");
			db.execSQL("INSERT INTO subscriptions(_id, title, url, " +
					"lastModified, lastUpdate, eTag, thumbnail) " +
					"SELECT id, title, url, " +
					"lastModified, lastUpdate, eTag, thumbnail " +
					"FROM subscriptions_old");
			db.execSQL("DROP TABLE subscriptions_old");
			db.execSQL("CREATE UNIQUE INDEX subscription_url ON subscriptions(url)");
		}

		if (newVersion <= 3) {
			// delete podcasts that are in subscriptions that were deleted incorrectly
			// this happened when gpodder deleted podcasts
			db.execSQL("DELETE FROM podcasts WHERE subscriptionid NOT IN (SELECT _id FROM subscriptions)");

			// fix queue so things are ordered right for queue fragment reordering
			String sql = "UPDATE PODCASTS SET queueposition = " +
					"(SELECT COUNT(*) FROM podcasts p2 WHERE p2.queueposition IS NOT NULL AND p2.queueposition < podcasts.queueposition) " +
					"WHERE podcasts.queueposition IS NOT NULL";
			db.execSQL(sql);
		}

		if (newVersion <= 4) {
			// add payment column
			db.execSQL("ALTER TABLE podcasts ADD COLUMN payment VARCHAR");
		}

		if (newVersion <= 5) {
			db.execSQL("ALTER TABLE subscriptions ADD COLUMN subscribed INTEGER NOT NULL DEFAULT 1");
		}
	}
}
