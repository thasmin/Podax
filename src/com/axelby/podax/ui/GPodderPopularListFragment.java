package com.axelby.podax.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.axelby.gpodder.NoAuthClient;
import com.axelby.gpodder.dto.Podcast;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;

import java.util.List;

public class GPodderPopularListFragment extends ListFragment {

	public GPodderPopularListFragment() {
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

		String[] strings = {"Loading from gpodder.net"};
		setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, strings));

		getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Podcast[]>() {
			@Override
			public Loader<Podcast[]> onCreateLoader(int i, Bundle bundle) {
				return new ToplistPodcastLoader(getActivity());
			}

			@Override
			public void onLoadFinished(Loader<Podcast[]> loader, Podcast[] feeds) {
				if (feeds != null)
					setListAdapter(new ToplistAdapter(getActivity(), feeds));
			}

			@Override
			public void onLoaderReset(Loader<Podcast[]> loader) {
			}
		});
	}

	private class ToplistPodcastLoader extends AsyncTaskLoader<Podcast[]> {
		public ToplistPodcastLoader(Context context) {
			super(context);
		}

		@Override
		public Podcast[] loadInBackground() {
			NoAuthClient client = new NoAuthClient(getContext());
			List<Podcast> toplist = client.getPodcastToplist();
			if (client.getErrorMessage() == null)
				return toplist.toArray(new Podcast[20]);
			Toast.makeText(getContext(), "Error retrieving toplist: " + client.getErrorMessage(), Toast.LENGTH_LONG).show();
			return null;
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}
	}

	private class ToplistAdapter extends ArrayAdapter<Podcast> {
		private View.OnClickListener addPodcastHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Podcast podcast = (Podcast) view.getTag();
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, podcast.getUrl());
				getContext().getContentResolver().insert(SubscriptionProvider.URI, values);
			}
		};
		private View.OnClickListener viewWebsiteHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View listitem = (View) view.getParent().getParent();
				Podcast podcast = (Podcast) listitem.getTag();
				startActivity(new Intent(Intent.ACTION_VIEW, podcast.getWebsite()));
			}
		};

		public ToplistAdapter(Context context, Podcast[] feeds) {
			super(context, R.layout.gpodder_toplist_item, feeds);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Podcast podcast = getItem(position);

			View v = convertView;
			if (v == null)
				v = getActivity().getLayoutInflater().inflate(R.layout.gpodder_toplist_item, null);

			((TextView) v.findViewById(R.id.title)).setText(podcast.getTitle());
			((TextView) v.findViewById(R.id.description)).setText(podcast.getDescription());
			((NetworkImageView) v.findViewById(R.id.logo)).setImageUrl(podcast.getLogoUrl(), Helper.getImageLoader(getActivity()));
			v.findViewById(R.id.add).setOnClickListener(addPodcastHandler);
			v.findViewById(R.id.view_website).setOnClickListener(viewWebsiteHandler);
			v.setTag(podcast);
			return v;
		}
	}
}

