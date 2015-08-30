package com.axelby.podax.itunes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Xml;

import com.axelby.podax.DBAdapter;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.joda.time.LocalDate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.schedulers.Schedulers;

public class PodcastFetcher {
		private Context _context = null;
		private final long _category;

		public PodcastFetcher(Context context) {
			this(context, 0);
		}

		public PodcastFetcher(Context context, long category) {
			_context = context;
			_category = category;
		}

		public Observable<List<Podcast>> getPodcasts() {
			return loadFromDB(_category)
				.subscribeOn(Schedulers.io())
				.concatWith(
					Observable.just(_category)
						.map(this::buildUrl)
						.flatMap(this::fetchFromITunes)
						.flatMap(this::extractEntries)
						.map(this::saveToDB)
				).first();
		}

		private Observable<List<Podcast>> loadFromDB(long category) {
			return Observable.create(subscriber -> {
				SQLiteDatabase db = new DBAdapter(_context).getReadableDatabase();
				String[] projection = new String[]{
					"_id", "date", "category", "position",
					"name", "summary", "imageUrl", "idUrl"
				};
				String selection = "category = ? AND date = ?";
				String[] selectionArgs = {Long.toString(category), Long.toString(LocalDate.now().toDate().getTime())};
				Cursor c = db.query("itunes", projection, selection, selectionArgs, null, null, "position");
				if (c == null) {
					subscriber.onCompleted();
					return;
				}

				ArrayList<Podcast> podcasts = new ArrayList<>(100);
				while (c.moveToNext()) {
					Podcast p = new Podcast();
					p.id = c.getLong(0);
					p.date = new LocalDate(c.getInt(1));
					p.category = c.getLong(2);
					p.position = c.getInt(3);
					p.name = c.getString(4);
					p.summary = c.getString(5);
					p.imageUrl = c.getString(6);
					p.idUrl = c.getString(7);
					podcasts.add(p);
				}
				c.close();

				if (podcasts.size() > 0)
					subscriber.onNext(podcasts);
				subscriber.onCompleted();
			});
		}

		String buildUrl(long category) {
			StringBuilder url = new StringBuilder();
			// TODO: figure out why https isn't working with okhttp and fix it
			url.append("http://itunes.apple.com/us/rss/toppodcasts/limit=100/");
			if (category != 0) {
				url.append("genre=");
				url.append(category);
				url.append("/");
			}
			url.append("explicit=true/xml");
			return url.toString();
		}

		private Observable<Response> fetchFromITunes(String url) {
			Request request = new Request.Builder()
				.url(url)
				.build();
			try {
				return Observable.just(new OkHttpClient().newCall(request).execute());
			} catch (IOException e) {
				return Observable.error(OnErrorThrowable.addValueAsLastCause(e, url));
			}
		}

		private Observable<List<Podcast>> extractEntries(Response response) {
			return Observable.create(subscriber -> {
				try {
					XmlPullParser parser = Xml.newPullParser();
					parser.setInput(response.body().byteStream(), "utf-8");

					// find feed tag
					int eventType = parser.getEventType();
					while (eventType != XmlPullParser.START_TAG)
						eventType = parser.next();

					ArrayList<Podcast> podcasts = new ArrayList<>(100);
					// find entry tag
					for (eventType = parser.next(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next())
						if (eventType == XmlPullParser.START_TAG && isAtomElement(parser, "entry")) {
							Podcast podcast = handleEntry(parser);
							podcast.position = podcasts.size();
							podcasts.add(podcast);
						}
					subscriber.onNext(podcasts);
					subscriber.onCompleted();
				} catch (XmlPullParserException | IOException e) {
					subscriber.onError(OnErrorThrowable.addValueAsLastCause(e, response.request().urlString()));
				}
			});
		}

		private List<Podcast> saveToDB(List<Podcast> podcasts) {
			SQLiteDatabase db = new DBAdapter(_context).getWritableDatabase();
			db.delete("itunes", "category = ?", new String[]{Long.toString(podcasts.get(0).category)});

			for (Podcast p : podcasts) {
				ContentValues values = new ContentValues(8);
				values.put("_id", p.id);
				values.put("date", p.date.toDate().getTime());
				values.put("category", p.category);
				values.put("position", p.position);
				values.put("name", p.name);
				values.put("summary", p.summary);
				values.put("imageUrl", p.imageUrl);
				values.put("idUrl", p.idUrl);
				db.insert("itunes", "_id", values);
			}

			return podcasts;
		}

		private Podcast handleEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
			Podcast podcast = new Podcast();
			podcast.category = _category;
			podcast.date = LocalDate.now();

			for (int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
				if (eventType == XmlPullParser.START_TAG) {
					if (isAtomElement(parser, "id")) {
						podcast.id = Long.valueOf(parser.getAttributeValue(NS_ITUNES, "id"));
						podcast.idUrl = parser.nextText();
					} else if (isAtomElement(parser, "summary")) {
						podcast.summary = parser.nextText();
					} else if (isITunesElement(parser, "name")) {
						podcast.name = parser.nextText();
					} else if (isITunesElement(parser, "image")
						&& parser.getAttributeValue("", "height").equals("170")) {
						podcast.imageUrl = parser.nextText().replace("170", "100");
					}
				} else if (eventType == XmlPullParser.END_TAG && isAtomElement(parser, "entry")) {
					return podcast;
				}
			}
			return podcast;
		}

		private static final String NS_ATOM = "http://www.w3.org/2005/Atom";
		private static final String NS_ITUNES = "http://itunes.apple.com/rss";

		private static boolean isITunesElement(@Nonnull XmlPullParser parser, @Nonnull String name) {
			return name.equals(parser.getName()) && NS_ITUNES.equals(parser.getNamespace());
		}

		private static boolean isAtomElement(@Nonnull XmlPullParser parser, @Nonnull String name) {
			return name.equals(parser.getName()) && NS_ATOM.equals(parser.getNamespace());
		}
	}

