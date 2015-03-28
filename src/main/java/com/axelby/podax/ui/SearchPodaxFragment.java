package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

import java.util.HashMap;

public class SearchPodaxFragment extends Fragment
		implements SearchActivity.QueryChangedHandler, LoaderManager.LoaderCallbacks<Cursor> {
	private GridLayout _subscriptionList;
	private TextView _subscriptionEmpty;

	private LinearLayout _episodeList;
	private TextView _episodeEmpty;

	private HashMap<Long, Bitmap> _thumbnails = new HashMap<>();

	private View.OnClickListener _subscriptionClickHandler = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			long id = (long) view.getTag();
			startActivity(PodaxFragmentActivity.createIntent(view.getContext(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, id));
		}
	};

	private final View.OnClickListener _episodeClickHandler = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			long id = (long) view.getTag();
			startActivity(PodaxFragmentActivity.createIntent(view.getContext(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, id));
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_podax_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		_subscriptionList = (GridLayout) view.findViewById(R.id.subscription_list);
		_subscriptionEmpty = (TextView) view.findViewById(R.id.subscription_empty);

		_episodeList = (LinearLayout) view.findViewById(R.id.episode_list);
		_episodeEmpty = (TextView) view.findViewById(R.id.episode_empty);

		getLoaderManager().initLoader(0, getArguments(), this);
		getLoaderManager().initLoader(1, getArguments(), this);
	}

	@Override
	public void onQueryChanged(String query) {
		Bundle args = new Bundle(1);
		args.putString(SearchManager.QUERY, query);
		getLoaderManager().restartLoader(0, args, this);
		getLoaderManager().restartLoader(1, args, this);
	}

	private Bitmap getThumbnail(long subId) {
		if (_thumbnails.containsKey(subId))
			return _thumbnails.get(subId);
		Bitmap thumb = SubscriptionCursor.getThumbnailImage(getActivity(), subId);
		_thumbnails.put(subId, thumb);
		return thumb;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loader, Bundle bundle) {
		String query = bundle.getString(SearchManager.QUERY);
		if (loader == 0)
			return new CursorLoader(getActivity(), SubscriptionProvider.SEARCH_URI, null, null, new String[] { query }, null);
		else if (loader == 1)
			return new CursorLoader(getActivity(), EpisodeProvider.SEARCH_URI, null, null, new String[] { query }, null);
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		if (getActivity() == null)
			return;

		Context context = getActivity();
		LayoutInflater inflater = LayoutInflater.from(context);
		if (cursorLoader.getId() == 0) {
			boolean isEmpty = cursor.getCount() == 0;
			_subscriptionList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
			_subscriptionEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

			_subscriptionList.removeAllViews();
			int rowCount = (int) Math.ceil(cursor.getCount() / 3.0f);
			_subscriptionList.setRowCount(rowCount);

			int thumbSize = _subscriptionList.getMeasuredWidth() / 3;
			thumbSize -= getResources().getDisplayMetrics().density * 10;

			while (cursor.moveToNext()) {
				SubscriptionCursor sub = new SubscriptionCursor(cursor);
				View view = inflater.inflate(R.layout.search_item_subscription, _subscriptionList, false);
				view.setOnClickListener(_subscriptionClickHandler);
				view.setTag(sub.getId());

				ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
				thumbnail.setLayoutParams(new LinearLayout.LayoutParams(thumbSize, thumbSize));
				thumbnail.setImageBitmap(getThumbnail(sub.getId()));

				TextView title = (TextView) view.findViewById(R.id.title);
				title.setLayoutParams(new LinearLayout.LayoutParams(thumbSize, ViewGroup.LayoutParams.WRAP_CONTENT));
				title.setText(sub.getTitle());

				_subscriptionList.addView(view);
			}
		}
		else if (cursorLoader.getId() == 1) {
			boolean isEmpty = cursor.getCount() == 0;
			_episodeList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
			_episodeEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

			while (cursor.moveToNext()) {
				EpisodeCursor ep = new EpisodeCursor(cursor);
				View view = inflater.inflate(R.layout.search_item_episode, _episodeList, false);
				view.setOnClickListener(_episodeClickHandler);
				view.setTag(ep.getId());

				ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
				thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(context, ep.getSubscriptionId()));

				TextView title = (TextView) view.findViewById(R.id.title);
				title.setText(ep.getTitle());

				_episodeList.addView(view);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		if (cursorLoader.getId() == 0) {
			_subscriptionList.setVisibility(View.GONE);
			_subscriptionList.removeAllViews();
			_subscriptionEmpty.setVisibility(View.VISIBLE);
		} else if (cursorLoader.getId() == 1) {
			_episodeList.setVisibility(View.GONE);
			_episodeList.removeAllViews();
			_episodeEmpty.setVisibility(View.VISIBLE);
		}
	}
}
