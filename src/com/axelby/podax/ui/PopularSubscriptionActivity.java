package com.axelby.podax.ui;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.SubscriptionUpdater;
import com.axelby.podax.UpdateService;

public class PopularSubscriptionActivity extends SherlockActivity {

	private class FeedDetails {
		private String title;
		private String description;
		private Date oldestPodcastDate;

		private FeedDetails() {
		}
	}

	ProgressDialog _dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.popularsubscription);

		String title = getIntent().getExtras().getString(Constants.EXTRA_TITLE);
		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(title);

		Button add_subscription = (Button) findViewById(R.id.add_subscription);
		add_subscription.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, getIntent().getExtras().getString(Constants.EXTRA_URL));
				getContentResolver().insert(SubscriptionProvider.URI, values);
				UpdateService.updateSubscriptions(PopularSubscriptionActivity.this);
				finish();
			}			
		});

		_dialog = ProgressDialog.show(this, "", "Loading Subscription...", true, true);

		String url = getIntent().getExtras().getString(Constants.EXTRA_URL);
		new AsyncTask<String, Void, FeedDetails>() {
			@Override
			protected FeedDetails doInBackground(String... urls) {
				if (urls.length != 1)
					return null;

				final FeedDetails details = new FeedDetails();

				RootElement root = new RootElement("rss");
				Element channel = root.getChild("channel");
				channel.getChild("title").setEndTextElementListener(new EndTextElementListener() {
					public void end(String body) {
						details.title = body;
					}
				});
				channel.getChild("description").setEndTextElementListener(new EndTextElementListener() {
					public void end(String body) {
						details.description = body;
					}
				});
				channel.getChild("item").getChild("pubDate").setEndTextElementListener(new EndTextElementListener() {
					public void end(String body) {
						Date pubDate = parseRFC822Date(body);
						if (pubDate == null)
							return;
						if (details.oldestPodcastDate == null || pubDate.before(details.oldestPodcastDate))
							details.oldestPodcastDate = pubDate;
					}
				});

				HttpGet get = new HttpGet(urls[0]);
				HttpClient client = new DefaultHttpClient();
				try {
					HttpResponse response = client.execute(get);
					Xml.parse(response.getEntity().getContent(), Xml.Encoding.UTF_8, root.getContentHandler());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}

				return details;
			}

			@Override
			protected void onPostExecute(FeedDetails result) {
				TextView title = (TextView) findViewById(R.id.title);
				title.setText(result.title);

				TextView description = (TextView) findViewById(R.id.description);
				description.setText(result.description);

				_dialog.dismiss();
			}

		}.execute(url);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Helper.registerMediaButtons(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, MainActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	public static Date parseRFC822Date(String date) {
		for (SimpleDateFormat format : SubscriptionUpdater.rfc822DateFormats) {
			try {
				return format.parse(date);
			} catch (ParseException e) {
			}
		}
		return null;
	}
}
