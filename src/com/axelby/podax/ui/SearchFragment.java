package com.axelby.podax.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.androidquery.AQuery;
import com.axelby.podax.Constants;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;
import com.axelby.podax.SearchSuggestionProvider;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

public class SearchFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {
	private static final int CURSOR_SUBSCRIPTIONS = 0;
	private static final int CURSOR_PODCASTS = 1;

	static final int OPTION_ADDTOQUEUE = 1;
	static final int OPTION_REMOVEFROMQUEUE = 2;
	static final int OPTION_PLAY = 3;
	protected static final int OPTION_UNSUBSCRIBE = 4;

	private SearchResultsAdapter _adapter;
	protected String _lastQuery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_fragment, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (Intent.ACTION_SEARCH.equals(getActivity().getIntent().getAction())) {
			String query = getActivity().getIntent().getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
		    		  SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
			suggestions.saveRecentQuery(query, null);

			Bundle bundle = new Bundle();
			bundle.putString("query", query);
			getLoaderManager().initLoader(CURSOR_SUBSCRIPTIONS, bundle, this);
			getLoaderManager().initLoader(CURSOR_PODCASTS, bundle, this);
		}

	    _adapter = new SearchResultsAdapter(getActivity());
		setListAdapter(_adapter);

		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
				SearchResultType type = _adapter.getType(info.position);
				switch (type) {
				case SUBSCRIPTION:
					menu.add(ContextMenu.NONE, OPTION_UNSUBSCRIBE, ContextMenu.NONE, R.string.unsubscribe);
					break;
				case PODCAST:
					Cursor c = (Cursor) getListAdapter().getItem(info.position);
					PodcastCursor podcast = new PodcastCursor(c);

					if (podcast.isDownloaded())
						menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE, R.string.play);

