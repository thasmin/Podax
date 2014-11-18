package com.axelby.podax.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SearchSuggestionProvider;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

public class SearchFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	protected static final int OPTION_UNSUBSCRIBE = 4;
	static final int OPTION_ADDTOPLAYLIST = 1;
	static final int OPTION_REMOVEFROMPLAYLIST = 2;
	static final int OPTION_PLAY = 3;
	private static final int CURSOR_SUBSCRIPTIONS = 0;
	private static final int CURSOR_PODCASTS = 1;
	protected String _lastQuery;
	private SearchResultsAdapter _adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_fragment, container);
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
						EpisodeCursor episode = new EpisodeCursor(c);

						if (episode.isDownloaded(getActivity()))
							menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE, R.string.play);

						if (episode.getPlaylistPosition() == null)
							menu.add(ContextMenu.NONE, OPTION_ADDTOPLAYLIST, ContextMenu.NONE, R.string.add_to_playlist);
						else
							menu.add(ContextMenu.NONE, OPTION_REMOVEFROMPLAYLIST, ContextMenu.NONE, R.string.remove_from_playlist);

						break;
					default:
						break;
				}
			}
		});

		final AutoCompleteTextView actv = (AutoCompleteTextView) getActivity().findViewById(R.id.query);

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
				if (_lastQuery.length() > 0)
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
				if (view == null || view.getEditableText() == null)
					return false;
				if (getActivity() == null || getActivity().getCurrentFocus() == null)
					return false;
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

		ImageView cancel = (ImageView) getActivity().findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				actv.setText("");
				_adapter.setPodcastCursor(null);
				_adapter.setSubscriptionCursor(null);
				_adapter.notifyDataSetChanged();
				getListView().invalidate();
			}
		});
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Bundle args = new Bundle();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment fragment;
		switch (_adapter.getType(position)) {
			case SUBSCRIPTION:
				fragment = new EpisodeListFragment();
				args.putLong(Constants.EXTRA_SUBSCRIPTION_ID, id);
				fragment.setArguments(args);
				ft.replace(R.id.fragment, fragment).addToBackStack(null).commit();
				break;
			case PODCAST:
				fragment = new EpisodeDetailFragment();
				args.putLong(Constants.EXTRA_EPISODE_ID, id);
				fragment.setArguments(args);
				ft.replace(R.id.fragment, fragment).addToBackStack(null).commit();
				break;
			default:
				break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (info == null)
			return false;

		SearchResultType type = _adapter.getType(info.position);
		Cursor cursor;
		switch (type) {
			case PODCAST:
				cursor = (Cursor) getListView().getItemAtPosition(info.position);
				EpisodeCursor episode = new EpisodeCursor(cursor);

				switch (item.getItemId()) {
					case OPTION_ADDTOPLAYLIST:
						episode.addToPlaylist(getActivity());
						break;
					case OPTION_REMOVEFROMPLAYLIST:
						episode.removeFromPlaylist(getActivity());
						break;
					case OPTION_PLAY:
						PlayerService.play(getActivity(), episode.getId());
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
			default:
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
					SubscriptionProvider.COLUMN_THUMBNAIL,
			};
			return new CursorLoader(getActivity(), SubscriptionProvider.SEARCH_URI, projection,
					null, new String[]{query}, SubscriptionProvider.COLUMN_TITLE);
		} else if (id == CURSOR_PODCASTS) {
			String[] projection = {
					EpisodeProvider.COLUMN_ID,
					EpisodeProvider.COLUMN_TITLE,
					EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
					EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
					EpisodeProvider.COLUMN_PLAYLIST_POSITION,
					EpisodeProvider.COLUMN_MEDIA_URL,
					EpisodeProvider.COLUMN_FILE_SIZE,
					EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
			};
			return new CursorLoader(getActivity(), EpisodeProvider.SEARCH_URI, projection,
					null, new String[]{query}, EpisodeProvider.COLUMN_PUB_DATE + " DESC");
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
	}

	public class SearchResultsAdapter extends BaseAdapter {
		private LayoutInflater _inflater;
		private Cursor _subscriptionCursor = null;
		private Cursor _podcastCursor = null;

		public SearchResultsAdapter(Context context) {
			_inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
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

			Cursor cursor = (Cursor) getItem(position);
			if (cursor == null)
				return -1;

			if (isSubscription(position))
				return new SubscriptionCursor(cursor).getId();
			if (isPodcast(position))
				return new EpisodeCursor(cursor).getId();

			throw new IllegalStateException();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;

			TextView textView = (TextView) _inflater.inflate(R.layout.list_item, parent, false);
			if (textView == null)
				return convertView;

			Object o = getItem(position);

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

					SubscriptionCursor subscription = new SubscriptionCursor((Cursor) o);

					view = _inflater.inflate(R.layout.search_subscription_listitem, parent);
					((TextView) view.findViewById(R.id.text)).setText(subscription.getTitle());
					((ImageView) view.findViewById(R.id.thumbnail)).setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), subscription.getId()));
					view.findViewById(R.id.more).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							if (getActivity() == null)
								return;
							getActivity().openContextMenu((View) (view.getParent()));
						}
					});

					return view;
				case PODCAST:
					if (o == null)
						return textView;

					EpisodeCursor episode = new EpisodeCursor((Cursor) o);

					view = _inflater.inflate(R.layout.playlist_list_item, parent);
					view.findViewById(R.id.drag).setVisibility(View.INVISIBLE);

					// more button handler
					view.findViewById(R.id.more).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							if (getActivity() == null)
								return;
							getActivity().openContextMenu((View) (view.getParent()));
						}
					});

					((TextView) view.findViewById(R.id.title)).setText(episode.getTitle());
					((TextView) view.findViewById(R.id.subscription)).setText(episode.getSubscriptionTitle());
					((ImageView) view.findViewById(R.id.thumbnail)).setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));
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
				Uri searchUri = Uri.withAppendedPath(EpisodeProvider.URI, "search");
				String[] projection = {
						EpisodeProvider.COLUMN_ID,
						EpisodeProvider.COLUMN_TITLE,
						EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
				};
				return _context.getContentResolver().query(searchUri, projection,
						null, new String[] { _query },
						EpisodeProvider.COLUMN_PUB_DATE + " DESC");
			} else
				throw new IllegalArgumentException("Invalid search group");
		}
		*/
	}
}
