package com.axelby.podax;

import android.util.Xml;

import com.axelby.riasel.FeedParser;
import com.axelby.riasel.Utils;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class PodcastRSSTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void oldRSSFeed() throws IOException, XmlPullParserException, FeedParser.UnknownFeedException {
		String podcastXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<rss version=\"2.0\">\n" +
			"  <channel>\n" +
			"    <title><![CDATA[Podax Test Podcast]]></title>\n" +
			"    <image>\n" +
			"      <url>http://podax.axelby.com/thumbnail.png</url>\n" +
			"    </image>" +
			"    <link><![CDATA[http://podax.axelby.com]]></link>\n" +
			"    <description><![CDATA[A podcast used to test the Android app Podax.]]></description>\n" +
			"    <language>en</language>\n" +
			"    <pubDate>Wed, 23 Nov 2011 21:39:07 -0500</pubDate>\n" +
			"    <lastBuildDate>Wed, 23 Nov 2011 22:00:00 -0500</lastBuildDate>\n" +
			"    <item>\n" +
			"      <title><![CDATA[Loop 1]]></title>\n" +
			"      <link><![CDATA[http://podax.axelby.com/loop1]]></link>\n" +
			"      <description><![CDATA[The first simulated podcast. 8 seconds of music.]]></description>\n" +
			"      <guid isPermaLink=\"false\"><![CDATA[F540680C-690D-4DD0-A2D3-36BB8E62B287]]></guid>\n" +
			"      <pubDate>Wed, 10 May 2011 22:39:00 -0500</pubDate>\n" +
			"      <enclosure length=\"133246\" type=\"audio/mpeg\" url=\"http://blog.axelby.com/loop1.mp3\" />\n" +
			"    </item>\n" +
			"  </channel>\n" +
			"</rss>";

		FeedParser feedParser = new FeedParser();
		feedParser.setOnFeedInfoHandler((feedParser1, feed) -> {
			Assert.assertEquals("feed title is wrong", "Podax Test Podcast", feed.getTitle());
			Assert.assertEquals("thumbnail is wrong", "http://podax.axelby.com/thumbnail.png", feed.getThumbnail());
			Assert.assertEquals("description is wrong", "A podcast used to test the Android app Podax.", feed.getDescription());
			Assert.assertEquals("link is wrong", "http://podax.axelby.com", feed.getLink());
			Date pubDate = Utils.parseDate("Wed, 23 Nov 2011 21:39:07 -0500");
			Assert.assertEquals("pub date is wrong", pubDate, feed.getPubDate());
			Date lastBuildDate = Utils.parseDate("Wed, 23 Nov 2011 22:00:00 -0500");
			Assert.assertEquals("last build date", lastBuildDate, feed.getLastBuildDate());
			// add link and last update
		});
		feedParser.setOnFeedItemHandler(((feedParser1, item) -> {
			Assert.assertEquals("media url is wrong", "http://blog.axelby.com/loop1.mp3", item.getMediaURL());
			Assert.assertEquals("title is wrong", "Loop 1", item.getTitle());
			Assert.assertEquals("description is wrong", "The first simulated podcast. 8 seconds of music.", item.getDescription());
			Assert.assertEquals("link is wrong", "http://podax.axelby.com/loop1", item.getLink());
			Date pubDate = Utils.parseDate("Wed, 10 May 2011 22:39:00 -0500");
			Assert.assertEquals("pub date is wrong", pubDate, item.getPublicationDate());
			Assert.assertEquals("media size is wrong", 133246L, item.getMediaSize().longValue());
			// TODO: support payment in RSS feeds
		}));

		InputStream input = new ByteArrayInputStream(podcastXML.getBytes("UTF-8"));
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(input, "UTF-8");
		feedParser.parseFeed(parser);
	}

}
