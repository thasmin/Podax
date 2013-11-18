package com.axelby.podax.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

public class ITunesPopularListFragment extends ListFragment {

	private class PodaxFeed {
		public String title;
		public String url;

		@Override
		public String toString() {
			return title;
		}
	}

	private boolean _loaded = false;

	public ITunesPopularListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.innerlist, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_loaded = false;
		String[] strings = {"Loading from iTunes"};
		setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, strings));

		new AsyncTask<Void, Void, ArrayList<PodaxFeed>>() {
			@Override
			protected ArrayList<PodaxFeed> doInBackground(Void... params) {
				final ArrayList<PodaxFeed> feeds = new ArrayList<PodaxFeed>();
				RootElement root = new RootElement("feeds");
				root.getChild("feed").setStartElementListener(new StartElementListener() {
					public void start(Attributes attrs) {
						PodaxFeed feed = new PodaxFeed();
						feed.title = attrs.getValue("title");
						feed.url = attrs.getValue("url");
						feeds.add(feed);
					}
				});

				HttpGet get = new HttpGet("http://podax.axelby.com/popularitunes.php");
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
			protected void onPostExecute(ArrayList<PodaxFeed> result) {
				// make sure the activity still exists
				if (getActivity() != null) {
					setListAdapter(new ArrayAdapter<PodaxFeed>(getActivity(), android.R.layout.simple_list_item_1, result));
					_loaded = true;
				}
				super.onPostExecute(result);
			}
		}.execute((Void) null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (!_loaded)
			return;

		PodaxFeed feed = (PodaxFeed) l.getItemAtPosition(position);
		if (feed == null)
			return;

		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_TITLE, feed.title);
		args.putString(Constants.EXTRA_URL, feed.url);
		PopularSubscriptionFragment fragment = new PopularSubscriptionFragment();
		fragment.setArguments(args);

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.fragment, fragment).addToBackStack(null).commit();
	}

}
