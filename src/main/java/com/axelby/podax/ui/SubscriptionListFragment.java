package com.axelby.podax.ui;

import android.app.DialogFragment;
import android.database.Cursor;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.BR;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.squareup.picasso.Picasso;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SubscriptionListFragment extends RxFragment {

	private SubscriptionAdapter _adapter = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

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

	private class SubscriptionAdapter extends RecyclerView.Adapter<DataBoundViewHolder> {
		private List<ItemModel> _subscriptions = new ArrayList<>(0);

		public SubscriptionAdapter() {
			setHasStableIds(true);

			Observable<SubscriptionCursor> ob = Observable.create(subscriber -> {
				Cursor c = getActivity().getContentResolver().query(SubscriptionProvider.URI, null, null, null, null);
				if (c != null) {
					while (c.moveToNext())
						subscriber.onNext(new SubscriptionCursor(c));
					c.close();
				}
				subscriber.onCompleted();
			});
			ob.map(sub -> ItemModel.fromSubscriptionId(sub.getTitle(), sub.getThumbnail(), sub.getId()))
				.toList()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(RxLifecycle.bindFragment(lifecycle()))
				.subscribe(
					subs -> {
						_subscriptions = subs;
						notifyDataSetChanged();
					},
					e -> Log.e("SubscriptionAdapter", "error while retrieving subscriptions", e)
				);
		}

		@Override
		public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return DataBoundViewHolder.from(parent, R.layout.subscription_list_item);
		}

		@Override
		public void onBindViewHolder(DataBoundViewHolder holder, int position) {
			holder.binding.setVariable(BR.model, _subscriptions.get(position));
		}

		@Override
        public long getItemId(int position) {
            return position;
        }

		@Override
		public int getItemCount() {
			return _subscriptions.size();
		}

	}

	@BindingAdapter("app:picassoImageUrl")
	@SuppressWarnings("unused")
	public static void loadImageUrlViaPicasso(ImageView image, String url) {
		if (url == null || url.length() == 0)
			return;
		Picasso.with(image.getContext()).load(url).fit().into(image);
	}

	public static class ItemModel {
		public enum RSSSource { ITunes, RSS, SubscriptionId }

		private final String _imageUrl;
		private final String _title;
		private final RSSSource _source;
		private final String _rssUrl;
		private final long _subscriptionId;

		public static ItemModel fromRSS(String title, String imageUrl, String rssUrl) {
			return new ItemModel(title, imageUrl, RSSSource.RSS, rssUrl);
		}

		public static ItemModel fromITunes(String title, String imageUrl, String itunesUrl) {
			return new ItemModel(title, imageUrl, RSSSource.ITunes, itunesUrl);
		}
		public static ItemModel fromSubscriptionId(String title, String imageUrl, long subscriptionId) {
			return new ItemModel(title, imageUrl, subscriptionId);
		}

		private ItemModel(String title, String imageUrl, RSSSource source, String rssUrl) {
			_title = title;
			_imageUrl = imageUrl;
			_source = source;
			_rssUrl = rssUrl;
			_subscriptionId = -1;
		}

		private ItemModel(String title, String imageUrl, long subscriptionId) {
			_title = title;
			_imageUrl = imageUrl;
			_source = RSSSource.SubscriptionId;
			_rssUrl = null;
			_subscriptionId = subscriptionId;
		}

		public String getImageUrl() { return _imageUrl; }
		public String getTitle() { return _title; }

		public void show(View view) {
			View thumbnail = view.findViewById(R.id.thumbnail);
			View title = view.findViewById(R.id.title);
			switch (_source) {
				case RSS:
					AppFlow.get(Helper.getActivityFromView(view)).displayPodcastViaRSSUrl(_title, _rssUrl, thumbnail, title);
					break;
				case ITunes:
					AppFlow.get(Helper.getActivityFromView(view)).displayPodcastViaITunes(_title, _rssUrl, thumbnail, title);
					break;
				case SubscriptionId:
					AppFlow.get(Helper.getActivityFromView(view)).displaySubscription(_title, _subscriptionId, thumbnail, title);
					break;
			}
		}
	}

}
