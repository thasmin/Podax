package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class SubscriptionListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private SubscriptionAdapter _adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter(getActivity(), null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		GridView _grid = (GridView) getActivity().findViewById(R.id.grid);
		_grid.setAdapter(_adapter);
		_grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> grid, View view, int position, long id) {
				SubscriptionCursor sub = new SubscriptionCursor((Cursor) grid.getItemAtPosition(position));
				long subscriptionId = sub.getId();

				EpisodeListFragment episodeListFragment = new EpisodeListFragment();
				Bundle args = new Bundle();
				args.putLong(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
				episodeListFragment.setArguments(args);
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment, episodeListFragment).addToBackStack(null).commit();
			}
		});
		registerForContextMenu(_grid);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.subscription_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.refresh_subscriptions) {
			UpdateService.updateSubscriptions(getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(0, 0, 0, R.string.unsubscribe);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0:
				AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				Cursor cursor = (Cursor) _adapter.getItem(menuInfo.position);
				SubscriptionCursor subscription = new SubscriptionCursor(cursor);
				getActivity().getContentResolver().delete(subscription.getContentUri(), null, null);
				break;
			default:
				return super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
				SubscriptionProvider.COLUMN_THUMBNAIL,
		};
		return new CursorLoader(getActivity(), SubscriptionProvider.URI, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		_adapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		_adapter.changeCursor(null);
	}

	private class SubscriptionAdapter extends ResourceCursorAdapter {
		public SubscriptionAdapter(Context context, Cursor cursor) {
			super(context, R.layout.subscription_list_item, cursor, true);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);

			((TextView) view.findViewById(R.id.text)).setText(subscription.getTitle());
			((SquareImageView) view.findViewById(R.id.thumbnail)).setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), subscription.getId()));
		}
	}
}
