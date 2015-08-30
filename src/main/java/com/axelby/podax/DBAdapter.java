package com.axelby.podax;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "podax.db";
	private static final int DATABASE_VERSION = 17;

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
				"thumbnail VARCHAR, " +
				"titleOverride VARCHAR," +
				"queueNew INTEGER NOT NULL DEFAULT 1," +
				"description VARCHAR," +
				"expirationDays INTEGER," +
				"singleUse INTEGER DEFAULT 0);");
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
				"downloadId INTEGER," +
				"needsGpodderUpdate INTEGER DEFAULT 0," +
				"gpodderUpdateTimestamp INTEGER," +
				"payment VARCHAR," +
				"finishedTime DATE)"
		);
		db.execSQL("CREATE UNIQUE INDEX podcasts_mediaUrl ON podcasts(mediaUrl)");
		db.execSQL("CREATE INDEX podcasts_queuePosition ON podcasts(queuePosition)");

		db.execSQL("CREATE TABLE podax(lastPodcastId INTEGER, activeDownloadId INTEGER)");
		db.execSQL("INSERT INTO podax(lastPodcastId, activeDownloadId) VALUES(NULL, NULL)");

		db.execSQL("CREATE TABLE gpodder_sync(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"url VARCHAR, " +
				"to_remove INTEGER DEFAULT 0, " +
				"to_add INTEGER DEFAULT 0)");

		db.execSQL("CREATE TABLE gpodder_device(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"username VARCHAR, " +
				"caption VARCHAR" +
				"type VARCHAR," +
				"needsChange INTEGER DEFAULT 0)");

		db.execSQL("CREATE VIRTUAL TABLE fts_podcasts USING fts3(_id, title, description);");
		db.execSQL("CREATE VIRTUAL TABLE fts_subscriptions USING FTS3(_id, title, url, titleOverride, description);");

		db.execSQL("CREATE TABLE itunes(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"date DATE, " +
				"category INTEGER, " +
				"position INTEGER, " +
				"name VARCHAR, " +
				"summary VARCHAR, " +
				"imageUrl VARCHAR, " +
				"idUrl VARCHAR)");
		db.execSQL("CREATE INDEX itunes_category_position_idx ON itunes(category, position)");
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2)
			upgradeV1toV2(db);

        if (oldVersion < 3)
			upgradeV2toV3(db);

        if (oldVersion < 4)
			upgradeV3toV4(db);

        if (oldVersion < 5)
			upgradeV4toV5(db);

        if (oldVersion < 6)
			upgradeV5toV6(db);

        if (oldVersion < 7)
			upgradeV6toV7(db);

        if (oldVersion < 8)
			upgradeV7toV8(db);

        if (oldVersion < 9)
            upgradeV8toV9(db);

        if (oldVersion < 12) {
            // fix bug where database was upgraded wrong -- attempt to do everything from number 5 on
            try { upgradeV5toV6(db); } catch (Exception ignored) { }
			try { upgradeV6toV7(db); } catch (Exception ignored) { }
			try { upgradeV7toV8(db); } catch (Exception ignored) { }
			try { upgradeV8toV9(db); } catch (Exception ignored) { }
        }

        if (oldVersion < 13)
            upgradeV12toV13(db);

		if (oldVersion < 14)
			upgradeV13toV14(db);

		if (oldVersion < 15)
			upgradeV14toV15(db);

		if (oldVersion < 16)
			upgradeV15toV16(db);

		if (oldVersion < 17)
			updateV16toV17(db);
    }

	private void upgradeV1toV2(SQLiteDatabase db) {
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

	private void upgradeV2toV3(SQLiteDatabase db) {
		// delete podcasts that are in subscriptions that were deleted incorrectly
		// this happened when gpodder deleted podcasts
		db.execSQL("DELETE FROM podcasts WHERE subscriptionid NOT IN (SELECT _id FROM subscriptions)");

		// fix queue so things are ordered right for queue fragment reordering
		String sql = "UPDATE PODCASTS SET queueposition = " +
				"(SELECT COUNT(*) FROM podcasts p2 WHERE p2.queueposition IS NOT NULL AND p2.queueposition < podcasts.queueposition) " +
				"WHERE podcasts.queueposition IS NOT NULL";
		db.execSQL(sql);
	}

	private void upgradeV3toV4(SQLiteDatabase db) {
		// add payment column
		db.execSQL("ALTER TABLE podcasts ADD COLUMN payment VARCHAR");
	}

	private void upgradeV4toV5(SQLiteDatabase db) {
		// add new subscription fields
		db.execSQL("ALTER TABLE subscriptions ADD COLUMN titleOverride VARCHAR");
		db.execSQL("ALTER TABLE subscriptions ADD COLUMN queueNew INTEGER NOT NULL DEFAULT 1");
		db.execSQL("ALTER TABLE subscriptions ADD COLUMN expirationDays INTEGER");
	}

	private void upgradeV5toV6(SQLiteDatabase db) {
		// add gpodder sync table
		db.execSQL("CREATE TABLE gpodder_sync(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"url VARCHAR, " +
				"to_remove INTEGER DEFAULT 0, " +
				"to_add INTEGER DEFAULT 0)");
	}

	private void upgradeV6toV7(SQLiteDatabase db) {
		// add download id column
		db.execSQL("ALTER TABLE podcasts ADD COLUMN downloadId INTEGER");
	}

	private void upgradeV7toV8(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE gpodder_device(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"username VARCHAR, " +
				"caption VARCHAR" +
				"type VARCHAR," +
				"needsChange INTEGER DEFAULT 0)");
	}

	private void upgradeV8toV9(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE podcasts ADD COLUMN needsGpodderUpdate INTEGER DEFAULT 0");
		db.execSQL("ALTER TABLE podcasts ADD COLUMN gpodderUpdateTimestamp INTEGER");
	}

	private void upgradeV12toV13(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE subscriptions ADD COLUMN description VARCHAR");
	}

	private void upgradeV13toV14(SQLiteDatabase db) {
		// operation not supported
		//db.execSQL("ALTER TABLE podcasts DROP COLUMN downloadId");
		db.execSQL("ALTER TABLE subscriptions ADD COLUMN singleUse INTEGER DEFAULT 0");
	}

	private void upgradeV14toV15(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE podcasts ADD COLUMN finishedTime DATE");
	}

	private void upgradeV15toV16(SQLiteDatabase db) {
		db.execSQL("CREATE VIRTUAL TABLE fts_podcasts USING fts3(_id, title, description);");
		db.execSQL("INSERT INTO fts_podcasts(_id, title, description) " +
				"SELECT _id, title, description FROM podcasts;");
		db.execSQL("CREATE VIRTUAL TABLE fts_subscriptions USING FTS3(_id, title, url, titleOverride, description);");
		db.execSQL("INSERT INTO fts_subscriptions(_id, title, url, titleOverride, description) " +
				"SELECT _id, title, url, titleOverride, description FROM subscriptions;");
	}

	private void updateV16toV17(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE itunes(" +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"date DATE, " +
				"category INTEGER, " +
				"position INTEGER, " +
				"name VARCHAR, " +
				"summary VARCHAR, " +
				"imageUrl VARCHAR, " +
				"idUrl VARCHAR)");
		db.execSQL("CREATE INDEX itunes_category_position_idx ON itunes(category, position)");
	}
}
