package com.axelby.podax.ui;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class WelcomeFragment extends SherlockFragment {

	private Button _americanLifeBtn;
	private Button _techNewsTodayBtn;
	private Button _rssBtn;

	class AddSubscriptionClickListener implements OnClickListener {
		String _rss;

		AddSubscriptionClickListener(String rss) {
			_rss = rss;
		}

		@Override
		public void onClick(View view) {
			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_URL, _rss);
			getActivity().getContentResolver().insert(SubscriptionProvider.URI, values);
			UpdateService.updateSubscriptions(getActivity());
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.welcome, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		FragmentActivity activity = getActivity();

		_americanLifeBtn = (Button) activity.findViewById(R.id.american_life_btn);
		_americanLifeBtn.setOnClickListener(new AddSubscriptionClickListener("http://feeds.thisamericanlife.org/talpodcast"));

		_techNewsTodayBtn = (Button) activity.findViewById(R.id.tech_news_today_btn);
		_techNewsTodayBtn.setOnClickListener(new AddSubscriptionClickListener("http://leo.am/podcasts/tnt"));

		_rssBtn = (Button) activity.findViewById(R.id.rssurl_btn);
		_rssBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				EditText rssText = (EditText) getActivity().findViewById(R.id.rssurl_text);
				String url = rssText.getText().toString();
				if (url.length() == 0)
					return;
				if (!url.contains("://"))
					url = "http://" + url;
				ContentValues values = new ContentValues();
				values.put(SubscriptionProvider.COLUMN_URL, url);
				getActivity().getContentResolver().insert(SubscriptionProvider.URI, values);
				UpdateService.updateSubscriptions(getActivity());
			}
		});
	}

}
