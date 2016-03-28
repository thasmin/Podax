package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.Episodes;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.trello.rxlifecycle.components.RxFragment;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.Weeks;
import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LatestActivityFragment extends RxFragment {

	private LatestActivityAdapter _adapter;
	private RecyclerView _listView;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		_adapter = new LatestActivityAdapter();
		Episodes.getLatestActivity()
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.toList()
			.compose(bindToLifecycle())
			.subscribe(
				this::setEpisodes,
				e -> Log.e("LatestActivityFragment", "unable to load latest activity", e)
			);
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

		CheckBox showAutomatic = (CheckBox) view.findViewById(R.id.show_automatic);
		showAutomatic.setOnCheckedChangeListener((compoundButton, value) ->
			getActivity().getSharedPreferences("latest_activity", Context.MODE_PRIVATE)
				.edit().putBoolean("automatic_show", true).apply());
		boolean showAutomatically = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean("automatic_show_latest_activity", true);
		showAutomatic.setChecked(showAutomatically);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		_listView.setAdapter(_adapter);
	}

	public void setEpisodes(List<EpisodeData> episodes) {
		// could really use algebraic data types here
		List<Object> items = new ArrayList<>(episodes.size() * 2);
		String lastPeriod = null;
		LocalDate today = Instant.now().toDateTime().toLocalDate();
		for (int i = 0; i < episodes.size(); ++i) {
			EpisodeData episode = episodes.get(i);
			Date date = episode.getPubDate();
			LocalDate pubDay = new DateTime(date.getTime()).toLocalDate();
			Period timeDiff = new Period(pubDay, today);
			int daysBetween = Days.daysBetween(pubDay, today).getDays();

			String period;
			if (daysBetween == 0)
				period = getString(R.string.today);
			else if (daysBetween == 1)
				period = getString(R.string.yesterday);
			else if (daysBetween < 7)
				period = String.format(Locale.getDefault(), "%d %s ago", timeDiff.getDays(), getActivity().getResources().getQuantityString(R.plurals.days, timeDiff.getDays()));
			else if (Weeks.weeksBetween(pubDay, today).getWeeks() < 4)
				period = String.format(Locale.getDefault(), "%d %s ago", timeDiff.getWeeks(), getActivity().getResources().getQuantityString(R.plurals.weeks, timeDiff.getWeeks()));
			else if (Months.monthsBetween(pubDay, today).getMonths() < 12)
				period = String.format(Locale.getDefault(), "%d %s ago", timeDiff.getMonths(), getActivity().getResources().getQuantityString(R.plurals.months, timeDiff.getMonths()));
			else
				period = String.format(Locale.getDefault(), "%d %s ago", timeDiff.getYears(), getActivity().getResources().getQuantityString(R.plurals.years, timeDiff.getYears()));
			period = period.toUpperCase();

			if (!period.equals(lastPeriod))
				items.add(period);
			items.add(i);
			lastPeriod = period;
		}
		_adapter.changeItems(items, episodes);
		_adapter.notifyDataSetChanged();

		getActivity().getSharedPreferences("latest_activity", Context.MODE_PRIVATE)
				.edit().putLong("last_check", Instant.now().getMillis() / 1000).apply();
	}

	private class LatestActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private List<EpisodeData> _episodes = null;
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

		private final View.OnClickListener _subscriptionClickHandler = view ->
			AppFlow.get(getActivity()).displaySubscription((long) view.getTag(), view);

		private final View.OnClickListener _episodeClickHandler = view ->
			AppFlow.get(getActivity()).displayEpisode((long) view.getTag());

		public LatestActivityAdapter() {
			setHasStableIds(true);
		}

		public void changeItems(List<Object> items, List<EpisodeData> episodes) {
			_items = items;
			_episodes = episodes;
		}

		public void clear() { _episodes = null; }

		@Override
		public long getItemId(int position) {
			if (_items.get(position) instanceof String) {
				return _items.get(position).hashCode();
			}
			return _episodes.get((Integer) _items.get(position)).getId();
		}

		@Override
		public int getItemCount() {
			if (_episodes == null)
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
				View view = inflater.inflate(R.layout.latest_activity_list_item, parent, false);
				return new ActivityHolder(view);
			} else {
				View view = inflater.inflate(R.layout.latest_activity_date, parent, false);
				return new HeaderHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
			if (_episodes == null)
				return;

			int viewType = getItemViewType(position);
			if (viewType == TYPE_ACTIVITY) {
				ActivityHolder holder = (ActivityHolder) viewHolder;

				EpisodeData episode = _episodes.get((Integer) _items.get(position));

				SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()).into(holder.subscription_img);
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
