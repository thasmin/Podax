package com.axelby.podax.ui;

import java.io.IOException;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.axelby.podax.Constants;
import com.axelby.podax.R;

public class PopularSubscriptionListFragment extends SherlockListFragment {

	private class PodaxFeed {
		public String title;
		public String url;

		@Override
		public String toString() {
			return title;
		}
	}

	private String _source;
	private String _url;

	public PopularSubscriptionListFragment() {
	}

	public PopularSubscriptionListFragment(String source, String url) {
		_source = source;
		_url = url;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.innerlist, null, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		String[] strings = { "Loading from " + _source };
		setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, strings));

		new AsyncTask<Void, Void, Vector<PodaxFeed>>() {
			@Override
			protected Vector<PodaxFeed> doInBackground(Void... params) {
				final Vector<PodaxFeed> feeds = new Vector<PodaxFeed>();
				RootElement root = new RootElement("feeds");
				root.getChild("feed").setStartElementListener(new StartElementListener() {
					public void start(Attributes attrs) {
						PodaxFeed feed = new PodaxFeed();
						feed.title = attrs.getValue("title");
						feed.url = attrs.getValue("url");
						feeds.add(feed);
					}
				});

				HttpGet get = new HttpGet(_url);
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
				return feeds;
			}

			@Override
			protected void onPostExecute(Vector<PodaxFeed> result) {
				// make sure the activity still exists
				if (getActivity() != null)
					setListAdapter(new ArrayAdapter<PodaxFeed>(getActivity(), android.R.layout.simple_list_item_1, result));
				super.onPostExecute(result);
			}
		}.execute((Void)null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		PodaxFeed feed = (PodaxFeed) l.getItemAtPosition(position);

		Intent intent = new Intent(getActivity(), PopularSubscriptionActivity.class);
		intent.putExtra(Constants.EXTRA_TITLE, feed.title);
		intent.putExtra(Constants.EXTRA_URL, feed.url);
		startActivity(intent);
	}

}
