package com.axelby.podax.ui;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.axelby.gpodder.NoAuthClient;
import com.axelby.gpodder.ToplistPodcast;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;

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

		String[] strings = {"Loading from GPodder"};
		setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, strings));

		getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<ToplistPodcast[]>() {
			@Override
			public Loader<ToplistPodcast[]> onCreateLoader(int i, Bundle bundle) {
				return new ToplistPodcastLoader(getActivity());
			}

			@Override
			public void onLoadFinished(Loader<ToplistPodcast[]> loader, ToplistPodcast[] feeds) {
				setListAdapter(new ToplistAdapter(getActivity(), feeds));
			}

			@Override
			public void onLoaderReset(Loader<ToplistPodcast[]> loader) {
			}
		});
	}

	private static class ToplistPodcastLoader extends AsyncTaskLoader<ToplistPodcast[]> {
		ToplistPodcast[] _cached = null;

		public ToplistPodcastLoader(Context context) {
			super(context);
		}

		@Override
		public ToplistPodcast[] loadInBackground() {
			return new NoAuthClient(getContext()).getPodcastToplist().toArray(new ToplistPodcast[20]);
		}

		@Override
		protected void onStartLoading() {
			if (takeContentChanged() || _cached == null)
				forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}
	}

	private class ToplistAdapter extends ArrayAdapter<ToplistPodcast> {
		private View.OnClickListener addPodcastHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ToplistPodcast podcast = (ToplistPodcast) view.getTag();
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, podcast.getUrl());
				getContext().getContentResolver().insert(SubscriptionProvider.URI, values);
			}
		};
		private View.OnClickListener viewWebsiteHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View listitem = (View) view.getParent().getParent();
				ToplistPodcast podcast = (ToplistPodcast) listitem.getTag();
				startActivity(new Intent(Intent.ACTION_VIEW, podcast.getWebsite()));
			}
		};

		public ToplistAdapter(Context context, ToplistPodcast[] feeds) {
			super(context, R.layout.gpodder_toplist_item, feeds);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ToplistPodcast podcast = getItem(position);

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

