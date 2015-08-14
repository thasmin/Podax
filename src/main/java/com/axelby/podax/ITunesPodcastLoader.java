package com.axelby.podax;

import android.util.Xml;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ITunesPodcastLoader {
	public static class Podcast {
		public long id;
		public String name;
		public String summary;
		public String imageUrl;
		public String idUrl;

		@Override public String toString() { return name; }
	}

	private ITunesPodcastLoader() { }

	public static Observable<Podcast> getPodcasts() {
		return getPodcasts(0);
	}

	public static Observable<Podcast> getPodcasts(long category) {
		return Observable.just(category)
				.subscribeOn(Schedulers.io())
				.map(_buildUrl)
				.flatMap(_fetchFromITunes)
				.flatMap(_extractEntries);
	}

	private static Func1<Long,String> _buildUrl = new Func1<Long, String>() {
		@Override
		public String call(Long category) {
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
	};

	private static Func1<String, Observable<Response>> _fetchFromITunes = new Func1<String, Observable<Response>>() {
		@Override
		public Observable<Response> call(String url) {
			Request request = new Request.Builder()
					.url(url)
					.build();
			try {
				return Observable.just(new OkHttpClient().newCall(request).execute());
			} catch (IOException e) {
				return Observable.error(OnErrorThrowable.addValueAsLastCause(e, url));
			}
		}
	};

	private static Func1<Response, Observable<Podcast>> _extractEntries = new Func1<Response, Observable<Podcast>>() {
		@Override
		public Observable<Podcast> call(final Response response) {
			return Observable.create(new Observable.OnSubscribe<Podcast>() {
				@Override
				public void call(Subscriber<? super Podcast> subscriber) {
					try {
						XmlPullParser parser = Xml.newPullParser();
						parser.setInput(response.body().byteStream(), "utf-8");

						// find feed tag
						int eventType = parser.getEventType();
						while (eventType != XmlPullParser.START_TAG)
							eventType = parser.next();

						// find entry tag
						for (eventType = parser.next(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next())
							if (eventType == XmlPullParser.START_TAG && isAtomElement(parser, "entry"))
								subscriber.onNext(handleEntry(parser));
						subscriber.onCompleted();
					} catch (XmlPullParserException | IOException e) {
						subscriber.onError(OnErrorThrowable.addValueAsLastCause(e, response.request().urlString()));
					}
				}
			});
		}
	};

	private static Podcast handleEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
		Podcast podcast = new Podcast();
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
