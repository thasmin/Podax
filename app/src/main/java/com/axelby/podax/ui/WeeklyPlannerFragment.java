package com.axelby.podax.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.Stats;
import com.axelby.podax.databinding.SubscriptionCheckboxBinding;
import com.axelby.podax.model.Episodes;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.trello.rxlifecycle.components.RxFragment;

import org.joda.time.LocalDate;

import java.util.List;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// TODO: localize, add "find new subscriptions" button
public class WeeklyPlannerFragment extends RxFragment {
	private TextView _listenTime;
	private TextView _autoAddTime;
	private TextView _diffTime;
	private TextView _diffLabel;
	private ViewGroup _subList;
	private View _subEmpty;

	private List<Long> _subIds = null;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		Observable.from(PodaxDB.subscriptions.getAll())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.toList()
			.compose(bindToLifecycle())
			.subscribe(
				this::setSubscriptions,
				e -> Log.e("WeeklyPlannerFragment", "unable to retrieve subscriptions", e)
			);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.weekly_planner_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listenTime = (TextView) view.findViewById(R.id.listen_time);
		_autoAddTime = (TextView) view.findViewById(R.id.autoadd_time);
		_diffTime = (TextView) view.findViewById(R.id.diff_time);
		_diffLabel = (TextView) view.findViewById(R.id.diff_label);
		_subList = (ViewGroup) view.findViewById(R.id.subscription_list);
		_subEmpty = view.findViewById(R.id.subscription_empty);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Observable.from(PodaxDB.subscriptions.getAll())
			.subscribeOn(Schedulers.io())
			.filter(SubscriptionData::areNewEpisodesAddedToPlaylist)
			.map(SubscriptionData::getId)
			.toList()
			.subscribe(
				subIds -> {
					_subIds = subIds;
					getAutoAddedTimeAndUpdate();
				},
				e -> Log.e("WeeklyPlannerFragment", "unable to retrieve subscription added to playlist", e)
			);

		PodaxDB.subscriptions.watchAll().subscribe(
			sub -> {
				if (_subIds == null)
					return;
				if (_subIds.contains(sub.getId()))
					_subIds.remove(sub.getId());
				else
					_subIds.add(sub.getId());
				getAutoAddedTimeAndUpdate();
			},
			e -> Log.e("WeeklyPlannerFragment", "error while updating for a subscription change", e)
		);
	}

	private void getAutoAddedTimeAndUpdate() {
		Episodes.getNewForSubscriptionIds(_subIds)
			.observeOn(AndroidSchedulers.mainThread())
			.reduce(0f, (carried, ep) -> carried += ep.getDuration() / 1000.0f)
			.subscribe(
				this::updateUI,
				e -> Log.e("WeeklyPlannerFragment", "unable to retrieve newest episodes", e)
			);
	}

	private void updateUI(float autoAddedTime) {
		float weekListenTime = 0f;
		for (LocalDate date = LocalDate.now().minusDays(1); date.compareTo(LocalDate.now().minusDays(8)) > 0; date = date.minusDays(1))
			weekListenTime += Stats.getListenTime(getActivity(), date);
		_listenTime.setText(Helper.getVerboseTimeString(getActivity(), weekListenTime, false));

		_autoAddTime.setText(Helper.getVerboseTimeString(getActivity(), autoAddedTime, false));

		float diffTime = Math.abs(autoAddedTime - weekListenTime);
		_diffTime.setText(Helper.getVerboseTimeString(getActivity(), diffTime, false));
		_diffLabel.setText(autoAddedTime > weekListenTime ? R.string.extra : R.string.shortage);
	}

	public void setSubscriptions(List<SubscriptionData> subscriptions) {
		boolean isEmpty = subscriptions.size() == 0;
		while (_subList.getChildCount() > 3)
			_subList.removeViewAt(3);
		_subEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

		LayoutInflater inflater = LayoutInflater.from(getActivity());
		for (SubscriptionData sub : subscriptions) {
			SubscriptionCheckboxBinding view = SubscriptionCheckboxBinding.inflate(inflater, _subList, false);
			view.setSubscription(sub);
			_subList.addView(view.getRoot());
		}
	}
}