					if (podcast.getQueuePosition() == null)
						menu.add(ContextMenu.NONE, OPTION_ADDTOQUEUE, ContextMenu.NONE, R.string.add_to_queue);
					else
						menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE, ContextMenu.NONE, R.string.remove_from_queue);

					break;
				}
			}
		});

		AutoCompleteTextView actv = (AutoCompleteTextView) getActivity().findViewById(R.id.query);

		// set up autocomplete from search suggestion provider
		/*
		Uri uri = Uri.withAppendedPath(SearchSuggestionProvider.URI, SearchManager.SUGGEST_URI_PATH_QUERY);
		Cursor cursor = getActivity().getContentResolver().query(uri, null, null, new String[] {_query}, null);
		if (cursor != null) {
			String[] from = new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1 };
			int[] to = new int[] { android.R.id.text1 };
			SimpleCursorAdapter records = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_dropdown_item_1line, null, from, to, 0);
			actv.setAdapter(records);
		}
		*/

		/*
		// set up autocomplete click handler
		actv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);
                String query = cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1));
            }
        });
        */

		actv.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				_lastQuery = s.toString();
				requery();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}			
		});

		actv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				if (actionId != EditorInfo.IME_ACTION_SEARCH)
					return false;
				SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
			    		  SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
				suggestions.saveRecentQuery(view.getEditableText().toString(), null);
				
				// hide keyboard
				InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

				return true;
			}
		});
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Intent intent;
		switch (_adapter.getType(position)) {
		case SUBSCRIPTION:
			intent = new Intent(getActivity(), PodcastListActivity.class);
			intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, id);
			startActivity(intent);
			break;
		case PODCAST:
			intent = new Intent(getActivity(), PodcastDetailActivity.class);
			intent.putExtra(Constants.EXTRA_PODCAST_ID, id);
			startActivity(intent);
			break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		SearchResultType type = _adapter.getType(info.position);
		Cursor cursor;
		switch (type) {
		case PODCAST:
			cursor = (Cursor) getListView().getItemAtPosition(info.position);
			PodcastCursor podcast = new PodcastCursor(cursor);

			switch (item.getItemId()) {
			case OPTION_ADDTOQUEUE:
				podcast.addToQueue(getActivity());
				break;
			case OPTION_REMOVEFROMQUEUE:
				podcast.removeFromQueue(getActivity());
				break;
			case OPTION_PLAY:
				ContentValues values = new ContentValues();
				values.put(PodcastProvider.COLUMN_ID, podcast.getId());
				getActivity().getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
				PlayerService.play(getActivity());
				break;
			}
			break;
		case SUBSCRIPTION:
			cursor = (Cursor) getListView().getItemAtPosition(info.position);
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);
			switch (item.getItemId()) {
			case OPTION_UNSUBSCRIBE:
				getActivity().getContentResolver().delete(subscription.getContentUri(), null, null);
				requery();
				break;
			}
			break;
		}

		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		String query = bundle.getString("query");
		if (id == CURSOR_SUBSCRIPTIONS) {
			String[] projection = {
					SubscriptionProvider.COLUMN_ID,
					SubscriptionProvider.COLUMN_TITLE,
			};
			return new CursorLoader(getActivity(), SubscriptionProvider.SEARCH_URI, projection,
					null, new String[] { query }, SubscriptionProvider.COLUMN_TITLE);
		} else if (id == CURSOR_PODCASTS) {
			String[] projection = {
					PodcastProvider.COLUMN_ID,
					PodcastProvider.COLUMN_TITLE,
					PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
					PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
					PodcastProvider.COLUMN_QUEUE_POSITION,
					PodcastProvider.COLUMN_MEDIA_URL,
					PodcastProvider.COLUMN_FILE_SIZE,
					PodcastProvider.COLUMN_SUBSCRIPTION_ID,
			};
			return new CursorLoader(getActivity(), PodcastProvider.SEARCH_URI, projection,
					null, new String[] { query }, PodcastProvider.COLUMN_PUB_DATE + " DESC");
		} else
			throw new IllegalArgumentException("Invalid loader id");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		if (loader.getId() == CURSOR_SUBSCRIPTIONS) {
			_adapter.setSubscriptionCursor(cursor);
		} else {
			_adapter.setPodcastCursor(cursor);
		}
		getListView().setSelection(0);
		getListView().invalidate();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void requery() {
		Bundle bundle = new Bundle();
		bundle.putString("query", _lastQuery);
		getLoaderManager().destroyLoader(CURSOR_SUBSCRIPTIONS);
		getLoaderManager().initLoader(CURSOR_SUBSCRIPTIONS, bundle, SearchFragment.this);
		getLoaderManager().destroyLoader(CURSOR_PODCASTS);
		getLoaderManager().initLoader(CURSOR_PODCASTS, bundle, SearchFragment.this);
	}

	private enum SearchResultType {
		SUBSCRIPTION_HEADER,
		SUBSCRIPTION,
		PODCAST_HEADER,
		PODCAST,
	};

    public class SearchResultsAdapter extends BaseAdapter {
		private LayoutInflater _inflater;

		private Cursor _subscriptionCursor = null;
		private Cursor _podcastCursor = null;

		public SearchResultsAdapter(Context context) {
			_inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		}

		public void setPodcastCursor(Cursor cursor) {
			_podcastCursor = cursor;
			notifyDataSetChanged();
		}

		public void setSubscriptionCursor(Cursor cursor) {
			_subscriptionCursor = cursor;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return getSubscriptionHeaderCount() + getSubscriptionCount() +
					getPodcastHeaderCount() + getPodcastCount();
		}

		private boolean hasSubscriptionHeader() {
			return getSubscriptionCount() > 0;
		}
		private boolean hasPodcastHeader() {
			return getPodcastCount() > 0;
		}
		private int getSubscriptionHeaderCount() {
			return hasSubscriptionHeader() ? 1 : 0;
		}
		private int getPodcastHeaderCount() {
			return hasPodcastHeader() ? 1 : 0;
		}
		private boolean isSubscriptionHeader(int position) {
			return hasSubscriptionHeader() && position == 0;
		}
		private int getSubscriptionCount() {
			if (_subscriptionCursor == null)
				return 0;
			return _subscriptionCursor.getCount();
		}
		private int getSubscriptionIndex(int position) {
			return position - 1;
		}
		private boolean isSubscription(int position) {
			// returns false when no subscriptions because no numbers are between 0 and 1
			return position > 0 && position < getSubscriptionCount() + 1;
		}
		private int getPodcastCount() {
			if (_podcastCursor == null)
				return 0;
			return _podcastCursor.getCount();
		}
		private int getPodcastHeaderPosition() {
			return getSubscriptionHeaderCount() + getSubscriptionCount();
		}
		private boolean isPodcastHeader(int position) {
			return position == getPodcastHeaderPosition();
		}
		private boolean isPodcast(int position) {
			return position > getPodcastHeaderPosition();
		}
		private int getPodcastIndex(int position) {
			return position - getPodcastHeaderPosition() - 1;
		}
		private boolean isHeader(int position) {
			return isSubscriptionHeader(position) || isPodcastHeader(position);
		}

		private SearchResultType getType(int position) {
			if (isSubscriptionHeader(position))
				return SearchResultType.SUBSCRIPTION_HEADER;
			if (isSubscription(position))
				return SearchResultType.SUBSCRIPTION;
			if (isPodcastHeader(position))
				return SearchResultType.PODCAST_HEADER;
			return SearchResultType.PODCAST;
		}

		@Override
		public Object getItem(int position) {
			if (isHeader(position))
				return null;
			if (isSubscription(position)) {
				if (_subscriptionCursor.isClosed())
					return null;
				_subscriptionCursor.moveToPosition(getSubscriptionIndex(position));
				return _subscriptionCursor;
			}
			if (isPodcast(position)) {
				if (_podcastCursor.isClosed())
					return null;
				_podcastCursor.moveToPosition(getPodcastIndex(position));
				return _podcastCursor;
			}
			throw new IllegalStateException();
		}

		@Override
		public long getItemId(int position) {
			if (isHeader(position))
				return -1;

			Cursor cursor = (Cursor)getItem(position);
			if (cursor == null)
				return -1;

			if (isSubscription(position))
				return new SubscriptionCursor(cursor).getId();
			if (isPodcast(position))
				return new PodcastCursor(cursor).getId();

			throw new IllegalStateException();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			TextView textView = (TextView) _inflater.inflate(R.layout.list_item, null);

			Object o = getItem(position);
			AQuery aq;

			switch (getType(position)) {
			case SUBSCRIPTION_HEADER:
				textView.setText("SUBSCRIPTIONS");
				return textView;
			case PODCAST_HEADER:
				textView.setText("PODCASTS");
				return textView;
			case SUBSCRIPTION:
				if (o == null)
					return textView;

				SubscriptionCursor subscription = new SubscriptionCursor((Cursor)o);
				
				view = _inflater.inflate(R.layout.subscription_list_item, null);
				aq = new AQuery(view);
				aq.find(R.id.text).text(subscription.getTitle());
				aq.find(R.id.thumbnail).image(subscription.getThumbnail(), new QueueFragment.ImageOptions());
				aq.find(R.id.more).clicked(new OnClickListener() {
					@Override
					public void onClick(View view) {
						if (getActivity() == null)
							return;
						getActivity().openContextMenu((View)(view.getParent()));
					}
				});
				
				return view;
			case PODCAST:
				if (o == null)
					return textView;

				PodcastCursor podcast = new PodcastCursor((Cursor)o);
				
				view = _inflater.inflate(R.layout.queue_list_item, null);
				aq = new AQuery(view);
				aq.find(R.id.drag).invisible();

				// more button handler
				aq.find(R.id.more).clicked(new OnClickListener() {
					@Override
					public void onClick(View view) {
						if (getActivity() == null)
							return;
						getActivity().openContextMenu((View)(view.getParent()));
					}
				});

				aq.find(R.id.title).text(podcast.getTitle());
				aq.find(R.id.subscription).text(podcast.getSubscriptionTitle());
				aq.find(R.id.thumbnail).image(podcast.getSubscriptionThumbnailUrl(), new QueueFragment.ImageOptions());
				return view;
			default:
				return textView;
			}
		}

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return !isSubscriptionHeader(position) && !isPodcastHeader(position);
		}

    	/*
    	public SearchResultsAdapter(Context context, Cursor cursor, String query)
    	{
    		super(cursor, context);
    		_context = context;
    		_query = query;
    	}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
			TextView textView = (TextView) view;
			textView.setText(cursor.getString(1));
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			String groupTitle = groupCursor.getString(1);
			if (groupTitle.equals("Subscriptions")) {
				Uri searchUri = Uri.withAppendedPath(SubscriptionProvider.URI, "search");
				String[] projection = {
						SubscriptionProvider.COLUMN_ID,
						SubscriptionProvider.COLUMN_TITLE,
				};
				return _context.getContentResolver().query(searchUri, projection,
						null, new String[] { _query },
						SubscriptionProvider.COLUMN_TITLE);
			} else if (groupTitle.equals("Podcasts")) {
				Uri searchUri = Uri.withAppendedPath(PodcastProvider.URI, "search");
				String[] projection = {
						PodcastProvider.COLUMN_ID,
						PodcastProvider.COLUMN_TITLE,
						PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				};
				return _context.getContentResolver().query(searchUri, projection,
						null, new String[] { _query },
						PodcastProvider.COLUMN_PUB_DATE + " DESC");
			} else
				throw new IllegalArgumentException("Invalid search group");
		}
		*/
	}
}