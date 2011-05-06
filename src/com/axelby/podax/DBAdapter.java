package com.axelby.podax;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter {
	private static final String[] SUBSCRIPTION_COLUMNS = new String[] { "id",
			"title", "url", "lastupdate" };
	private static final String[] PODCAST_COLUMNS = new String[] { "id",
			"subscriptionId", "title", "link", "pubDate", "description",
			"mediaUrl", "fileSize", "queuePosition", "lastPosition" };
	private static final String DATABASE_NAME = "podax.db";
	private static final int DATABASE_VERSION = 1;
	private Context _context;
	private DBHelper _helper;
	private SQLiteDatabase _db;
	
	private DBAdapter(Context context) {
		this._context = context;
		this._helper = new DBHelper(_context);
		this._db = this._helper.getWritableDatabase();
	}
	
	private static DBAdapter _singleton = null;
	public static DBAdapter getInstance(Context context) {
		if (_singleton == null)
			_singleton = new DBAdapter(context);
		return _singleton;
	}

	private Vector<Subscription> collectSubscriptions(Cursor c) {
		Vector<Subscription> subs = new Vector<Subscription>();
		c.moveToFirst();
		while (!c.isAfterLast())
		{
			Subscription sub = new Subscription(
					c.getInt(0), c.getString(1), c.getString(2), 
					new Date(c.getInt(3) * 1000)
			);
			subs.add(sub);
			c.moveToNext();
		}
		c.close();
		return subs;
	}
	
	public Vector<Subscription> getSubscriptions() {
		Cursor c = this._db.query("subscriptions", SUBSCRIPTION_COLUMNS, 
				null, null, null, null, null);
		Vector<Subscription> s = collectSubscriptions(c);
		c.close();
		return s;
	}

	public Subscription loadSubscription(String url) {
		Cursor c = this._db.query("subscriptions", SUBSCRIPTION_COLUMNS, 
				"url = ?", new String[] { url }, null, null, null);
		if (c.isAfterLast())
		{
			c.close();
			return null;
		}
		Subscription s = collectSubscriptions(c).get(0);
		c.close();
		return s;
	}

	public Subscription loadSubscription(int id) {
		Cursor c = this._db.query("subscriptions", SUBSCRIPTION_COLUMNS, 
				"id = ?", new String[] { Integer.toString(id) }, null, null, null);
		if (c.isAfterLast())
		{
			c.close();
			return null;
		}
		Subscription s = collectSubscriptions(c).get(0);
		c.close();
		return s;
	}

	public Vector<Subscription> getUpdatableSubscriptions() {
		Cursor c = this._db.query("subscriptions", SUBSCRIPTION_COLUMNS, 
				null /*"lastupdate IS NULL"*/, null, null, null, null);
		Vector<Subscription> s = collectSubscriptions(c);
		c.close();
		return s;
	}

	public void deleteSubscription(Subscription subscription) {
		this._db.delete("subscriptions", "id = ?", 
				new String[] { Integer.toString(subscription.getId()) });
	}

	public void deleteAllSubscriptions() {
		this._db.delete("subscriptions", "", new String[] { });
	}
	
	public Subscription addSubscription(String url) {
		ContentValues values = new ContentValues();
		values.put("url", url);
		this._db.insert("subscriptions", null, values);
		return loadSubscription(url);
	}
	
	public Subscription addSubscription(String url, String title) {
		ContentValues values = new ContentValues();
		values.put("url", url);
		values.put("title", title);
		long newId = this._db.insert("subscriptions", null, values);
		return loadSubscription((int)newId);
	}

	public void updateSubscriptionTitle(String url, String title) {
		this._db.execSQL("UPDATE subscriptions SET title = ?, lastupdate = (DATETIME('now')) WHERE url = ?",
				new Object[] { title, url });
	}
	
	public void updatePodcastsFromFeed(Vector<RssPodcast> podcasts) {
		this._db.beginTransaction();
		for (RssPodcast podcast : podcasts) {
			ContentValues values = new ContentValues();
			values.put("subscriptionId", podcast.getSubscription().getId());
			values.put("title", podcast.getTitle());
			values.put("link", podcast.getLink());
			values.put("pubDate", podcast.getPubDate().getTime() / 1000);
			values.put("description", podcast.getDescription());
			values.put("mediaUrl", podcast.getMediaUrl());
			if (loadPodcast(podcast.getLink()) != null)
				this._db.update("podcasts", values, "link = ?", new String[] { podcast.getLink() });
			else {
				long id = this._db.insert("podcasts", null, values);
				
				// if the podcast is less than 5 days old, add it to the queue
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, -5);
				if (podcast.getPubDate().after(c.getTime()))
					this.addPodcastToQueue((int)id);
			}
		}
		this._db.setTransactionSuccessful();
		this._db.endTransaction();
	}
	
	public void savePodcast(Podcast podcast) {
		ContentValues values = new ContentValues();
		values.put("subscriptionId", podcast.getSubscription().getId());
		values.put("title", podcast.getTitle());
		values.put("link", podcast.getLink());
		values.put("pubDate" , podcast.getPubDate().getTime() / 1000);
		values.put("description" , podcast.getDescription());
		values.put("mediaUrl", podcast.getMediaUrl());
		values.put("fileSize", podcast.getFileSize());
		values.put("queuePosition", podcast.getQueuePosition());
		values.put("lastPosition", podcast.getLastPosition());
		if (podcast.getId() != -1)
			this._db.update("podcasts", values, "id = ?", new String[] { Integer.toString(podcast.getId()) });
		else
			this._db.update("podcasts", values, "link = ?", new String[] { podcast.getLink() });
	}

	public Podcast loadPodcast(String link) {
		Cursor c = this._db.query("podcasts", PODCAST_COLUMNS, 
				"link = ?", new String[] { link }, null, null, null);
		if (c.isAfterLast()) {
			c.close();
			return null;
		}
		return collectPodcasts(c).get(0);
	}

	public Podcast loadPodcast(int id) {
		if (id == -1)
			return null;
		
		Cursor c = this._db.query("podcasts", PODCAST_COLUMNS, 
				"id = ?", new String[] { Integer.toString(id) }, null, null, null);
		if (c.isAfterLast()) {
			c.close();
			return null;
		}
		return collectPodcasts(c).get(0);
	}

	private Vector<Podcast> collectPodcasts(Cursor c) {
		Vector<Podcast> pods = new Vector<Podcast>();
		c.moveToFirst();
		while (!c.isAfterLast())
		{
			Subscription subscription = this.loadSubscription(c.getInt(1));
			Integer fileSize = c.isNull(7) ? null : c.getInt(7);
			Integer queuePosition = c.isNull(8) ? null : c.getInt(8);
			pods.add(new Podcast(
					c.getInt(0), subscription,
					c.getString(2), c.getString(3), new Date(c.getLong(4) * 1000),
					c.getString(5), c.getString(6), fileSize, queuePosition,
					c.getInt(9)
			));
			c.moveToNext();
		}
		c.close();
		return pods;
	}
	
	public Vector<Podcast> getPodcastsForSubscription(int subscriptionId) {
		Cursor c = this._db.query("podcasts", PODCAST_COLUMNS, 
				"subscriptionId = ?", new String[] { Integer.toString(subscriptionId) },
				null, null, "pubDate DESC");
		return collectPodcasts(c);
	}

	private Vector<Integer> collectIds(Cursor c) {
		Vector<Integer> ids = new Vector<Integer>();
		c.moveToFirst();
		while (!c.isAfterLast())
		{
			ids.add(c.getInt(0));
			c.moveToNext();
		}
		c.close();
		return ids;
	}

	public Vector<Integer> getQueueIds() {
		Cursor c = this._db.query("podcasts", new String[] { "id" }, 
				"queuePosition IS NOT NULL", null, null, null, "queuePosition");
		return collectIds(c);
	}

	public void addPodcastToQueue(Integer podcastId) {
		this._db.execSQL("UPDATE podcasts SET queuePosition = " +
				"(SELECT COALESCE(MAX(queuePosition) + 1, 0) FROM podcasts) " +
				"WHERE id = ?", 
				new Object[] { podcastId });
	}
	
	public void removePodcastFromQueue(Integer podcastId) {
		this._db.execSQL("UPDATE podcasts SET queuePosition = queuePosition - 1 " +
				"WHERE queuePosition > (SELECT queuePosition FROM podcasts WHERE id = ?)", 
				new Object[] { podcastId });
		this._db.execSQL("UPDATE podcasts SET queuePosition = NULL WHERE id = ?", 
				new Object[] { podcastId });
	}
	
	public static class DBHelper extends SQLiteOpenHelper {
		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE subscriptions(" + 
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
					"title VARCHAR, " + 
					"url VARCHAR NOT NULL, " + 
					"lastupdate DATE);");
			db.execSQL("CREATE UNIQUE INDEX subscription_url ON subscriptions(url)");
			
			db.execSQL("CREATE TABLE podcasts(" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"subscriptionId INTEGER, " +
					"title VARCHAR, " +
					"link VARCHAR, " +
					"pubDate DATE, " + 
					"description VARCHAR, " + 
					"mediaUrl VARCHAR," +
					"fileSize INTEGER," +
					"queuePosition INTEGER," +
					"lastPosition INTEGER NOT NULL DEFAULT 0)"
			);
			db.execSQL("CREATE UNIQUE INDEX podcasts_mediaUrl ON podcasts(mediaUrl)");
			db.execSQL("CREATE INDEX podcasts_queuePosition ON podcasts(queuePosition)");
			
			db.execSQL("CREATE TABLE podax(lastPodcastId INTEGER)");
			db.execSQL("INSERT INTO podax(lastPodcastId) VALUES(NULL)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	public CharSequence getPodcastTitle(Integer podcastId) {
		Cursor c = this._db.query("podcasts", new String[] { "title" }, 
				"id = ?", new String[] { Integer.toString(podcastId) }, 
				null, null, null);
		c.moveToFirst();
		if (!c.isAfterLast())
		{
			c.close();
			return null;
		}
		String title = c.getString(0);
		c.close();
		return title;

	}

	public void updatePodcastPosition(int podcastId, int position) {
		ContentValues values = new ContentValues();
		values.put("lastPosition", position);
		_db.update("podcasts", values, "id = ?", new String[] { String.valueOf(podcastId) });
		
		values = new ContentValues();
		values.put("lastPodcastId", podcastId);
		_db.update("podax", values, "", null);
	}
	
	public Podcast loadLastPlayedPodcast() {
		Cursor c = _db.rawQuery("SELECT p.* FROM podcasts p JOIN podax ON p.id = podax.lastPodcastId", null);
		if (c.isAfterLast()) {
			c.close();
			return null;
		}
		
		return collectPodcasts(c).get(0);
	}
	
	public Vector<Podcast> searchPodcasts(String query) {
		String lower = "%" + query.toLowerCase() + "%";
		Cursor c = _db.rawQuery("SELECT * from podcasts WHERE LOWER(title) LIKE ? OR LOWER(description) LIKE ? ORDER BY pubDate DESC", 
				new String[] { lower, lower });
		return collectPodcasts(c);
	}

	
	public Vector<Subscription> searchSubscriptions(String query) {
		String lower = "%" + query.toLowerCase() + "%";
		Cursor c = _db.rawQuery("SELECT * from subscriptions WHERE LOWER(title) LIKE ? ORDER BY title", 
				new String[] { lower });
		return collectSubscriptions(c);
	}
}
