package com.axelby.podax;

import android.content.ContentValues;
import android.content.Context;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class OPMLImporter {
	public static int read(final Context context, File file) throws IOException, SAXException {
		final int[] count = {0};

		ElementListener outlineHandler = new ElementListener() {
			public void start(Attributes attrs) {
				// only pay attention to rss feeds
				String type = attrs.getValue("type");
				String xmlUrl = attrs.getValue("xmlUrl");
				String title = attrs.getValue("title");
				String text = attrs.getValue("text");

				if (type == null || !type.equals("rss"))
					return;

				// useless without a url
				if (xmlUrl == null)
					return;

				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, xmlUrl);
				if (title != null)
					values.put(SubscriptionProvider.COLUMN_TITLE, title);
				else if (text != null)
					values.put(SubscriptionProvider.COLUMN_TITLE, text);
				context.getContentResolver().insert(SubscriptionProvider.URI, values);
				count[0]++;
			}

			public void end() {
			}
		};

		RootElement root = new RootElement("opml");
		Element body = root.getChild("body");
		body.getChild("outline").setElementListener(outlineHandler);
		body.getChild("outline").getChild("outline").setElementListener(outlineHandler);
		FileInputStream input = new FileInputStream(file);
		Xml.parse(input, Xml.Encoding.UTF_8, root.getContentHandler());
		input.close();

		return count[0];
	}
}
