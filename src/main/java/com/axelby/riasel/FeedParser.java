package com.axelby.riasel;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FeedParser {

	public interface FeedInfoHandler {
		void OnFeedInfo(FeedParser feedParser, Feed feed);
	}
	public interface FeedItemHandler {
		void OnFeedItem(FeedParser feedParser, FeedItem item);
	}

	public class UnknownFeedException extends Exception {
		private static final long serialVersionUID = -4953090101978301549L;
		public UnknownFeedException() {
			super("This is not a RSS or Atom feed and is unsupported by Riasel.");
		}
	}

	private FeedInfoHandler _feedInfoHandler;
	private FeedItemHandler _feedItemHandler;
	private boolean _stopProcessing = false;

	public FeedParser() {
	}

	FeedInfoHandler getOnFeedInfoHandler() {
		return _feedInfoHandler;
	}
	public void setOnFeedInfoHandler(FeedInfoHandler handler) {
		_feedInfoHandler = handler;
	}
	FeedItemHandler getOnFeedItemHandler() {
		return _feedItemHandler;
	}
	public void setOnFeedItemHandler(FeedItemHandler handler) {
		_feedItemHandler = handler;
	}

	public boolean shouldStopProcessing() {
		return _stopProcessing;
	}
	public void stopProcessing() {
		_stopProcessing = true;
	}

	public void parseFeed(XmlPullParser parser) throws XmlPullParserException, IOException, UnknownFeedException {
		// make sure this is an RSS document
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.START_TAG)
			eventType = parser.next();
		if (parser.getName().equals("rss")) {
			RSSParser.process(parser, this);
		} else if (parser.getName().equals("feed")) {
			AtomParser.process(parser, this);
		} else {
			throw new UnknownFeedException();
		}
	}

}
