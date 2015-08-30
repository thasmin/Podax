package com.axelby.podax.itunes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.axelby.podax.DBAdapter;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import rx.Observable;
import rx.schedulers.Schedulers;

public class RSSUrlFetcher {
		private final Context _context;
		private final String _iTunesUrl;

		public RSSUrlFetcher(Context context, String iTunesUrl) {
			_context = context;
			_iTunesUrl = iTunesUrl;
		}

		public Observable<String> getRSSUrl() {
			return Observable.just(_iTunesUrl)
				.observeOn(Schedulers.io())
				.flatMap(this::requestPlist)
				.flatMap(this::parsePlist)
				.first()
				.map(this::saveToDB);
		}

		private String saveToDB(String rssUrl) {
			SQLiteDatabase db = new DBAdapter(_context).getWritableDatabase();
			db.execSQL("UPDate itunes " +
				"SET subscriptionId = (SELECT subscriptionId FROM subscriptions WHERE url = ?) " +
				"WHERE itunesUrl = ?",
				new String[] { rssUrl, _iTunesUrl });
			return rssUrl;
		}

		private Observable<String> requestPlist(String iTunesUrl) {
			try {
				OkHttpClient client = new OkHttpClient();
				Response call = client.newCall(new Request.Builder()
						.url(iTunesUrl)
						.addHeader("User-Agent", "iTunes/10.2.1")
						.build()).execute();
				return Observable.just(call.body().string());
			} catch (IOException e) {
				return Observable.error(e);
			}
		}

		private Observable<String> parsePlist(String resp) {
			int doctypeAt = resp.indexOf("<!DOCTYPE ");
			String doctype = resp.substring(doctypeAt + 10, resp.indexOf(" ", doctypeAt + 10));
			if (doctype.equals("plist")) {
				int urlAt = resp.indexOf("<key>url</key>");
				int urlStart = resp.indexOf(">", urlAt + 15) + 1;
				int urlEnd = resp.indexOf("<", urlStart);
				String newUrl = resp.substring(urlStart, urlEnd).replace("&amp;", "&");

				return Observable.just(newUrl)
						.flatMap(this::requestPlist)
						.flatMap(this::parsePlist);
			}

			int rssUrlStart = resp.indexOf("feed-url=\"") + 10;
			int rssUrlEnd = resp.indexOf("\"", rssUrlStart);
			return Observable.just(resp.substring(rssUrlStart, rssUrlEnd));
		}
	}

