package com.axelby.podax.ui;

import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

import java.util.HashMap;

public class SearchActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
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
		setContentView(R.layout.search_activity);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
		    actionBar.setDisplayHomeAsUpEnabled(true);

		_subscriptionList = (GridLayout) findViewById(R.id.subscription_list);
		_subscriptionEmpty = (TextView) findViewById(R.id.subscription_empty);

		_episodeList = (LinearLayout) findViewById(R.id.episode_list);
		_episodeEmpty = (TextView) findViewById(R.id.episode_empty);

		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
			finish();
			return;
		}

		EditText query = (EditText) findViewById(R.id.query);
		query.setText(intent.getStringExtra(SearchManager.QUERY));
		query.setSelection(query.getText().length());
		query.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence str, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}

			@Override
			public void onTextChanged(CharSequence str, int start, int before, int count) {
				Bundle args = new Bundle(1);
				args.putString(SearchManager.QUERY, str.toString());
				getLoaderManager().restartLoader(0, args, SearchActivity.this);
				getLoaderManager().restartLoader(1, args, SearchActivity.this);
			}
		});

		getLoaderManager().initLoader(0, intent.getExtras(), this);
		getLoaderManager().initLoader(1, intent.getExtras(), this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Helper.registerMediaButtons(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loader, Bundle bundle) {
		String query = bundle.getString(SearchManager.QUERY);
		if (loader == 0)
			return new CursorLoader(this, SubscriptionProvider.SEARCH_URI, null, null, new String[] { query }, null);
		else if (loader == 1)
			return new CursorLoader(this, EpisodeProvider.SEARCH_URI, null, null, new String[] { query }, null);
		return null;
	}

	private Bitmap getThumbnail(long subId) {
		if (_thumbnails.containsKey(subId))
			return _thumbnails.get(subId);
		Bitmap thumb = SubscriptionCursor.getThumbnailImage(this, subId);
		_thumbnails.put(subId, thumb);
		return thumb;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		LayoutInflater inflater = LayoutInflater.from(this);
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
				thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(SearchActivity.this, ep.getSubscriptionId()));

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
