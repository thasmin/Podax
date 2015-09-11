package com.axelby.podax.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.joanzapata.android.iconify.Iconify;

public class AddSubscriptionFragment extends Fragment {
    public AddSubscriptionFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_subscription, container, false);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        iTunesCategory[] sources = new iTunesCategory[] {
                new iTunesCategory("Top", 0, Iconify.IconValue.fa_trophy),
                new iTunesCategory("Arts", 1301, Iconify.IconValue.fa_photo),
                new iTunesCategory("Business", 1321, Iconify.IconValue.fa_building),
                new iTunesCategory("Comedy", 1303, Iconify.IconValue.fa_smile_o),
                new iTunesCategory("Education", 1304, Iconify.IconValue.fa_graduation_cap),
                new iTunesCategory("Games & Hobbies", 1323, Iconify.IconValue.fa_gamepad),
                new iTunesCategory("Government & Organizations", 1325, Iconify.IconValue.fa_institution),
                new iTunesCategory("Health", 1307, Iconify.IconValue.fa_stethoscope),
                new iTunesCategory("Kids", 1305, Iconify.IconValue.fa_child),
                new iTunesCategory("Music", 1310, Iconify.IconValue.fa_music),
                new iTunesCategory("News & Politics", 1311, Iconify.IconValue.fa_map_marker),
                new iTunesCategory("Religion & Spirituality", 1314, Iconify.IconValue.fa_cloud),
                new iTunesCategory("Science & Medicine", 1315, Iconify.IconValue.fa_flask),
                new iTunesCategory("Society & Culture", 1324, Iconify.IconValue.fa_glass),
                new iTunesCategory("Sports & Recreation", 1316, Iconify.IconValue.fa_car),
                new iTunesCategory("TV & Film", 1309, Iconify.IconValue.fa_film),
                new iTunesCategory("Technology", 1318, Iconify.IconValue.fa_code)
        };

		View.OnClickListener categoryClickListener = view1 -> {
			Long catId = (Long) view1.getTag();
			startActivity(PodaxFragmentActivity.createIntent(view1.getContext(), ITunesToplistFragment.class, Constants.EXTRA_CATEGORY_ID, catId));
		};

        LinearLayout toplists = (LinearLayout) view.findViewById(R.id.toplists);
		for (iTunesCategory cat : sources) {
			int layoutId = R.layout.fragment_add_subscription_item;
			View catView = LayoutInflater.from(view.getContext()).inflate(layoutId, toplists, false);
			catView.setTag(cat.id);
			catView.setOnClickListener(categoryClickListener);

			((TextView) catView.findViewById(R.id.name)).setText(cat.name);
			TextView icon = (TextView) catView.findViewById(R.id.icon);
			Iconify.setIcon(icon, cat.icon);
			toplists.addView(catView);
		}

        view.findViewById(R.id.web_subscribe).setOnClickListener(view1 ->
			startActivity(PodaxFragmentActivity.createIntent(view1.getContext(), WebSubscriptionFragment.class, null))
		);

        view.findViewById(R.id.add_rss).setOnClickListener(view1 -> {
			EditText rssText = (EditText) getActivity().findViewById(R.id.rssurl);
			if (rssText.getText() == null)
				return;
			String url = rssText.getText().toString();
			if (url.length() == 0)
				return;
			if (!url.contains("://"))
				url = "http://" + url;

			SubscriptionProvider.addNewSubscription(getActivity(), url);
			rssText.setText("");
		});
    }

    public static class iTunesCategory {
        public final long id;
        public final String name;
        public final Iconify.IconValue icon;

        public iTunesCategory(String name, long id, Iconify.IconValue icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
