package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

public class LatestActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private LatestActivityAdapter _adapter;
	private RecyclerView _listView;
	private CheckBox _showAutomatic;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		_adapter = new LatestActivityAdapter();
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.latest_activity_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) view.findViewById(R.id.recyclerview);
		_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
		_listView.setItemAnimator(new DefaultItemAnimator());

		_showAutomatic = (CheckBox) view.findViewById(R.id.show_automatic);
		_showAutomatic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
				getActivity().getSharedPreferences("latest_activity", Context.MODE_PRIVATE)
						.edit().putBoolean("automatic_show", true).apply();
			}
		});
		boolean showAutomatically = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean("automatic_show_latest_activity", true);
		_showAutomatic.setChecked(showAutomatically);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		_listView.setAdapter(_adapter);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), EpisodeProvider.LATEST_ACTIVITY_URI, null, null, null, EpisodeProvider.COLUMN_PUB_DATE + " DESC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// could really use algebraic data types here
		List<Object> items = new ArrayList<>(cursor.getCount() * 2);
		String lastPeriod = null;
		LocalDate today = Instant.now().toDateTime().toLocalDate();
		while (cursor.moveToNext()) {
			Date date = new EpisodeCursor(cursor).getPubDate();
			LocalDate pubDay = new DateTime(date.getTime()).toLocalDate();
			Period timeDiff = new Period(pubDay, today);

			String period;
			if (timeDiff.getDays() == 0)
				period = getString(R.string.today);
			else if (timeDiff.getDays() == 1)
				period = getString(R.string.yesterday);
			else if (timeDiff.getDays() < 7)
				period = String.format("%d %s ago", timeDiff.getDays(), getActivity().getResources().getQuantityString(R.plurals.days, timeDiff.getDays()));
			else if (timeDiff.getWeeks() < 4)
				period = String.format("%d %s ago", timeDiff.getWeeks(), getActivity().getResources().getQuantityString(R.plurals.weeks, timeDiff.getWeeks()));
			else if (timeDiff.getMonths() < 12)
				period = String.format("%d %s ago", timeDiff.getMonths(), getActivity().getResources().getQuantityString(R.plurals.months, timeDiff.getMonths()));
			else
				period = String.format("%d %s ago", timeDiff.getYears(), getActivity().getResources().getQuantityString(R.plurals.years, timeDiff.getYears()));
			period = period.toUpperCase();

			if (!period.equals(lastPeriod))
				items.add(period);
			items.add(cursor.getPosition());
			lastPeriod = period;
		}
		_adapter.changeItems(items, cursor);
		_adapter.notifyDataSetChanged();

		getActivity().getSharedPreferences("latest_activity", Context.MODE_PRIVATE)
				.edit().putLong("last_check", Instant.now().getMillis() / 1000).apply();
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (getActivity() == null)
			return;

		_adapter.clear();
		_adapter.notifyDataSetChanged();
	}

	private class LatestActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private Cursor _cursor = null;
		private List<Object> _items = null;

		private final int TYPE_HEADER = 0;
		private final int TYPE_ACTIVITY = 1;

		public class HeaderHolder extends RecyclerView.ViewHolder {
			public TextView header;
			public HeaderHolder(View view) {
				super(view);
				header = (TextView) view;
			}
		}

		public class ActivityHolder extends RecyclerView.ViewHolder {
			public ImageView subscription_img;
			public TextView episode_title;
			public TextView episode_description;

			public ActivityHolder(View view) {
				super(view);

				subscription_img = (ImageView) view.findViewById(R.id.subscription_img);
				subscription_img.setOnClickListener(_subscriptionClickHandler);
				episode_title = (TextView) view.findViewById(R.id.episode_title);
				episode_title.setOnClickListener(_episodeClickHandler);
				episode_description = (TextView) view.findViewById(R.id.episode_description);
			}
		}

		private final View.OnClickListener _subscriptionClickHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, (long) view.getTag()));
			}
		};

		private final View.OnClickListener _episodeClickHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, (long) view.getTag()));
			}
		};

		private final View.OnClickListener _playHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				PlayerService.play(getActivity(), (long) view.getTag());
			}
		};

		public LatestActivityAdapter() {
			setHasStableIds(true);
		}

		public void changeItems(List<Object> items, Cursor cursor) {
			_items = items;
			_cursor = cursor;
		}

		public void clear() { _cursor = null; }

		@Override
		public long getItemId(int position) {
			if (_items.get(position) instanceof String) {
				return _items.get(position).hashCode();
			}
			_cursor.moveToPosition((Integer) _items.get(position));
			return new EpisodeCursor(_cursor).getId();
		}

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _items.size();
		}

		@Override
		public int getItemViewType(int position) {
			return _items.get(position) instanceof String ? TYPE_HEADER : TYPE_ACTIVITY;
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			if (viewType == TYPE_ACTIVITY) {
				View view = inflater.inflate(R.layout.list_item_latest_activity, parent, false);
				return new ActivityHolder(view);
			} else {
				View view = inflater.inflate(R.layout.latest_activity_date, parent, false);
				return new HeaderHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
			if (_cursor == null)
				return;

			int viewType = getItemViewType(position);
			if (viewType == TYPE_ACTIVITY) {
				ActivityHolder holder = (ActivityHolder) viewHolder;

				_cursor.moveToPosition((Integer) _items.get(position));
				EpisodeCursor episode = new EpisodeCursor(_cursor);

				holder.subscription_img.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));
				holder.subscription_img.setTag(episode.getSubscriptionId());
				holder.episode_title.setText(episode.getTitle());
				holder.episode_title.setTag(episode.getId());
				holder.episode_description.setText(Html.fromHtml(episode.getDescription(), new NullImageGetter(), new NullTagHandler()));
			} else if (viewType == TYPE_HEADER) {
				HeaderHolder holder = (HeaderHolder) viewHolder;
				String period = (String) _items.get(position);
				holder.header.setText(period);
			}
		}

		private class NullImageGetter implements Html.ImageGetter {
			@Override
			public Drawable getDrawable(String url) { return null; }
		}

		private class NullTagHandler implements Html.TagHandler {
			@Override
			public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) { }
		}
	}
}
