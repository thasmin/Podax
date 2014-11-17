package com.axelby.podax.ui;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.joanzapata.android.iconify.Iconify;

public class AddSubscriptionFragment extends Fragment {
    public AddSubscriptionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_subscription, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
        ListView listView = (ListView) getActivity().findViewById(R.id.list);
        listView.setAdapter(new AddSubscriptionAdapter(getActivity(), sources));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Helper.changeFragment(getActivity(), ITunesToplistFragment.class, Constants.EXTRA_CATEGORY_ID, id);
            }
        });

        getActivity().findViewById(R.id.web_subscribe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.changeFragment(getActivity(), WebSubscriptionFragment.class, null);
            }
        });

        getActivity().findViewById(R.id.add_rss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
    }

    public static class iTunesCategory {
        public long id;
        public String name;
        public Iconify.IconValue icon;

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

    private class AddSubscriptionAdapter extends ArrayAdapter<iTunesCategory> {
        iTunesCategory[] _categories;

        public AddSubscriptionAdapter(Context context, iTunesCategory[] categories) {
            super(context, R.layout.fragment_add_subscription_item, R.id.name, categories);
            _categories = categories;
        }

        @Override
        public long getItemId(int position) {
            return _categories[position].id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            TextView icon;
            if (view.getTag() != null)
                icon = (TextView) view.getTag();
            else {
                icon = (TextView) view.findViewById(R.id.icon);
                view.setTag(icon);
            }

            iTunesCategory category = getItem(position);
            Iconify.setIcon(icon, category.icon);

            return view;
        }
    }
}
