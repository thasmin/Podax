package com.axelby.riasel;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class AtomParser {

	private final static String NS_ATOM = "http://www.w3.org/2005/Atom";

	public static void process(XmlPullParser parser, FeedParser feedParser) throws XmlPullParserException, IOException {

		Feed feed = new Feed();

		for (int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
			if (eventType == XmlPullParser.START_TAG) {
				if (isAtomElement(parser, "title"))
					feed.setTitle(parser.nextText());
				else if (isAtomElement(parser, "icon"))
					feed.setThumbnail(parser.nextText());
                else if (isAtomElement(parser, "subtitle"))
                    feed.setDescription(parser.nextText());
				else if (isAtomElement(parser, "updated"))
					feed.setLastBuildDate(Utils.parseDate(parser.nextText()));
				else if (isAtomElement(parser, "entry"))
					break;
			}
		}

		if (feedParser.getOnFeedInfoHandler() != null)
			feedParser.getOnFeedInfoHandler().OnFeedInfo(feedParser, feed);
		if (feedParser.shouldStopProcessing())
			return;

		parseEntries(parser, feedParser);
	}

	private static void parseEntries(XmlPullParser parser, FeedParser feedParser) throws XmlPullParserException, IOException {
		FeedItem item = null;

		// grab podcasts from item tags
		for (int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
			if (eventType == XmlPullParser.START_TAG) {
				if (isAtomElement(parser, "entry"))
					item = new FeedItem();

                if (item == null)
                    continue;

				if (isAtomElement(parser, "id")) {
					item.setUniqueId(parser.nextText());
				} else if (isAtomElement(parser, "title")) {
					item.setTitle(parser.nextText());
				} else if (isAtomElement(parser, "link")) {
					String rel = parser.getAttributeValue(null, "rel");
					if (rel == null || rel.equals("alternate"))
						item.setLink(parser.getAttributeValue(null, "href"));
					else if (rel.equals("payment"))
						item.setPaymentURL(parser.getAttributeValue(null, "href"));
					else if (rel.equals("enclosure")) {
						if (parser.getAttributeValue(null, "length") != null)
							item.setMediaSize(Long.valueOf(parser.getAttributeValue(null, "length")));
						item.setMediaURL(parser.getAttributeValue(null, "href"));
					}
				} else if (isAtomElement(parser, "summary") && item.getDescription() == null)
					item.setDescription(parser.nextText());
				else if (isAtomElement(parser, "content"))
					item.setDescription(parser.nextText());
				else if (isAtomElement(parser, "published"))
					item.setPublicationDate(Utils.parseDate(parser.nextText()));
				else if (isAtomElement(parser, "updated") && item.getPublicationDate() == null)
					item.setPublicationDate(Utils.parseDate(parser.nextText()));
			} else if (eventType == XmlPullParser.END_TAG) {
				if (isAtomElement(parser, "entry")) {
					if (feedParser.getOnFeedItemHandler() != null)
						feedParser.getOnFeedItemHandler().OnFeedItem(feedParser, item);
					if (feedParser.shouldStopProcessing())
						return;
					item = null;
				}
			}
		}
	}

	private static boolean isAtomElement(XmlPullParser parser, String name) {
		return parser.getName().equals(name) && parser.getNamespace().equals(NS_ATOM);
	}
}
