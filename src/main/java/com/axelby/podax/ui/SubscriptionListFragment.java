package com.axelby.podax.ui;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.trello.rxlifecycle.components.RxFragment;

import javax.annotation.Nonnull;

public class SubscriptionListFragment extends RxFragment
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private SubscriptionAdapter _adapter = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter();
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	public static class AddSubscriptionDialog extends DialogFragment {
		public AddSubscriptionDialog() { }

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.subscription_list_add, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			EditText url = (EditText) view.findViewById(R.id.url);
			url.setOnEditorActionListener((url1, actionId, event) -> {
				SubscriptionProvider.addNewSubscription(url1.getContext(), url1.getText().toString());
				dismiss();
				return true;
			});

			view.findViewById(R.id.subscribe).setOnClickListener(button -> {
				EditText url1 = (EditText) ((ViewGroup)button.getParent()).findViewById(R.id.url);
				SubscriptionProvider.addNewSubscription(button.getContext(), url1.getText().toString());
				dismiss();
			});
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
		list.setLayoutManager(new WrappingLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
		list.setAdapter(_adapter);

		View.OnClickListener addListener = view1 -> {
			AddSubscriptionDialog dialog = new AddSubscriptionDialog();
			dialog.show(getFragmentManager(), "addSubscription");
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
		_adapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		_adapter.changeCursor(null);
	}

	public class SubscriptionListViewHolder extends RecyclerView.ViewHolder {
		public final View holder;
		public final ImageView thumbnail;
		public final TextView title;

		public SubscriptionListViewHolder(View v) {
			super(v);

			holder = v;
			thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
			title = (TextView) v.findViewById(R.id.title);
		}
	}

	private class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionListViewHolder> {
		private Cursor _cursor;

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
			notifyDataSetChanged();
		}

		private final View.OnClickListener _subscriptionChoiceHandler = view -> {
			long subId = (long) view.getTag();
			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, subId));
		};

		@Override
		public SubscriptionListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			view.setOnClickListener(_subscriptionChoiceHandler);
			return new SubscriptionListViewHolder(view);
		}

		@Override
		public void onBindViewHolder(SubscriptionListViewHolder holder, int position) {
			_cursor.moveToPosition(position);
			SubscriptionCursor subscription = new SubscriptionCursor(_cursor);
			holder.holder.setTag(subscription.getId());
			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(holder.thumbnail.getContext(), subscription.getId()));
			holder.title.setText(subscription.getTitle());
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
