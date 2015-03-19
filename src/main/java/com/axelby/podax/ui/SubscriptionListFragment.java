package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

import javax.annotation.Nonnull;

public class SubscriptionListFragment extends Fragment
		implements LoaderManager.LoaderCallbacks<Cursor>,
		CoverFlowLayoutManager.SelectedChildChangedHandler {

	private SubscriptionAdapter _adapter = null;

	private long _selectedId = -1;
	private int _selectedPosition = -1;
	private Cursor _cursor;

	private RecyclerView _list;
	private TextView _subscriptionTitle;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter();
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_list = (RecyclerView) view.findViewById(R.id.list);
		CoverFlowLayoutManager coverFlowLayoutManager = new CoverFlowLayoutManager();
		coverFlowLayoutManager.setOnSelectedChildChanged(this);
		_list.setLayoutManager(coverFlowLayoutManager);
		_list.setItemAnimator(new DefaultItemAnimator());

		_subscriptionTitle = (TextView) view.findViewById(R.id.subscription_title);

		View settings = view.findViewById(R.id.settings);
		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), SubscriptionSettingsFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, _selectedId));
			}
		});

		View unsub = view.findViewById(R.id.unsubscribe);
		unsub.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().getContentResolver().delete(SubscriptionProvider.getContentUri(_selectedId), null, null);
				_adapter.notifyItemRemoved(_selectedPosition);
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_list.setAdapter(_adapter);

        View.OnClickListener addListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(PodaxFragmentActivity.createIntent(getActivity(), AddSubscriptionFragment.class, null));
            }
        };
        getActivity().findViewById(R.id.add).setOnClickListener(addListener);

	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.subscription_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            UpdateService.updateSubscriptions(getActivity());
            return true;
        }
        return false;
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
				SubscriptionProvider.COLUMN_THUMBNAIL,
                SubscriptionProvider.COLUMN_DESCRIPTION,
		};
		return new CursorLoader(getActivity(), SubscriptionProvider.URI, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;
		changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		clearCursor();
	}

	@Override
	public void onSelectedChildChanged(int position) {
		if (_cursor == null)
			return;
		_cursor.moveToPosition(position);
		SubscriptionCursor sub = new SubscriptionCursor(_cursor);
		_selectedPosition = position;
		_selectedId = sub.getId();
		_subscriptionTitle.setText(sub.getTitle());
	}

	public void changeCursor(Cursor cursor) {
		_cursor = cursor;
		_adapter.notifyDataSetChanged();
		onSelectedChildChanged(0);
	}

	public void clearCursor() {
		_cursor = null;
		_adapter.notifyDataSetChanged();
	}

	private class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
			public final ImageView thumbnail;

            public ViewHolder(View v) {
				super(v);

				thumbnail = (ImageView) v;
				thumbnail.setOnClickListener(_subscriptionChoiceHandler);
            }
        }

		private final View.OnClickListener _subscriptionChoiceHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, _selectedId));
			}
		};

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			ImageView view = new ImageView(parent.getContext());
			view.setLayoutParams(new ViewGroup.LayoutParams(512, 512));
			view.setScaleType(ImageView.ScaleType.FIT_XY);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			_cursor.moveToPosition(position);
			SubscriptionCursor subscription = new SubscriptionCursor(_cursor);
			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(holder.thumbnail.getContext(), subscription.getId()));
		}

		@Override
        public long getItemId(int position) {
			_cursor.moveToPosition(position);
            return new SubscriptionCursor(_cursor).getId();
        }

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _cursor.getCount();
		}

	}
}
